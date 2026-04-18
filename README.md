# Discentes
Julliely de Sousa, Fernanda Ribeiro e João Marcelo Pereira

# Sistema de Gerenciamento Escolar

Este projeto é uma atividade acadêmica desenvolvida com o objetivo de compreender e aplicar os princípios fundamentais da **Arquitetura de Microsserviços**. O sistema simula o fluxo de uma instituição de ensino, abrangendo desde o cadastro base até a lógica de matrículas e validação de pré-requisitos.

## 🏗️ Arquitetura de Microsserviços

O sistema foi estruturado para demonstrar o desacoplamento entre diferentes domínios de negócio. No frontend, a aplicação foi organizada para consumir serviços independentes, permitindo que cada módulo (Alunos, Professores, Disciplinas e Matrículas) funcione como uma unidade lógica separada.

### Diagrama Conceitual da Solução
```text
[ Frontend (Interface Web) ]
           |
           +--- (REST API) ---> [ Cadastro Service (Alunos/Professores) - Porta 8081 ]
           +--- (REST API) ---> [ Disciplina Service (Disciplinas/Períodos) - Porta 8083 ]
           +--- (REST API) ---> [ Matrícula Service (Matrículas) - Porta 8082 ]
```

### Divisão de Responsabilidades:
- **Cadastro Service** (Porta 8081): Gestão de dados cadastrais dos estudantes e professores.
- **Disciplina Service** (Porta 8083): Organização da grade curricular, períodos e validação de professores.
- **Matrícula Service** (Porta 8082): Lógica de negócio para enturmação, verificação de alunos e disciplinas existentes.

## 🗄️ Estratégia de Persistência (Banco de Dados)

Em uma implementação de **Microsserviços Puros**, cada serviço deve possuir seu próprio banco de dados isolado (*Database per Service*), garantindo que uma alteração no esquema de um serviço não afete os outros.

> **Nota do Projeto:** Por se tratar de um trabalho acadêmico de pequeno porte, este projeto utiliza um **banco de dados único**. Esta decisão foi tomada para simplificar a infraestrutura e facilitar o deploy da atividade, embora a camada de aplicação tenha sido escrita respeitando a separação lógica de dados, facilitando uma futura migração para bancos independentes.

## 🚀 Tecnologias Utilizadas

- **Backend**: Java 21, Spring Boot 3.5.13, Spring Data JPA, Hibernate
- **Banco de Dados**: PostgreSQL 15
- **Frontend**: HTML5, CSS3 e JavaScript (Vanilla JS)
- **Integração**: Fetch API com padrão Service Pattern
- **Orquestração**: Docker Compose
- **Arquitetura**: Microsserviços com API Gateway

## 🐳 Executando com Docker Compose

Este projeto inclui um `docker-compose.yml` que orquestra todos os serviços:

- **PostgreSQL**: Banco de dados (porta 5432)
- **cadastro-service**: Gestão de alunos e professores (porta 8081)
- **disciplina-service**: Gestão de disciplinas e períodos (porta 8083)
- **matricula-service**: Gestão de matrículas (porta 8082)
- **gateway-service**: API Gateway para roteamento (porta 8080)
- **frontend**: Interface web (porta 5500)

### Passos para Executar:
1. Certifique-se de ter Docker e Docker Compose instalados
2. Na raiz do projeto, execute:
   ```bash
   docker compose up --build
   ```
3. Aguarde os serviços subirem (pode levar alguns minutos na primeira vez)
4. Acesse:
   - **Frontend**: http://localhost:5500
   - **API Gateway**: http://localhost:8080

### Populando o Banco:
Os dados são populados automaticamente via scripts SQL executados no container PostgreSQL.

## 📡 Endpoints da API (via Gateway)

Todos os endpoints são acessados através do gateway em `http://localhost:8080`:

### Cadastro Service
- `GET /cadastro/alunos` - Listar alunos
- `POST /cadastro/alunos` - Cadastrar aluno
- `GET /cadastro/professores` - Listar professores
- `POST /cadastro/professores` - Cadastrar professor

### Disciplina Service
- `GET /disciplina/disciplinas` - Listar disciplinas
- `POST /disciplina/disciplinas` - Cadastrar disciplina
- `GET /disciplina/periodos` - Listar períodos

### Matrícula Service
- `GET /matricula/matriculas` - Listar matrículas
- `POST /matricula/matriculas` - Realizar matrícula

## 🏗️ Arquitetura Atual

```
[ Frontend (HTML/JS) ] --> [ Gateway Service (porta 8080) ]
                              |
                              +--> [ Cadastro Service (porta 8081) ]
                              +--> [ Disciplina Service (porta 8083) ]
                              +--> [ Matrícula Service (porta 8082) ]
                              |
                              +--> [ PostgreSQL (porta 5432) ]
```

- **Gateway Service**: Centraliza o acesso, lida com CORS e roteia requests para os serviços internos.
- **Comunicação**: REST APIs síncronas entre serviços.
- **Banco**: Compartilhado (PostgreSQL único) para simplicidade acadêmica.

## 📂 Estrutura do Projeto

```
sistema-de-gerenciamento-escolar/
├── cadastro-service/          # Microsserviço de cadastro
├── disciplina-service/        # Microsserviço de disciplinas
├── matricula-service/         # Microsserviço de matrículas
├── gateway-service/           # API Gateway
├── frontend/                  # Interface web
│   ├── pages/                 # Páginas por domínio
│   ├── js/
│   │   ├── services/          # Camada de comunicação
│   │   └── config.js          # Configurações de API
├── docker-compose.yml         # Orquestração Docker
├── 01_reset_database.sql      # Script de reset do banco
├── 02_populate_database.sql   # Script de população
└── README.md
```

## 🔧 Desenvolvimento Local (sem Docker)

### Pré-requisitos
- Java 21
- Maven
- PostgreSQL

### Passos:
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
*Este projeto é um componente prático para a disciplina de Arquitetura de Software, demonstrando princípios de microsserviços.*