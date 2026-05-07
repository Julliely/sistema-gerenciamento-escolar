# Discentes
Julliely de Sousa, Fernanda Ribeiro e João Marcelo Pereira

# Sistema de Gerenciamento Escolar

Trabalho acadêmico que aplica os fundamentos de **Arquitetura de Microsserviços** e **Sistemas Distribuídos**. O sistema simula uma instituição de ensino com cadastro de alunos/professores, disciplinas e matrículas. Roda como um cluster Kubernetes distribuído em **3 máquinas**, com **tratamento de falhas** entre os serviços usando Circuit Breaker.

## Arquitetura

O sistema é composto por **4 microsserviços Java (Spring Boot)**, um **frontend (Nginx)** e um **PostgreSQL**, todos rodando num cluster Kubernetes de 3 nodes.

```
                  [ Cliente / Navegador ]
                          |
                  http://escola.local
                          |
                 [ Nginx Ingress Controller ]
                          |
        +-----------------+-----------------+
        |                                   |
  [ Frontend (Nginx) ]               [ API Gateway ]
                                          (8080)
                                            |
              +-----------------------------+-----------------------------+
              |                             |                             |
    [ Cadastro Service ]         [ Disciplina Service ]         [ Matricula Service ]
          (8081)                       (8083)                         (8082)
              |                             |                             |
              +-----------------------------+-----------------------------+
                                            |
                                  [ PostgreSQL 15 ]
                                       (5432)
```

### Distribuição nas 3 máquinas (nodes do K8s)

Cada microsserviço tem **3 réplicas**, e o Kubernetes (via `podAntiAffinity`) procura colocar cada réplica num node diferente. Resultado típico:

```
+-------------------+   +-------------------+   +-------------------+
|     node-1        |   |     node-2        |   |     node-3        |
+-------------------+   +-------------------+   +-------------------+
| frontend          |   | frontend          |   | frontend          |
| gateway           |   | gateway           |   | gateway           |
| cadastro          |   | cadastro          |   | cadastro          |
| disciplina        |   | disciplina        |   | disciplina        |
| matricula         |   | matricula         |   | matricula         |
| postgresql ★      |   |                   |   |                   |
+-------------------+   +-------------------+   +-------------------+
```

> O PostgreSQL é um StatefulSet com 1 réplica (volume persistente). Se um node cair, os serviços continuam funcionando nos outros dois — esse é o ganho de rodar distribuído.

### Divisão de Responsabilidades

| Serviço | Porta | Responsabilidade |
|---|---|---|
| **Cadastro Service** | 8081 | Gestão de alunos e professores |
| **Disciplina Service** | 8083 | Disciplinas e períodos |
| **Matrícula Service** | 8082 | Matrículas (chama Cadastro e Disciplina) |
| **Gateway Service** | 8080 | Roteamento e Circuit Breaker |
| **Frontend** | 80 | Interface web (Nginx) |
| **PostgreSQL** | 5432 | Banco de dados |

## Tratamento de Erros (Resilience4j)

O grande desafio de um sistema distribuído é o que fazer quando **um serviço chama outro e o outro está fora do ar ou lento**. A solução implementada combina três padrões da biblioteca **Resilience4j**:

### 1. Retry — tenta de novo

Quando uma chamada falha (timeout, conexão recusada), o serviço espera um pouco e tenta novamente, até 3 vezes, com **backoff exponencial** (1s, 2s, 4s). Falhas transitórias (um pod reiniciando, rede instável) se resolvem sozinhas.

### 2. Circuit Breaker — para de bater na porta fechada

Se as falhas continuam, abrir conexões repetidas só piora o problema. O Circuit Breaker observa as últimas 10 chamadas e:

```
   FECHADO  ─── 50% de falhas ───►  ABERTO  ─── 30s ───►  MEIO-ABERTO
       ▲                                                       │
       └──────────────── tudo ok ──────────────────────────────┘
```

- **FECHADO**: tudo normal, requisições passam.
- **ABERTO**: serviço-alvo está quebrado. Requisições falham na hora (sem tentar) e o **fallback** entra.
- **MEIO-ABERTO**: depois de 30s, deixa passar 3 chamadas de teste. Se funcionarem, volta a fechar.

### 3. Fallback — resposta de emergência

Quando o circuito está aberto ou todas as tentativas falharam, o serviço retorna uma resposta padrão (ex: "503 — serviço temporariamente indisponível", lista vazia, valor cacheado) em vez de quebrar a requisição inteira. Evita falhas em cascata.

### Onde está configurado

| Quem chama | Quem é chamado | Circuit Breaker |
|---|---|---|
| disciplina-service | cadastro-service | `cadastroService` |
| matricula-service | cadastro-service | `cadastroService` |
| matricula-service | disciplina-service | `disciplinaService` |
| gateway-service | cadastro-service | `cadastroRoute` |
| gateway-service | matricula-service | `matriculaRoute` |
| gateway-service | disciplina-service | `disciplinaRoute` |

Configuração nos arquivos `application.properties` de cada serviço. Estado visível em:

```bash
kubectl exec -n escola-system deploy/gateway-service -- curl -s localhost:8080/actuator/health
```

### Health Checks (Kubernetes)

Cada pod expõe três endpoints que o K8s usa para tomar decisões:

| Endpoint | Para que serve |
|---|---|
| `/actuator/health/startup` | "Já terminei de subir?" — K8s só começa a testar liveness/readiness depois disso |
| `/actuator/health/readiness` | "Estou pronto para receber tráfego?" — se NÃO, o Service para de mandar requisições |
| `/actuator/health/liveness` | "Ainda estou vivo?" — se NÃO, o K8s reinicia o pod automaticamente |

Combinado com `replicas: 3` e anti-affinity, isso garante que **uma falha individual nunca derruba o sistema**.

## Como rodar

### Pré-requisitos
- Cluster Kubernetes com **3 nodes** (pode ser kind, k3s, minikube com 3 nodes ou cloud)
- `kubectl` configurado
- Nginx Ingress Controller instalado no cluster
- Imagens Docker dos serviços construídas e disponíveis no cluster

### Construir as imagens

Em cada serviço:
```bash
cd cadastro-service && mvn clean package && docker build -t escola/cadastro-service:latest .
cd disciplina-service && mvn clean package && docker build -t escola/disciplina-service:latest .
cd matricula-service && mvn clean package && docker build -t escola/matricula-service:latest .
cd gateway-service && mvn clean package && docker build -t escola/gateway-service:latest .
cd frontend && docker build -t escola/frontend:latest .
```

### Deploy
```bash
cd k8s
chmod +x deploy.sh
./deploy.sh apply
```

### Acessar
Adicione ao `/etc/hosts` (ou `C:\Windows\System32\drivers\etc\hosts` no Windows):
```
127.0.0.1 escola.local
```

Acesse: **http://escola.local**

### Verificar a distribuição nos 3 nodes
```bash
kubectl get pods -n escola-system -o wide
```

A coluna `NODE` mostra em qual máquina cada pod está rodando.

### Remover
```bash
cd k8s
./deploy.sh delete
```

## Componentes Kubernetes (resumo)

| Recurso | Quantidade | Função |
|---|---|---|
| Namespace | 1 (escola-system) | Isolamento lógico |
| ConfigMap | 1 | URLs internas dos serviços |
| Secret | 1 | Credenciais do PostgreSQL |
| StatefulSet | 1 (postgresql) | Banco com volume persistente |
| Deployment | 5 (3 réplicas cada) | 4 microsserviços + frontend |
| Service (ClusterIP) | 6 | DNS interno entre os pods |
| Ingress | 1 | Ponto de entrada externo |

## Endpoints da API (via Gateway em http://escola.local)

### Cadastro
- `GET/POST /cadastro/alunos`
- `PUT/DELETE /cadastro/alunos/{id}`
- `GET/POST /cadastro/professores`
- `PUT/DELETE /cadastro/professores/{id}`

### Disciplina
- `GET/POST /disciplina/disciplinas`
- `PUT/DELETE /disciplina/disciplinas/{id}`
- `GET/POST /disciplina/periodos`

### Matrícula
- `GET/POST /matricula/matriculas`
- `GET /matricula/matriculas/aluno/{alunoId}`
- `PATCH /matricula/matriculas/{id}/status`
- `DELETE /matricula/matriculas/{id}`

## Tecnologias

- **Java 21**, Spring Boot 3.5
- **Resilience4j** (Circuit Breaker, Retry, Fallback)
- **Spring Actuator** (health checks)
- **PostgreSQL 15**
- **Docker** + **Kubernetes** (3 nodes)
- **Nginx** (frontend + ingress)
- HTML/CSS/JavaScript (frontend)

## Estrutura do Projeto

```
sistema-de-gerenciamento-escolar/
├── cadastro-service/         # Microsservico Java (porta 8081)
├── disciplina-service/       # Microsservico Java (porta 8083)
├── matricula-service/        # Microsservico Java (porta 8082)
├── gateway-service/          # API Gateway (porta 8080)
├── frontend/                 # Interface web (Nginx)
├── k8s/
│   ├── 00-namespace.yaml
│   ├── 01-configmap.yaml
│   ├── 01-secrets.yaml
│   ├── 02-postgresql.yaml          # StatefulSet + PVC
│   ├── 03-cadastro-service.yaml    # Deployment + Service (3 replicas + anti-affinity)
│   ├── 04-disciplina-service.yaml  # idem
│   ├── 05-matricula-service.yaml   # idem
│   ├── 06-gateway-service.yaml     # idem
│   ├── 07-frontend.yaml            # idem
│   ├── 08-ingress.yaml             # Roteamento HTTP externo
│   └── deploy.sh                   # Script de apply/delete
├── docker-compose.yml        # Para testar localmente sem K8s
├── 01_reset_database.sql
└── 02_populate_database.sql
```

## Desenvolvimento Local (sem Kubernetes)

Se quiser testar a aplicação na sua máquina antes de subir no cluster:

```bash
docker compose up --build
```

- Frontend: http://localhost:5500
- Gateway: http://localhost:8080

---

*Componente prático da disciplina de Arquitetura de Software — demonstra microsserviços, sistemas distribuídos com Kubernetes e tratamento de falhas com Resilience4j.*
