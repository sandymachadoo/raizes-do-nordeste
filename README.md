# Raízes do Nordeste — Back-end API

Solução REST para o estudo de caso da franquia **Raízes do Nordeste**. A aplicação centraliza operações de unidades, cardápio, estoque local, pedidos multicanal, pagamento simulado, programa de fidelidade e trilha de auditoria, com autenticação JWT e perfis de acesso.

---

## Evidências (entrega técnica)

| Item | Link / localização |
|---|---|
| **Repositório GitHub** | https://github.com/sandymachadoo/raizes-do-nordeste |
| **Swagger (local)** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON (local)** | http://localhost:8080/v3/api-docs |
| **Coleção Postman** | [`raizes-postman-collection.json`](raizes-postman-collection.json) |
| **DER (PlantUML)** | [`docs/der.puml`](docs/der.puml) — exportar PNG/PDF para entrega |
| **Deploy público** | Não aplicável (execução local — ver [Quick start](#quick-start-passo-a-passo)) |

**Exportar o DER:** abra `docs/der.puml` no IntelliJ/VS Code (plugin PlantUML) ou em [plantuml.com](https://www.plantuml.com/plantuml) e exporte PNG/PDF.

### Como o avaliador acessa

1. Clone o repositório público: `git clone https://github.com/sandymachadoo/raizes-do-nordeste.git`
2. Configure MySQL e variáveis ([seção c](#c-variáveis-de-ambiente)).
3. Execute `mvnw.cmd spring-boot:run` ([seção f](#f-iniciar-a-api)).
4. Abra o Swagger: http://localhost:8080/swagger-ui.html
5. Importe no Postman: arquivo `raizes-postman-collection.json` (raiz do projeto).

> O Swagger roda **localmente** após subir a API. Não há deploy em nuvem neste MVP.

---

## Quick start (passo a passo)

1. Instale os [requisitos](#b-requisitos) (Java 21, Maven, MySQL 8).
2. Crie o banco `raizes_db` ou deixe a URL JDBC criar automaticamente ([seção e](#e-banco-de-dados-migrations-e-seed)).
3. Copie e ajuste variáveis: `cp .env.example .env` ([seção c](#c-variáveis-de-ambiente)).
4. Instale dependências: `mvnw.cmd clean install` ([seção d](#d-instalar-dependências)).
5. Inicie a API: `mvnw.cmd spring-boot:run` ([seção f](#f-iniciar-a-api)).
6. Acesse a documentação: [Swagger](#g-documentação-swaggeropenapi).
7. Execute os testes: [seção h](#h-testes).

Base URL local: `http://localhost:8080`

---

## b) Requisitos

### Linguagem e runtime

| Item | Versão |
|---|---|
| **Java** | 21 |
| **Maven** | 3.9+ (wrapper incluso: `mvnw` / `mvnw.cmd`) |
| **MySQL** | 8+ (porta padrão `3306`) |

### Banco de dados

- MySQL 8+ com banco `raizes_db` (pode ser criado automaticamente pela URL JDBC).
- Usuário/senha configuráveis (padrão local: `root` / `root`).

### Dependências principais (Maven)

Gerenciadas automaticamente pelo `pom.xml`:

| Dependência | Uso |
|---|---|
| Spring Boot 3.5 Web | API REST |
| Spring Data JPA | Persistência ORM |
| Spring Security + JWT | Autenticação e autorização |
| MySQL Connector | Driver JDBC |
| Spring Validation | Validação de DTOs |
| Springdoc OpenAPI | Swagger / OpenAPI |
| Spring Actuator | Health checks (disponibilidade) |
| Resilience4j | Retry e circuit breaker (pagamento mock) |
| Lombok | Redução de boilerplate |

---

## c) Variáveis de ambiente

### Arquivo `.env.example`

Na raiz do projeto há um modelo com todas as variáveis:

```bash
cp .env.example .env
```

Edite `.env` com seus valores locais. **Não commite o arquivo `.env`** (está no `.gitignore`).

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do MySQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta do JWT (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MS` | Expiração do token em milissegundos |
| `SERVER_PORT` | Porta da API (padrão `8080`) |

### Como a aplicação lê as variáveis

O `application.properties` usa placeholders com fallback:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/raizes_db?...}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}
jwt.secret=${JWT_SECRET:raizes-do-nordeste-jwt-secret-key-minimo-32-chars}
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
server.port=${SERVER_PORT:8080}
```

Você pode configurar via:

- variáveis de ambiente do sistema/terminal,
- arquivo `.env` (com plugin do IDE ou export manual),
- ou edição direta de `src/main/resources/application.properties`.

---

## d) Instalar dependências

Na raiz do projeto:

```bash
# Windows
mvnw.cmd clean install

# Linux / macOS
./mvnw clean install
```

O Maven baixa todas as dependências do `pom.xml` e compila o projeto. É necessário internet na primeira execução.

---

## e) Banco de dados, migrations e seed

### Criar o banco

**Opção A — automática:** a URL JDBC inclui `createDatabaseIfNotExist=true`.

**Opção B — manual (MySQL Workbench ou CLI):**

```sql
CREATE DATABASE IF NOT EXISTS raizes_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Migrations (schema)

Este projeto **não utiliza Flyway/Liquibase**. O schema é gerenciado pelo **Hibernate**:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Na **primeira execução** da API, as tabelas são criadas/atualizadas automaticamente (`usuarios`, `pedidos`, `produtos`, etc.).

### Seed (dados iniciais)

**Não há seed automático** no repositório. Para demonstrar o fluxo:

**1. Usuário ADMIN (MySQL — uma vez):**

```sql
INSERT INTO usuarios (nome, email, senha_hash, role, ativo, consentimento_lgpd, data_consentimento_lgpd)
VALUES (
  'Administrador',
  'admin@raizes.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  'ADMIN',
  true,
  true,
  NOW()
);
```

Senha: `password`

**2. Demais dados (Postman ou Swagger):**

| Ordem | Endpoint | Observação |
|---|---|---|
| 1 | `POST /auth/registro` | Cliente com `consentimentoLgpd: true` |
| 2 | `POST /auth/login` | Obter JWT |
| 3 | `POST /unidades` | Requer token ADMIN |
| 4 | `POST /produtos` | ADMIN ou GERENTE |
| 5 | `POST /estoque/entrada` | Quantidade na unidade |
| 6 | `POST /pedidos` | Cliente; `canalPedido` obrigatório |

Coleção Postman: `raizes-postman-collection.json`

---

## f) Iniciar a API

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Aguarde no console: `Started RaizesDoNordesteApplication`.

Verifique:

```text
GET http://localhost:8080/actuator/health   → {"status":"UP"}
GET http://localhost:8080/unidades          → [] ou lista de unidades
```

> **Porta em uso:** altere `SERVER_PORT` no `.env` ou pare a instância anterior.

---

## g) Documentação (Swagger / OpenAPI)

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

### Autenticar no Swagger

1. `POST /auth/login` ou `POST /auth/registro`
2. Copie o campo `token` da resposta
3. Clique em **Authorize**
4. Informe: `Bearer {token}`

### Documentação complementar (TCC)

| Documento | Conteúdo |
|---|---|
| `docs/API_ENDPOINTS.md` | Checklist por endpoint (roteiro 5.3) |
| `docs/LGPD.md` | Dados pessoais, consentimento, controles |
| `docs/RNF_IMPLEMENTACAO.md` | Desempenho, disponibilidade, resiliência |
| `docs/PROMOCOES_CAMPANHAS.md` | Promoções (conceitual) |
| `PLANO_DE_TESTES.md` | Casos de teste |

---

## h) Testes

### Testes automatizados (Maven)

```bash
# Windows
mvnw.cmd test

# Linux / macOS
./mvnw test
```

Inclui teste de contexto Spring Boot e teste de integração do Actuator (`ActuatorHealthIntegrationTest`). Requer **MySQL acessível** com as credenciais configuradas.

### Testes manuais (Postman)

Importe `raizes-postman-collection.json` e siga o fluxo em `PLANO_DE_TESTES.md`.

### Teste de carga (k6 — opcional)

Pré-requisito: [k6 instalado](https://k6.io/docs/get-started/installation/)

```bash
k6 run scripts/k6-load-test.js
```

Com URL customizada:

```bash
k6 run -e BASE_URL=http://localhost:8080 scripts/k6-load-test.js
```

---

## Fluxo principal (MVP)

```
POST /auth/registro
POST /auth/login
POST /unidades
POST /produtos
POST /estoque/entrada
POST /pedidos                    → canalPedido obrigatório
POST /pagamentos/solicitar/{id}
POST /pagamentos/callback        → APROVADO ou NEGADO
PATCH /pedidos/{id}/status       → EM_PREPARO → PRONTO → ENTREGUE
```

**Exemplo de pedido:**

```json
{
  "unidadeId": 1,
  "canalPedido": "WEB",
  "itens": [{ "produtoId": 1, "quantidade": 1 }]
}
```

**Exemplo de callback mock:**

```json
{
  "transactionId": "uuid-retornado-na-solicitacao",
  "status": "APROVADO"
}
```

---

## Organização por recurso (API)

| Módulo | Responsabilidade |
|---|---|
| `/auth` | Registro de cliente e login |
| `/usuarios` | Gestão de usuários internos |
| `/unidades` | Consulta e manutenção de lojas |
| `/produtos` | Cardápio e disponibilidade por unidade |
| `/estoque` | Entrada, saída e consulta por unidade |
| `/pedidos` | Criação, consulta, cancelamento e status |
| `/pagamentos` | Solicitação mock e callback |
| `/fidelidade` | Saldo, histórico e resgate |
| `/auditoria` | Histórico de ações sensíveis (ADMIN) |
| `/actuator` | Health, liveness e readiness |

Detalhamento completo: `docs/API_ENDPOINTS.md`

---

## Arquitetura (camadas)

```
src/main/java/com/raizesdonordeste/
├── api/              → Controllers, DTOs, exceções HTTP
├── application/      → Services (casos de uso)
├── domain/           → Entidades JPA e enums
└── infrastructure/   → Repositories, Security, config, integrações
```

---

## Padrão de erro

```json
{
  "timestamp": "2026-06-26T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Recurso não encontrado.",
  "path": "/produtos/99"
}
```

---

## Entregáveis complementares

| Artefato | Arquivo |
|---|---|
| Coleção Postman | `raizes-postman-collection.json` (fluxo principal + pasta **Erros**) |
| DER (PlantUML) | `docs/der.puml` — exportar PNG/PDF para o TCC |
| Plano de testes | `PLANO_DE_TESTES.md` |
| Endpoints (checklist) | `docs/API_ENDPOINTS.md` |
| LGPD | `docs/LGPD.md` |
| RNFs | `docs/RNF_IMPLEMENTACAO.md` |
| Promoções (conceitual) | `docs/PROMOCOES_CAMPANHAS.md` |
| Teste de carga k6 | `scripts/k6-load-test.js` |
| Variáveis de ambiente | `.env.example` |

---

## Limitações conhecidas

- Pagamento externo é **mock interno**.
- Schema via Hibernate `update` (sem Flyway/Liquibase).
- Sem seed automático; ADMIN inicial via SQL.
- Sem paginação nas listagens (`page`/`limit`).
- Promoções apenas documentadas (`docs/PROMOCOES_CAMPANHAS.md`).

---

## Licença

Projeto acadêmico — uso educacional.
