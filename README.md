# 📚 Library API

Uma API RESTful robusta para gerenciamento de bibliotecas, empréstimos de livros e controle de usuários. Construída com as melhores práticas de mercado utilizando Spring Boot, arquitetura em camadas e conteinerização completa com Docker.

---

## 🚀 Tecnologias Utilizadas

Este projeto foi desenvolvido com as seguintes tecnologias:

* **Java 25**
* **Spring Boot 3** (Web, Data JPA, Validation)
* **PostgreSQL** (Banco de dados relacional)
* **Flyway** (Migrações e versionamento de banco de dados)
* **Docker & Docker Compose** (Orquestração de infraestrutura)
* **Integração de API Externa** (ViaCEP para validação de endereços)
* **Swagger / OpenAPI 3** (Documentação automatizada)

---

## ✨ Funcionalidades (Features)

A API possui um domínio rico focado na segurança e integridade dos dados:

* **Gestão de Usuários (Pessoas):**
    * Criação de usuários com criptografia de senhas.
    * Validação de unicidade para CPF e E-mail no banco de dados.
    * Validação automática de CEP em tempo real via **API ViaCEP**.
    * Separação de privilégios (`ADMIN` e `USER`).
* **Gestão de Livros:**
    * Cadastro de livros utilizando `LocalDate` no padrão ISO-8601.
* **Gestão de Empréstimos:**
    * Vinculação entre Usuários e Livros.
    * Soft delete para devoluções de livros.
* **Segurança de Dados (DTOs):**
    * Isolamento completo entre a camada de visualização e o banco de dados utilizando `Request DTOs` e `Response DTOs` para evitar o vazamento de dados sensíveis (como senhas e CPFs).
* **Tratamento de Exceções:**
    * Respostas HTTP padronizadas para erros de validação, não encontrados (404) e violações de regras de negócio (400).

---

## ⚙️ Arquitetura do Projeto

O código segue o princípio de **Separation of Concerns (Separação de Preocupações)**, dividido em:

1.  **Controllers (`/controller`):** Camada Web, responsável exclusiva por receber as requisições HTTP e retornar os Status Codes corretos.
2.  **Services (`/service`):** Concentra todas as regras de negócio, injeções de dependência e integrações externas. Não possui acoplamento com a camada Web.
3.  **Repositories (`/repositores`):** Interfaces de comunicação com o banco de dados via Spring Data JPA.
4.  **Models (`/models`):** Entidades espelhadas nas tabelas do banco de dados relacional.
5.  **DTOs (`/dto`):** Objetos de transferência de dados que definem contratos estritos de entrada (`request`) e saída (`response`).

---

## 🛠️ Como Executar o Projeto Localmente

### Pré-requisitos
Certifique-se de ter instalado em sua máquina:
* [Git](https://git-scm.com/)
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) (ou Docker Engine/Compose ativo)

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/MarcoTFranco/emakers-backend-trainee-2026.git
    cd library-api
    ```

2.  **Gere o pacote compilado (Executável `.jar`):**
    No terminal, na raiz do projeto, utilize o Maven Wrapper para compilar a aplicação sem precisar instalar o Maven no sistema operativo:
    ```bash
    # No Windows:
    .\mvnw clean package -DskipTests
    
    # No Linux/Mac:
    ./mvnw clean package -DskipTests
    ```

3.  **Suba a infraestrutura completa (API + Banco de Dados + pgAdmin):**
    Com o arquivo `.jar` gerado na pasta `target`, utilize o Docker Compose para criar a rede interna, subir o banco Postgres e iniciar a aplicação:
    ```bash
    docker compose up -d --build
    ```

A API estará disponível e pronta para receber requisições na porta `8080`.

---

## 📖 Documentação da API (Swagger)

A documentação interativa de todos os endpoints foi gerada automaticamente pelo Swagger.
Com a aplicação rodando, acesse no seu navegador:

* **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

---

## 🗄️ Acesso ao Banco de Dados (pgAdmin)

O `docker-compose` inclui um contêiner do pgAdmin para gerenciamento visual do banco de dados.

* **URL:** `http://localhost:5050`
* **E-mail:** `admin@admin.com`
* **Senha:** `root`

**Para conectar o servidor dentro do pgAdmin:**
* **Host name/address:** `db`
* **Port:** `5432`
* **Username:** `user`
* **Password:** `password`