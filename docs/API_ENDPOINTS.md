# Documentação da API — Checklist por Endpoint

Referência para o roteiro **5.3** (documentação por endpoint) e **5.4** (regras mínimas).

**Base URL:** `http://localhost:8080`  
**Autenticação:** header `Authorization: Bearer {token}` (exceto rotas públicas).

---

## Padrão de erro (todas as falhas)

```json
{
  "timestamp": "2026-06-27T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descrição do erro.",
  "path": "/pedidos/1"
}
```

| Status | Quando |
|---|---|
| 400 | Validação de campos ou regra de negócio |
| 401 | Token ausente ou inválido |
| 403 | Autenticado, mas perfil sem permissão |
| 404 | Recurso não encontrado |
| 503 | Gateway de pagamento indisponível (circuit breaker) |
| 500 | Erro interno |

> **Nota:** conflitos (ex.: e-mail duplicado) retornam **400**, não 409. Validação de bean retorna **400**, não 422.

---

## `/auth` — Autenticação

### POST `/auth/registro`
1. **Propósito:** Cadastrar cliente com consentimento LGPD.  
2. **Método + rota:** `POST /auth/registro`  
3. **Auth:** Pública  
4. **Parâmetros:** nenhum  
5. **Body:**
```json
{
  "nome": "Cliente Teste",
  "email": "cliente@teste.com",
  "senha": "123456",
  "telefone": "81999999999",
  "consentimentoLgpd": true
}
```
6. **Response 201:**
```json
{
  "id": 2,
  "nome": "Cliente Teste",
  "email": "cliente@teste.com",
  "role": "CLIENTE",
  "ativo": true,
  "consentimentoLgpd": true
}
```
7. **Status:** 201, 400  
8. **Erro:** JSON padrão acima.

---

### POST `/auth/login`
1. **Propósito:** Autenticar usuário e obter JWT.  
2. **Método + rota:** `POST /auth/login`  
3. **Auth:** Pública  
4. **Parâmetros:** nenhum  
5. **Body:**
```json
{
  "email": "cliente@teste.com",
  "senha": "123456"
}
```
6. **Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "email": "cliente@teste.com",
  "role": "CLIENTE"
}
```
7. **Status:** 200, 401  
8. **Erro:** JSON padrão.

---

## `/usuarios` — Usuários internos

### POST `/usuarios`
1. **Propósito:** Criar usuário interno (gerente, atendente, etc.).  
2. **POST /usuarios**  
3. **Auth:** JWT — **ADMIN**  
4. **Parâmetros:** nenhum  
5. **Body:**
```json
{
  "nome": "Gerente Teste",
  "email": "gerente@raizes.com",
  "senha": "123456",
  "role": "GERENTE"
}
```
6. **Response 201:** objeto usuário (sem senha).  
7. **Status:** 201, 400, 401, 403  
8. **Erro:** JSON padrão.

### GET `/usuarios`
1. **Propósito:** Listar usuários.  
2. **GET /usuarios**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**  
4. **Query:** nenhum (*paginação planejada: `page`, `limit`*)  
5. **Body:** —  
6. **Response 200:** array de usuários.  
7. **Status:** 200, 401, 403  

### GET `/usuarios/{id}`
1. **Propósito:** Buscar usuário por ID.  
2. **GET /usuarios/{id}`**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**  
4. **Path:** `id` (Long)  
5. **Body:** —  
6. **Response 200:** objeto usuário.  
7. **Status:** 200, 401, 403, 404  

---

## `/unidades`

### GET `/unidades`
1. **Propósito:** Listar unidades da rede.  
2. **GET /unidades**  
3. **Auth:** Pública  
4. **Parâmetros:** nenhum  
5. **Body:** —  
6. **Response 200:** `[{ "id": 1, "nome": "Raízes Centro", "ativa": true, ... }]`  
7. **Status:** 200  

### GET `/unidades/{id}`
1. **Propósito:** Detalhar unidade.  
2. **GET /unidades/{id}`**  
3. **Auth:** Pública  
4. **Path:** `id`  
5. **Response 200:** objeto unidade.  
7. **Status:** 200, 404  

### POST `/unidades`
1. **Propósito:** Cadastrar unidade.  
2. **POST /unidades**  
3. **Auth:** JWT — **ADMIN**  
5. **Body:**
```json
{
  "nome": "Raízes Centro",
  "endereco": "Rua A, 100",
  "cidade": "Recife",
  "estado": "PE",
  "ativa": true
}
```
6. **Response 201:** unidade criada.  
7. **Status:** 201, 400, 401, 403  

### PUT `/unidades/{id}`
1. **Propósito:** Atualizar unidade.  
2. **PUT /unidades/{id}`**  
3. **Auth:** JWT — **ADMIN**  
4. **Path:** `id`  
5. **Body:** igual ao POST.  
6. **Response 200:** unidade atualizada.  
7. **Status:** 200, 400, 401, 403, 404  

---

## `/produtos`

### GET `/produtos`
1. **Propósito:** Consultar cardápio (opcionalmente com estoque por unidade).  
2. **GET /produtos**  
3. **Auth:** Pública  
4. **Query:** `unidadeId` (opcional) — inclui `quantidadeDisponivel`  
5. **Response 200:**
```json
[
  {
    "id": 1,
    "nome": "Baião de Dois",
    "preco": 25.90,
    "ativo": true,
    "quantidadeDisponivel": 50
  }
]
```
7. **Status:** 200  

### GET `/produtos/{id}`
1. **Propósito:** Detalhar produto.  
2. **GET /produtos/{id}`**  
3. **Auth:** Pública  
4. **Path:** `id` — **Query:** `unidadeId` (opcional)  
7. **Status:** 200, 404  

### POST `/produtos`
1. **Propósito:** Cadastrar produto no cardápio.  
2. **POST /produtos**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**  
5. **Body:**
```json
{
  "nome": "Baião de Dois",
  "descricao": "Prato típico",
  "preco": 25.90,
  "ativo": true
}
```
7. **Status:** 201, 400, 401, 403  

### PUT `/produtos/{id}`
1. **Propósito:** Atualizar produto.  
2. **PUT /produtos/{id}`**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**  
7. **Status:** 200, 400, 401, 403, 404  

---

## `/estoque`

### GET `/estoque`
1. **Propósito:** Consultar estoque de uma unidade.  
2. **GET /estoque**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**, **ATENDENTE**  
4. **Query:** `unidadeId` (obrigatório)  
6. **Response 200:** lista com produto e quantidade.  
7. **Status:** 200, 401, 403  

### POST `/estoque/entrada`
1. **Propósito:** Registrar entrada de estoque.  
2. **POST /estoque/entrada**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**  
5. **Body:**
```json
{
  "unidadeId": 1,
  "produtoId": 1,
  "quantidade": 50
}
```
7. **Status:** 200, 400, 401, 403  

### POST `/estoque/saida`
1. **Propósito:** Registrar saída manual de estoque.  
2. **POST /estoque/saida**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**  
5. **Body:** igual entrada.  
7. **Status:** 200, 400, 401, 403  

---

## `/pedidos`

### POST `/pedidos`
1. **Propósito:** Cliente cria pedido multicanal.  
2. **POST /pedidos**  
3. **Auth:** JWT — **CLIENTE**  
5. **Body:**
```json
{
  "unidadeId": 1,
  "canalPedido": "WEB",
  "itens": [{ "produtoId": 1, "quantidade": 2 }],
  "pontosResgatados": 0
}
```
6. **Response 201:** pedido com status `AGUARDANDO_PAGAMENTO`.  
7. **Status:** 201, 400, 401, 403  

### GET `/pedidos`
1. **Propósito:** Listar pedidos (cliente vê só os seus).  
2. **GET /pedidos**  
3. **Auth:** JWT — autenticado  
4. **Query:** `canalPedido`, `status`, `unidadeId` (todos opcionais)  
7. **Status:** 200, 401  

### GET `/pedidos/{id}`
1. **Propósito:** Consultar pedido por ID.  
2. **GET /pedidos/{id}`**  
3. **Auth:** JWT — cliente (próprio pedido) ou equipe  
4. **Path:** `id`  
7. **Status:** 200, 400, 401, 404  

### PATCH `/pedidos/{id}/status`
1. **Propósito:** Avançar status operacional.  
2. **PATCH /pedidos/{id}/status**  
3. **Auth:** JWT — **ADMIN**, **GERENTE**, **ATENDENTE**, **COZINHA** (conforme transição)  
5. **Body:** `{ "status": "EM_PREPARO" }`  
7. **Status:** 200, 400, 401, 403, 404  

### POST `/pedidos/{id}/cancelar`
1. **Propósito:** Cancelar pedido (regras por perfil).  
2. **POST /pedidos/{id}/cancelar**  
3. **Auth:** JWT — **CLIENTE** (dono), **ADMIN**, **GERENTE**, **ATENDENTE**  
4. **Path:** `id` — **Body:** nenhum  
7. **Status:** 200, 400, 401, 403, 404  

---

## `/pagamentos`

### POST `/pagamentos/solicitar/{pedidoId}`
1. **Propósito:** Enviar pedido ao gateway mock de pagamento.  
2. **POST /pagamentos/solicitar/{pedidoId}`**  
3. **Auth:** JWT — autenticado  
4. **Path:** `pedidoId`  
6. **Response 200:**
```json
{
  "id": 1,
  "pedidoId": 1,
  "transactionId": "uuid-gerado",
  "status": "PENDENTE"
}
```
7. **Status:** 200, 400, 401, 503  

### POST `/pagamentos/callback`
1. **Propósito:** Simular retorno do gateway (aprovação/recusa).  
2. **POST /pagamentos/callback**  
3. **Auth:** Pública  
5. **Body:**
```json
{
  "transactionId": "uuid-da-solicitacao",
  "status": "APROVADO"
}
```
7. **Status:** 200, 400  

---

## `/fidelidade`

### GET `/fidelidade/saldo`
1. **Propósito:** Consultar saldo de pontos do cliente.  
2. **GET /fidelidade/saldo**  
3. **Auth:** JWT — **CLIENTE** (com consentimento LGPD)  
6. **Response 200:** `{ "saldoPontos": 100 }`  
7. **Status:** 200, 400, 401, 403  

### GET `/fidelidade/historico`
1. **Propósito:** Histórico de movimentos de fidelidade.  
2. **GET /fidelidade/historico**  
3. **Auth:** JWT — **CLIENTE**  
7. **Status:** 200, 401, 403  

### POST `/fidelidade/resgate`
1. **Propósito:** Resgatar pontos (mínimo 50).  
2. **POST /fidelidade/resgate**  
3. **Auth:** JWT — **CLIENTE**  
5. **Body:** `{ "pontos": 50 }`  
7. **Status:** 200, 400, 401, 403  

---

## `/auditoria`

### GET `/auditoria`
1. **Propósito:** Listar trilha de ações sensíveis.  
2. **GET /auditoria**  
3. **Auth:** JWT — **ADMIN**  
6. **Response 200:** array com `usuarioEmail`, `acao`, `detalhes`, `dataHora`.  
7. **Status:** 200, 401, 403  

---

## `/actuator` — Monitoramento (RNF)

### GET `/actuator/health`
1. **Propósito:** Verificar disponibilidade da API e dependências.  
2. **GET /actuator/health**  
3. **Auth:** Pública  
6. **Response 200:** `{ "status": "UP" }`  
7. **Status:** 200, 503  

---

## Regras 5.4 — situação do projeto

| Regra | Situação |
|---|---|
| URLs plurais | ✅ Atendido |
| ID no path | ✅ Atendido |
| Paginação `?page=&limit=` | ⏳ Planejado (listagens retornam lista completa no MVP) |
| Status codes coerentes | ✅ Atendido (400 em vez de 409/422 — ver nota acima) |
| Erro JSON único | ✅ Atendido |

**Swagger interativo:** `/swagger-ui.html`  
**OpenAPI:** `/v3/api-docs`
