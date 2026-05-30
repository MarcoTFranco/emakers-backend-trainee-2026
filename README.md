# 📚 Library API

API RESTful para gerenciamento de bibliotecas — controle de acervo, usuários e empréstimos de livros. Desenvolvida com Spring Boot 4, autenticação JWT via RSA, versionamento de banco com Flyway e infraestrutura completa em Docker.

---

## 🚀 Tecnologias

| Tecnologia | Uso |
|---|---|
| **Java 25** | Linguagem principal |
| **Spring Boot 4.0.6** | Framework base (Web, Data JPA, Validation, Security) |
| **Spring Security 6 + OAuth2 Resource Server** | Autenticação e autorização JWT com chaves RSA-256 |
| **PostgreSQL** | Banco de dados relacional |
| **Flyway** | Versionamento e migrações de banco de dados |
| **Docker & Docker Compose** | Containerização e orquestração |
| **ViaCEP** | API externa para validação de endereços (CEP) |
| **Swagger / OpenAPI 3** | Documentação interativa da API |

---

## 🗂️ Modelo de Dados

```
┌─────────────┐       ┌────────────────┐       ┌─────────────┐
│   TB_BOOKS  │       │   TB_LOANS     │       │  TB_PERSON  │
│─────────────│       │────────────────│       │─────────────│
│ id (UUID)   │◄──────│ book_id (FK)   │──────►│ id (UUID)   │
│ title       │       │ person_id (FK) │       │ name        │
│ author      │       │ active (bool)  │       │ cpf         │
│ publication │       └────────────────┘       │ email       │
│   _date     │                                │ zip_code    │
└─────────────┘                                │ password    │
                                               │ role        │
                                               └─────────────┘
```

- **Livro ↔ Pessoa**: relação N:M mediada por `TB_LOANS`
- **Soft delete**: devoluções marcam `active = false`, preservando o histórico de empréstimos

---

## ✨ Funcionalidades

**Autenticação e Autorização**
- Login via `POST /authenticate` retorna JWT assinado com RSA-256
- Dois perfis: `ADMIN` (acesso total) e `USER` (acesso às próprias rotas)
- Ownership checks em todas as rotas sensíveis — usuário só altera seus próprios dados

**Gestão de Usuários**
- Cadastro com validação de CPF (algoritmo Módulo 11), e-mail e CEP em tempo real via ViaCEP
- Normalização de e-mail (case-insensitive) para evitar duplicatas por capitalização
- Atualização de perfil (nome, e-mail, CEP) separada de troca de senha
- Troca de senha com confirmação da senha atual
- Primeiro administrador provisionado via variáveis de ambiente (sem credenciais hardcoded no código)

**Gestão de Livros**
- CRUD completo — criação, listagem, busca, atualização e exclusão
- Datas de publicação no padrão ISO-8601 (`LocalDate`)

**Gestão de Empréstimos**
- Empréstimo identificado por `bookId` e `personId` (UUIDs — sem tráfego de CPF na requisição)
- Trava de empréstimo duplicado: mesma pessoa não pode pegar o mesmo livro duas vezes sem devolver
- Limite de 5 empréstimos ativos simultâneos por usuário
- Devolução por ID do empréstimo (soft delete)

**Qualidade e Segurança**
- Tratamento global de exceções via `@RestControllerAdvice` (respostas padronizadas)
- DTOs de entrada e saída isolam a camada de persistência (sem vazamento de senha ou hash)
- Validações Bean Validation em todas as entradas HTTP

---

## 🔐 Fluxo de Autenticação

```
1. POST /authenticate  { "email": "...", "password": "..." }
        │
        ▼
2. JWT retornado  { "token": "eyJ..." }
        │
        ▼
3. Authorization: Bearer <token>  em todas as requisições protegidas
```

O token JWT contém `sub` (e-mail), `scope` (ADMIN ou USER) e `userId` (UUID imutável da pessoa, estável mesmo após troca de e-mail).

---

## 📋 Endpoints

### Autenticação

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| `POST` | `/authenticate` | Público | Login — retorna JWT |

### Usuários

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| `POST` | `/users` | Público | Cadastrar novo usuário |
| `POST` | `/users/admin` | ADMIN | Cadastrar novo administrador |
| `GET` | `/users` | ADMIN | Listar todos os usuários |
| `GET` | `/users/{id}` | Autenticado | Buscar usuário por ID |
| `PUT` | `/users/{id}` | Dono ou ADMIN | Atualizar perfil (nome, e-mail, CEP) |
| `PUT` | `/users/{id}/change-password` | Dono | Trocar senha |
| `DELETE` | `/users/{id}` | ADMIN | Remover usuário |

### Livros

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| `GET` | `/books` | Público | Listar todos os livros |
| `GET` | `/books/{id}` | Público | Buscar livro por ID |
| `POST` | `/books` | ADMIN | Cadastrar livro |
| `PUT` | `/books/{id}` | ADMIN | Atualizar livro |
| `DELETE` | `/books/{id}` | ADMIN | Remover livro |

### Empréstimos

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| `POST` | `/loans` | Autenticado | Realizar empréstimo |
| `DELETE` | `/loans/{id}` | Dono ou ADMIN | Devolver livro |

> **Body do empréstimo:**
> ```json
> { "bookId": "uuid-do-livro", "personId": "uuid-da-pessoa" }
> ```

---

## 🛠️ Como Executar Localmente

### Pré-requisitos

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (ou Docker Engine + Compose)

### Passo a Passo

**1. Clone o repositório**

```bash
git clone https://github.com/MarcoTFranco/emakers-backend-trainee-2026.git
cd emakers-backend-trainee-2026
```

**2. Configure as credenciais do primeiro administrador**

Copie o arquivo de exemplo e preencha com dados reais. O arquivo `.env` **nunca** deve ser commitado:

```bash
cp .env.example .env
```

Edite o `.env`:

```env
ADMIN_EMAIL=admin@suaempresa.com
ADMIN_PASSWORD=senha_forte_aqui
ADMIN_CPF=111.444.777-35
ADMIN_NAME=Administrator    # opcional
ADMIN_CEP=01310-100         # opcional
```

> O `AdminInitializer` cria o admin automaticamente na primeira inicialização.
> Nas execuções seguintes, se já existir um admin no banco, o passo é ignorado.

**3. Compile o projeto**

```bash
# Windows
.\mvnw clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```

**4. Suba a infraestrutura completa**

```bash
docker compose up -d --build
```

A API estará disponível em `http://localhost:8080`.

---

## 📖 Documentação Interativa (Swagger)

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 🗄️ Acesso ao Banco de Dados (pgAdmin)

| Campo | Valor |
|-------|-------|
| URL | `http://localhost:5050` |
| E-mail | `admin@admin.com` |
| Senha | `root` |

**Para adicionar o servidor no pgAdmin:**

| Campo | Valor |
|-------|-------|
| Host | `db` |
| Port | `5432` |
| Username | `user` |
| Password | `password` |

---

## 🗃️ Migrations (Flyway)

O schema é gerenciado pelo Flyway. As migrations estão em `src/main/resources/db/migration/`:

| Versão | Descrição |
|--------|-----------|
| `V1` | Cria tabela `TB_BOOKS` |
| `V2` | Cria tabela `TB_PERSON` |
| `V3` | Cria tabela `TB_LOANS` |
| `V4` | Adiciona coluna `active` em `TB_LOANS` |
| `V5` | Adiciona coluna `role` em `TB_PERSON` |
| `V6` | Corrige tipo da coluna `publication_date` para `date` |

---

## ⚙️ Arquitetura

```
src/main/java/com/emakers/library_api/
├── config/          # SecurityConfig, GlobalExceptionHandler, AdminInitializer
├── controller/      # Camada HTTP — recebe requisições, devolve status codes
├── service/         # Regras de negócio e integrações externas
├── repositores/     # Interfaces Spring Data JPA
├── models/          # Entidades JPA
├── dto/
│   ├── request/     # Contratos de entrada (validados com Bean Validation)
│   └── response/    # Contratos de saída (sem dados sensíveis)
└── validation/      # Validador customizado de CPF (algoritmo Módulo 11)
```

O projeto segue o princípio de **Separação de Responsabilidades**: controllers não contêm regras de negócio e services não conhecem a camada HTTP.
