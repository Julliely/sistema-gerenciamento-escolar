# =====================================================================
# Script de teste local: cria cluster Kubernetes com 3 nodes (kind),
# builda as imagens, carrega no cluster e faz o deploy completo.
#
# Uso:    .\k8s\start-local.ps1
# Limpar: .\k8s\start-local.ps1 -Action delete
# =====================================================================
param(
    [ValidateSet("apply", "delete")]
    [string]$Action = "apply"
)

# IMPORTANTE: nao usamos "Stop" aqui porque PowerShell 5.1 trata
# qualquer texto em stderr de comando nativo como erro terminal.
# Verificamos $LASTEXITCODE manualmente nos pontos criticos.
$ErrorActionPreference = "Continue"

$ClusterName = "escola"
$Namespace = "escola-system"
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Write-Step($msg) {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host " $msg" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
}

function Assert-Success($msg) {
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "FALHA: $msg (exit code $LASTEXITCODE)" -ForegroundColor Red
        exit 1
    }
}

if ($Action -eq "delete") {
    Write-Step "Removendo cluster kind '$ClusterName'..."
    kind delete cluster --name $ClusterName
    Write-Host "Cluster removido." -ForegroundColor Green
    exit 0
}

# ---------------------------------------------------------------------
# 1. Garantir que o kind esta instalado
# ---------------------------------------------------------------------
Write-Step "[1/7] Verificando kind"
if (-not (Get-Command kind -ErrorAction SilentlyContinue)) {
    Write-Host "kind nao encontrado. Instalando via winget..."
    winget install --id Kubernetes.kind --silent --accept-source-agreements --accept-package-agreements
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    if (-not (Get-Command kind -ErrorAction SilentlyContinue)) {
        Write-Host "Falha ao instalar kind via winget. Instale manualmente:" -ForegroundColor Red
        Write-Host "  https://kind.sigs.k8s.io/docs/user/quick-start/#installation" -ForegroundColor Red
        exit 1
    }
}
kind version

# Verificar se Docker esta rodando
docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Docker Desktop nao esta rodando. Abra o Docker Desktop e aguarde subir antes de rodar de novo." -ForegroundColor Red
    exit 1
}

# ---------------------------------------------------------------------
# 2. Criar o cluster com 3 nodes (se ainda nao existir)
# ---------------------------------------------------------------------
Write-Step "[2/7] Criando cluster '$ClusterName' com 3 nodes"

# kind get clusters escreve "No kind clusters found." em stderr quando vazio.
# Capturamos stdout+stderr juntos como string e ignoramos o ruido.
$clustersRaw = & kind get clusters 2>&1
$clusterExists = $false
foreach ($line in @($clustersRaw)) {
    $name = "$line".Trim()
    if ($name -eq $ClusterName) { $clusterExists = $true }
}

if ($clusterExists) {
    Write-Host "Cluster '$ClusterName' ja existe, reutilizando." -ForegroundColor Yellow
} else {
    kind create cluster --config "$PSScriptRoot\kind-cluster.yaml"
    Assert-Success "kind create cluster"
}

kubectl config use-context "kind-$ClusterName" | Out-Null
Assert-Success "kubectl config use-context"

# Permitir que o control-plane tambem rode pods (so 3 nodes => uso todos)
Write-Host "Removendo taint do control-plane para usar os 3 nodes..."
kubectl taint nodes "$ClusterName-control-plane" node-role.kubernetes.io/control-plane:NoSchedule- 2>&1 | Out-Null

Write-Host "Nodes do cluster:"
kubectl get nodes -o wide

# ---------------------------------------------------------------------
# 3. Instalar Nginx Ingress Controller
# ---------------------------------------------------------------------
Write-Step "[3/7] Instalando Nginx Ingress Controller"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/kind/deploy.yaml
Assert-Success "kubectl apply ingress-nginx"

Write-Host "Aguardando Ingress Controller ficar pronto (pode demorar 1-2 min)..."
kubectl wait --namespace ingress-nginx `
    --for=condition=ready pod `
    --selector=app.kubernetes.io/component=controller `
    --timeout=180s
Assert-Success "kubectl wait ingress-nginx"

# ---------------------------------------------------------------------
# 4. Buildar as imagens Docker
# ---------------------------------------------------------------------
Write-Step "[4/7] Buildando imagens Docker"
$services = @(
    @{ Name = "cadastro-service";   Path = "$RepoRoot\cadastro-service" },
    @{ Name = "disciplina-service"; Path = "$RepoRoot\disciplina-service" },
    @{ Name = "matricula-service";  Path = "$RepoRoot\matricula-service" },
    @{ Name = "gateway-service";    Path = "$RepoRoot\gateway-service" },
    @{ Name = "frontend";           Path = "$RepoRoot\frontend" }
)
foreach ($svc in $services) {
    Write-Host ">>> Buildando escola/$($svc.Name):latest" -ForegroundColor Yellow
    docker build -t "escola/$($svc.Name):latest" $svc.Path
    Assert-Success "docker build $($svc.Name)"
}

# ---------------------------------------------------------------------
# 5. Carregar imagens no cluster kind
# ---------------------------------------------------------------------
Write-Step "[5/7] Carregando imagens no cluster kind"
foreach ($svc in $services) {
    Write-Host ">>> kind load docker-image escola/$($svc.Name):latest" -ForegroundColor Yellow
    kind load docker-image "escola/$($svc.Name):latest" --name $ClusterName
    Assert-Success "kind load $($svc.Name)"
}

# ---------------------------------------------------------------------
# 6. Aplicar manifests do sistema
# ---------------------------------------------------------------------
Write-Step "[6/7] Aplicando manifests do sistema"
kubectl apply -f "$PSScriptRoot\00-namespace.yaml"
Assert-Success "apply namespace"
kubectl apply -f "$PSScriptRoot\01-configmap.yaml"
Assert-Success "apply configmap"
kubectl apply -f "$PSScriptRoot\01-secrets.yaml"
Assert-Success "apply secrets"
kubectl apply -f "$PSScriptRoot\02-postgresql.yaml"
Assert-Success "apply postgresql"

Write-Host "Aguardando PostgreSQL ficar pronto..."
kubectl wait --namespace=$Namespace --for=condition=ready pod -l app=postgresql --timeout=180s
Assert-Success "wait postgresql"

kubectl apply -f "$PSScriptRoot\03-cadastro-service.yaml"
kubectl apply -f "$PSScriptRoot\04-disciplina-service.yaml"
kubectl apply -f "$PSScriptRoot\05-matricula-service.yaml"
kubectl apply -f "$PSScriptRoot\06-gateway-service.yaml"
kubectl apply -f "$PSScriptRoot\07-frontend.yaml"
kubectl apply -f "$PSScriptRoot\08-ingress.yaml"
Assert-Success "apply ingress"

# ---------------------------------------------------------------------
# 7. Resumo
# ---------------------------------------------------------------------
Write-Step "[7/7] Aguardando todos os pods ficarem prontos"
kubectl wait --namespace=$Namespace --for=condition=ready pod --all --timeout=300s
if ($LASTEXITCODE -ne 0) {
    Write-Host "Alguns pods ainda nao estao prontos. Veja com 'kubectl get pods -n $Namespace'." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " Deploy concluido!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Distribuicao dos pods nos 3 nodes:"
kubectl get pods -n $Namespace -o wide

Write-Host ""
Write-Host "Adicione no arquivo C:\Windows\System32\drivers\etc\hosts (como admin):"
Write-Host "  127.0.0.1 escola.local" -ForegroundColor Yellow
Write-Host ""
Write-Host "Depois acesse:"
Write-Host "  http://escola.local" -ForegroundColor Yellow
Write-Host ""
Write-Host "Para parar e remover tudo:"
Write-Host "  .\k8s\start-local.ps1 -Action delete" -ForegroundColor Cyan
