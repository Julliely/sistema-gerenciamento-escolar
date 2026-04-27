# Discentes
Julliely de Sousa, Fernanda Ribeiro e João Marcelo Pereira

# Sistema de Gerenciamento Escolar

Este projeto é uma atividade acadêmica desenvolvida com o objetivo de compreender e aplicar os princípios fundamentais da **Arquitetura de Microsserviços**. O sistema simula o fluxo de uma instituição de ensino, abrangendo desde o cadastro base até a lógica de matrículas e validação de pré-requisitos.

## Arquitetura Geral

O sistema é composto por **4 microsserviços Java (Spring Boot)**, um **frontend estático (Nginx)** e um **banco de dados PostgreSQL**, todos orquestrados via **Docker Compose** para desenvolvimento local e com **manifests Kubernetes** para deploy em cluster.

```
                         [ Internet / Ingress Controller ]
                                      |
                     +----------------+----------------+
                     |                                 |
              [ Frontend (Nginx) ]           [ API Gateway (8080) ]
                                                  |
                          +-----------------------+-----------------------+
                          |                       |                       |
                 [ Cadastro Service ]    [ Disciplina Service ]   [ Matricula Service ]
                     (8081)                   (8083)                   (8082)
                          |                       |                       |
                          +-----------+-----------+-----------+-----------+
                                      |
                             [ PostgreSQL 15 ]
                                 (5432)

       [ Prometheus (9090) ] ----scrape----> [ Todos os servicos /actuator/prometheus ]
              |
       [ Grafana (3000) ] --- dashboards com metricas e alertas
```

### Divisão de Responsabilidades

| Serviço | Porta | Responsabilidade |
|---|---|---|
| **Cadastro Service** | 8081 | Gestão de alunos e professores |
| **Disciplina Service** | 8083 | Grade curricular, períodos e validação de professores |
| **Matrícula Service** | 8082 | Enturmação, verificação de alunos e disciplinas |
| **Gateway Service** | 8080 | Roteamento centralizado, CORS e Circuit Breaker |
| **Frontend** | 5500 (Docker) / 80 (K8s) | Interface web |

## Tecnologias Utilizadas

### Backend
- Java 21, Spring Boot 3.5.13, Spring Data JPA, Hibernate
- Spring Actuator (health checks, métricas)
- Resilience4j (Circuit Breaker, Retry, Fallback)
- Micrometer + Prometheus Registry (métricas)

### Frontend
- HTML5, CSS3, JavaScript (Vanilla JS)
- Fetch API com padrão Service Pattern

### Banco de Dados
- PostgreSQL 15

### Infraestrutura
- Docker & Docker Compose (desenvolvimento local)
- Kubernetes (produção) — manifests completos em `k8s/`
- Nginx Ingress Controller
- Prometheus + Grafana (observabilidade)

### CI/CD
- GitHub Actions (build, test, Docker push para GHCR)
- kubeconform (validação de manifests K8s)

## Executando com Docker Compose

### Pré-requisitos
- Docker e Docker Compose instalados

### Passos
```bash
docker compose up --build
```

Aguarde os serviços subirem e acesse:
- **Frontend**: http://localhost:5500
- **API Gateway**: http://localhost:8080

### Populando o Banco
Os dados são populados automaticamente via scripts SQL executados no container PostgreSQL.

## Deploy no Kubernetes

### Pré-requisitos
- Cluster Kubernetes (minikube, kind, ou cloud)
- `kubectl` configurado
- Nginx Ingress Controller instalado

### Deploy do Sistema
```bash
cd k8s
chmod +x deploy.sh
./deploy.sh apply
```

### Deploy da Observabilidade
```bash
cd k8s/monitoring
chmod +x deploy.sh
./deploy.sh apply
```

### Configurar /etc/hosts
```
127.0.0.1 escola.local
127.0.0.1 grafana.escola.local
127.0.0.1 prometheus.escola.local
```

### Acessar
- **Sistema**: http://escola.local
- **Grafana**: http://grafana.escola.local (admin / admin123)
- **Prometheus**: http://prometheus.escola.local

### Remover
```bash
cd k8s/monitoring && ./deploy.sh delete
cd k8s && ./deploy.sh delete
```

## Resiliência (Resilience4j)

Os microsserviços utilizam **Circuit Breaker** e **Retry** para evitar falhas em cascata:

```
1. Request chega ao servico
2. Servico tenta chamar outro servico (ex: matricula -> cadastro)
3. Se falhar → Retry (ate 3x com backoff exponencial)
4. Se continuar falhando → Circuit Breaker registra falha
5. Apos 50% de falha em 10 chamadas → Circuito ABRE
6. Proximas chamadas → Fallback imediato (sem tentar chamar)
7. Apos 30s → HALF-OPEN (testa 3 chamadas)
8. Se funcionar → Circuito FECHA (volta ao normal)
```

### Circuit Breakers configurados

| Serviço | Circuit Breaker | Protege contra |
|---|---|---|
| disciplina-service | `cadastroService` | cadastro-service fora do ar |
| matricula-service | `cadastroService` | cadastro-service fora do ar |
| matricula-service | `disciplinaService` | disciplina-service fora do ar |
| gateway-service | `cadastroRoute` | cadastro-service fora do ar |
| gateway-service | `matriculaRoute` | matricula-service fora do ar |
| gateway-service | `disciplinaRoute` | disciplina-service fora do ar |

O estado dos circuit breakers é visível em:
- `/actuator/health` (health indicator)
- Grafana dashboard (painel "Circuit Breaker - Estado")

## Observabilidade

### Health Checks (Spring Actuator)

Cada microsserviço expõe:

| Endpoint | Função |
|---|---|
| `/actuator/health` | Status geral do serviço |
| `/actuator/health/readiness` | Pronto para receber tráfego? |
| `/actuator/health/liveness` | Processo está vivo? |
| `/actuator/prometheus` | Métricas no formato Prometheus |
| `/actuator/info` | Informações do serviço |

### Prometheus

Coleta métricas de todos os serviços a cada 10-15 segundos. Alertas configurados:

| Alerta | Condição | Severidade |
|---|---|---|
| ServicoIndisponivel | Serviço fora do ar >1min | critical |
| MemoriaJVMAlta | Heap >80% por >5min | warning |
| TaxaErro5xxAlta | >5% de 5xx por >2min | warning |
| CircuitBreakerAberto | Circuit breaker OPEN >30s | critical |
| LatenciaAlta | P95 >2s por >3min | warning |

### Grafana (Dashboard pré-configurado)

O dashboard "Sistema Escolar - Visão Geral" inclui 10 painéis:

| Painel | Métrica |
|---|---|
| Serviços Online | Contador de serviços UP |
| Status dos Serviços | Tabela UP/DOWN |
| Requisições HTTP/s | Taxa de requests por serviço |
| Latência P95 | Percentil 95 por serviço |
| Erros 4xx e 5xx | Taxa de erros |
| Circuit Breaker Estado | CLOSED / OPEN / HALF-OPEN |
| JVM Heap Memory | Uso de memória da JVM |
| JVM Threads | Threads ativas |
| Conexões HikariCP | Conexões ativas/idle com o banco |
| Resilience4j Sucesso/Falha | Chamadas com sucesso vs falha |

## CI/CD (GitHub Actions)

### Pipeline CI (`ci.yml`) — a cada push/PR

```
Push/PR para main
    |
    +-- [build-and-test] 4 jobs em paralelo (mvn clean verify)
    +-- [validate-k8s]   kubeconform valida manifests YAML
    +-- [ci-summary]     verifica se tudo passou
```

### Pipeline CD (`cd.yml`) — a cada tag v* ou manual

```
Tag v1.0.0
    |
    +-- [build-and-push] 5 imagens Docker em paralelo → ghcr.io
    +-- [cd-summary]     lista imagens publicadas
```

Para publicar imagens:
```bash
git tag v1.0.0
git push origin v1.0.0
```

## Endpoints da API (via Gateway)

Todos os endpoints são acessados através do gateway em `http://localhost:8080`:

### Cadastro Service
- `GET /cadastro/alunos` — Listar alunos
- `POST /cadastro/alunos` — Cadastrar aluno
- `PUT /cadastro/alunos/{id}` — Atualizar aluno
- `DELETE /cadastro/alunos/{id}` — Deletar aluno
- `GET /cadastro/professores` — Listar professores
- `POST /cadastro/professores` — Cadastrar professor
- `PUT /cadastro/professores/{id}` — Atualizar professor
- `DELETE /cadastro/professores/{id}` — Deletar professor

### Disciplina Service
- `GET /disciplina/disciplinas` — Listar disciplinas
- `POST /disciplina/disciplinas` — Cadastrar disciplina
- `PUT /disciplina/disciplinas/{id}` — Atualizar disciplina
- `DELETE /disciplina/disciplinas/{id}` — Deletar disciplina
- `GET /disciplina/periodos` — Listar períodos
- `POST /disciplina/periodos` — Cadastrar período

### Matrícula Service
- `GET /matricula/matriculas` — Listar matrículas
- `POST /matricula/matriculas` — Realizar matrícula
- `GET /matricula/matriculas/aluno/{alunoId}` — Matrículas por aluno
- `PATCH /matricula/matriculas/{id}/status` — Atualizar status
- `DELETE /matricula/matriculas/{id}` — Cancelar matrícula
- `POST /matricula/matriculas/finalizar-semestre` — Finalizar semestre

## Componentes Kubernetes

### Namespace `escola-system`

| Recurso | Nome | Função |
|---|---|---|
| Namespace | escola-system | Isolamento lógico |
| ConfigMap | escola-config | URLs dos serviços |
| Secret | db-credentials | Credenciais do PostgreSQL |
| StatefulSet | postgresql | Banco de dados com PVC |
| Deployment (2 réplicas) | cadastro-service | Microsserviço de cadastro |
| Deployment (2 réplicas) | disciplina-service | Microsserviço de disciplinas |
| Deployment (2 réplicas) | matricula-service | Microsserviço de matrículas |
| Deployment (2 réplicas) | gateway-service | API Gateway |
| Deployment (2 réplicas) | frontend | Nginx com proxy reverso |
| Ingress | escola-ingress | Entrada única + rate limiting |
| HPA | cadastro/matricula/gateway-hpa | Autoscaling por CPU/memória |
| PDB | *-pdb | Garante mín 1 pod por serviço |
| NetworkPolicy | 7 policies | Controle de tráfego (least privilege) |

### Namespace `monitoring`

| Recurso | Nome | Função |
|---|---|---|
| Deployment | prometheus | Coleta de métricas |
| Deployment | grafana | Dashboards |
| Ingress | monitoring-ingress | Acesso externo |

## Estratégia de Persistência

> **Nota:** Por se tratar de um trabalho acadêmico, este projeto utiliza um **banco de dados único** compartilhado por todos os microsserviços. A camada de aplicação foi escrita respeitando a separação lógica de dados, facilitando uma futura migração para bancos independentes.

## Estrutura do Projeto

```
sistema-de-gerenciamento-escolar/
├── cadastro-service/              # Microsserviço de cadastro (Java/Spring Boot)
├── disciplina-service/            # Microsserviço de disciplinas
├── matricula-service/             # Microsserviço de matrículas
├── gateway-service/               # API Gateway
├── frontend/                      # Interface web (HTML/CSS/JS)
│   ├── pages/                     # Páginas por domínio
│   ├── js/services/               # Camada de comunicação com API
│   └── js/config.js               # Configurações
├── k8s/                           # Manifests Kubernetes
│   ├── 00-namespace.yaml          # Namespace escola-system
│   ├── 01-configmap.yaml          # ConfigMap com URLs dos serviços
│   ├── 01-secrets.yaml            # Secret com credenciais do banco
│   ├── 02-postgresql.yaml         # StatefulSet + PVC + Service do PostgreSQL
│   ├── 03-cadastro-service.yaml   # Deployment + Service (2 réplicas)
│   ├── 04-disciplina-service.yaml # Deployment + Service (2 réplicas)
│   ├── 05-matricula-service.yaml  # Deployment + Service (2 réplicas)
│   ├── 06-gateway-service.yaml    # Deployment + Service (2 réplicas)
│   ├── 07-frontend.yaml           # Deployment + Nginx ConfigMap + Service
│   ├── 08-ingress.yaml            # Ingress com rate limiting
│   ├── 09-hpa.yaml                # HorizontalPodAutoscaler
│   ├── 10-pdb.yaml                # PodDisruptionBudgets
│   ├── 11-network-policies.yaml   # NetworkPolicies (least privilege)
│   ├── deploy.sh                  # Script de deploy/delete
│   └── monitoring/                # Stack de observabilidade
│       ├── 00-namespace.yaml
│       ├── 01-prometheus-config.yaml  # Scrape configs + alertas
│       ├── 02-prometheus.yaml         # Deployment + RBAC + PVC
│       ├── 03-grafana-config.yaml     # Datasource + dashboard JSON
│       ├── 04-grafana.yaml            # Deployment + PVC
│       ├── 05-ingress.yaml            # Ingress para Grafana/Prometheus
│       └── deploy.sh
├── .github/workflows/
│   ├── ci.yml                     # CI: build, test, validação K8s
│   └── cd.yml                     # CD: build e push de imagens Docker
├── docker-compose.yml             # Orquestração local
├── 01_reset_database.sql          # Script de reset do banco
├── 02_populate_database.sql       # Script de população
└── README.md
```

## Desenvolvimento Local (sem Docker)

### Pré-requisitos
- Java 21
- Maven
- PostgreSQL

### Passos
1. Criar banco `escola_db` no PostgreSQL
2. Executar scripts SQL (`01_reset_database.sql` e `02_populate_database.sql`)
3. Iniciar serviços em terminais separados:
   ```bash
   cd cadastro-service && mvn spring-boot:run
   cd disciplina-service && mvn spring-boot:run
   cd matricula-service && mvn spring-boot:run
   cd gateway-service && mvn spring-boot:run
   ```
4. Abrir `frontend/index.html` com Live Server no VS Code

---
*Este projeto é um componente prático para a disciplina de Arquitetura de Software, demonstrando princípios de microsserviços, Kubernetes, observabilidade e CI/CD.*
