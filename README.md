# Raízes do Nordeste — Back-end API

Solução REST para o estudo de caso da franquia **Raízes do Nordeste**. A aplicação centraliza operações de unidades, cardápio, estoque local, pedidos com origem multicanal, pagamento simulado, programa de fidelidade e trilha de auditoria, com autenticação JWT e perfis de acesso.

---

## Requisitos

- **Java 21**
- **Maven 3.9+**
- **MySQL 8+** (porta padrão `3306`)

---

## Configuração

Copie o arquivo de exemplo e ajuste conforme seu ambiente:

```bash
cp .env.example .env
```

Variáveis de referência:

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do MySQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta do token (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MS` | Tempo de expiração do JWT em milissegundos |

Na prática, os valores também podem ser definidos em `src/main/resources/application.properties`. A API sobe localmente com os padrões do projeto, desde que o MySQL esteja ativo.

Exemplo mínimo no `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/raizes_db?createDatabaseIfNotExist=true&serverTimezone=America/Sao_Paulo
spring.datasource.username=root
spring.datasource.password=root

jwt.secret=sua-chave-secreta-com-pelo-menos-32-caracteres
jwt.expiration-ms=86400000
```

---

## Execução

```bash
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

> Em Linux/macOS, use `./mvnw` no lugar de `mvnw.cmd`.

Base URL local:

```
http://localhost:8080
```

---

## Swagger

Documentação interativa:

```
http://localhost:8080/swagger-ui.html
```

**Como autenticar no Swagger**

1. Execute `POST /auth/login` ou registre um cliente em `POST /auth/registro`.
2. Copie o campo `token` da resposta.
3. Clique em **Authorize**.
4. Informe: `Bearer {token}`.

Contrato OpenAPI em JSON:

```
http://localhost:8080/v3/api-docs
```

---

## Banco de dados

O schema é gerenciado pelo Hibernate com `spring.jpa.hibernate.ddl-auto=update`. Na primeira execução, as tabelas são criadas automaticamente se o banco existir ou puder ser criado pela URL JDBC.

Criação manual opcional:

```sql
CREATE DATABASE IF NOT EXISTS raizes_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Dados iniciais

Este repositório **não possui seed automático**. Para validar o fluxo principal, cadastre os registros abaixo (via Swagger ou Postman):

| Ordem | Ação | Observação |
|---|---|---|
| 1 | `POST /auth/registro` | Cliente com consentimento LGPD |
| 2 | `POST /auth/login` | Obter JWT |
| 3 | `POST /usuarios` | Criar perfil ADMIN (requer usuário admin pré-existente ou insert manual) |
| 4 | `POST /unidades` | Cadastrar loja |
| 5 | `POST /produtos` | Cadastrar itens do cardápio |
| 6 | `POST /estoque/entrada` | Informar quantidade na unidade |

---

## Fluxo principal (MVP)

Sequência recomendada para demonstração ponta a ponta:

```
POST /auth/registro
POST /auth/login
POST /unidades
POST /produtos
POST /estoque/entrada
POST /pedidos                    → canalPedido obrigatório
POST /pedidos/{id}/cancelar      → cancelamento explícito (cliente ou equipe)
POST /pagamentos/solicitar/{id}
POST /pagamentos/callback        → status APROVADO ou NEGADO
PATCH /pedidos/{id}/status       → EM_PREPARO → PRONTO → ENTREGUE
```

**Exemplo de pedido**

```json
{
  "unidadeId": 1,
  "canalPedido": "WEB",
  "itens": [
    { "produtoId": 1, "quantidade": 1 }
  ]
}
```

**Exemplo de callback mock**

```json
{
  "transactionId": "uuid-retornado-na-solicitacao",
  "status": "APROVADO"
}
```

---

## Notas sobre a implementação

- **MySQL + JPA:** o modelo relacional favorece integridade entre unidade, estoque, pedido, itens e pagamento, alinhado ao controle operacional da rede.
- **DTOs na borda da API:** entidades não são expostas diretamente; campos sensíveis como `senhaHash` permanecem fora das respostas.
- **Pagamento desacoplado:** `PagamentoService` simula o gateway externo com `transactionId` e callback, permitindo testar aprovação e recusa sem provedor real.
- **Estoque:** na criação do pedido há apenas validação de disponibilidade; a baixa ocorre somente após callback **APROVADO**.
- **Segurança:** JWT stateless + `@PreAuthorize` por perfil (`ADMIN`, `GERENTE`, `ATENDENTE`, `COZINHA`, `CLIENTE`).
- **LGPD:** registro de cliente exige `consentimentoLgpd` e persiste `dataConsentimentoLgpd`.
- **Auditoria:** login, pedidos, estoque, pagamentos e resgates relevantes são registrados; consulta em `GET /auditoria` (ADMIN).
- **Multicanalidade:** pedidos exigem `canalPedido` (`APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB`) e podem ser filtrados por `?canalPedido=`.
- **Cancelamento:** `POST /pedidos/{id}/cancelar` — cliente cancela apenas em `AGUARDANDO_PAGAMENTO`; equipe (atendente/gerente/admin) também em `PAGO` e `EM_PREPARO`, com estorno de estoque e pontos quando aplicável.
- **Desempenho:** virtual threads habilitadas (`spring.threads.virtual.enabled=true`) para operações de I/O.

---

## Estrutura do projeto

```
src/main/java/com/raizesdonordeste/
├── api/
│   ├── controller/     endpoints REST
│   ├── dto/            contratos de entrada e saída
│   └── exception/      erros padronizados (ErrorResponse)
├── application/
│   └── service/        casos de uso e orquestração
├── domain/
│   ├── model/          entidades JPA
│   └── enums/          status, perfis, canais
└── infrastructure/
    ├── config/         Security, OpenAPI
    ├── repository/     Spring Data JPA
    └── security/       JWT, filtros, UserDetails
```

---

## Endpoints por módulo

| Módulo | Responsabilidade |
|---|---|
| `/auth` | Registro de cliente e login |
| `/usuarios` | Gestão de usuários internos |
| `/unidades` | Consulta e manutenção de lojas |
| `/produtos` | Cardápio e disponibilidade por unidade |
| `/estoque` | Entrada, saída e consulta por unidade |
| `/pedidos` | Criação, consulta filtrada, cancelamento e status |
| `/pagamentos` | Solicitação mock e callback |
| `/fidelidade` | Saldo, histórico e resgate |
| `/auditoria` | Histórico de ações sensíveis |

---

## Padrão de erro

Respostas de falha seguem estrutura única:

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

## Testes

```bash
mvnw.cmd test
```

O projeto inclui teste de contexto Spring Boot. Os testes de integração dependem de MySQL acessível com as credenciais configuradas.

---

## Entregáveis complementares

| Artefato | Arquivo |
|---|---|
| Coleção Postman | `raizes-postman-collection.json` |
| Plano de testes | `PLANO_DE_TESTES.md` |
| Promoções (conceitual) | `docs/PROMOCOES_CAMPANHAS.md` |

---

## Limitações atuais

- Pagamento externo é **mock interno**, sem integração real.
- Não há paginação nas listagens (`page`/`limit`).
- Migrations versionadas (Flyway/Liquibase) não foram adotadas; schema via Hibernate `update`.
- Promoções e campanhas: documentação conceitual em `docs/PROMOCOES_CAMPANHAS.md` (sem implementação no MVP).

---

## Licença

Projeto acadêmico — uso educacional.
