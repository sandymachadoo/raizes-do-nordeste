# Plano de Testes — Raízes do Nordeste API

Documento de apoio à validação do MVP (disciplina / TCC). Base URL padrão: `http://localhost:8080`.

---

## 1. Objetivo

Validar requisitos funcionais (RF), regras de negócio, segurança por perfil, multicanalidade, pagamento mock, fidelidade e auditoria.

---

## 2. Ambiente

| Item | Valor |
|---|---|
| Java | 21 |
| Banco | MySQL 8 (`raizes_db`) |
| Ferramentas | Postman (coleção `raizes-postman-collection.json`), Swagger |
| Autenticação | JWT Bearer no header `Authorization` |

---

## 3. Pré-condições

1. MySQL ativo com credenciais de `application.properties`.
2. API em execução (`mvnw spring-boot:run`).
3. Usuário ADMIN criado (registro manual ou insert) para cadastro de unidades/produtos.

---

## 4. Casos de teste

### CT-01 — Registro de cliente (RF01 / LGPD)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `POST /auth/registro` com `consentimentoLgpd: true` | HTTP 201, usuário CLIENTE |
| 2 | Registro sem consentimento | HTTP 400 |

### CT-02 — Login e JWT (RF02)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `POST /auth/login` credenciais válidas | HTTP 200, campo `token` |
| 2 | Login senha inválida | HTTP 401 |

### CT-03 — Unidade e cardápio (RF05–RF07)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `POST /unidades` (ADMIN) | HTTP 201 |
| 2 | `POST /produtos` | HTTP 201 |
| 3 | `GET /produtos?unidadeId=1` | Lista produtos da unidade |

### CT-04 — Estoque (RF08)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `POST /estoque/entrada` quantidade 10 | Saldo 10 |
| 2 | `POST /estoque/saida` quantidade 3 | Saldo 7 |
| 3 | Saída maior que saldo | HTTP 400 |

### CT-05 — Pedido multicanal (RF09 / RF10)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `POST /pedidos` com `canalPedido: WEB` | HTTP 201, status `AGUARDANDO_PAGAMENTO` |
| 2 | Pedido sem `canalPedido` | HTTP 400 |
| 3 | `GET /pedidos?canalPedido=WEB` | Filtra por canal |

### CT-06 — Pagamento mock (RF11)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `POST /pagamentos/solicitar/{pedidoId}` | `transactionId` gerado, status PENDENTE |
| 2 | Callback `APROVADO` | Pedido `PAGO`, estoque baixado |
| 3 | Callback `NEGADO` | Pedido `CANCELADO`, pontos estornados |

### CT-07 — Cancelamento explícito (RF12)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | Cliente `POST /pedidos/{id}/cancelar` em AGUARDANDO | HTTP 200, status CANCELADO |
| 2 | Cliente cancelar pedido PAGO | HTTP 400 |
| 3 | Gerente cancelar pedido PAGO | HTTP 200, estoque restaurado |
| 4 | Cancelar pedido ENTREGUE | HTTP 400 |

### CT-08 — Fluxo de status (RF13)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | `PATCH /pedidos/{id}/status` → EM_PREPARO (ATENDENTE) | OK |
| 2 | EM_PREPARO → PRONTO (COZINHA) | OK |
| 3 | PRONTO → ENTREGUE (ATENDENTE) | OK |
| 4 | Transição inválida (PAGO → ENTREGUE) | HTTP 400 |

### CT-09 — Fidelidade (RF14)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | Após pagamento aprovado, `GET /fidelidade/saldo` | Saldo > 0 |
| 2 | `POST /fidelidade/resgate` com < 50 pts | HTTP 400 |
| 3 | Resgate sem consentimento LGPD | HTTP 400 |

### CT-10 — Segurança por perfil (RNF)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | CLIENTE acessa `GET /auditoria` | HTTP 403 |
| 2 | Endpoint sem token | HTTP 401 |
| 3 | CLIENTE vê apenas próprios pedidos | Lista filtrada |

### CT-11 — Auditoria (RF24)

| Passo | Ação | Resultado esperado |
|---|---|---|
| 1 | Executar login, pedido, pagamento | Registros em `GET /auditoria` (ADMIN) |

---

## 5. Critérios de aceite do MVP

- [ ] Fluxo ponta a ponta: registro → pedido → pagamento → status → entrega
- [ ] Filtro `?canalPedido=` funcional
- [ ] Cancelamento com regras de perfil e estorno de estoque/pontos
- [ ] Erros padronizados (`ErrorResponse`)
- [ ] Swagger documentado e autenticável

---

## 6. Testes automatizados

```bash
mvnw.cmd test
```

Teste de contexto Spring Boot (`RaizesDoNordesteApplicationTests`) valida subida da aplicação com MySQL configurado.

---

## 7. RNF — Desempenho (referência)

Virtual threads habilitadas (`spring.threads.virtual.enabled=true`). Teste de carga manual opcional com ferramenta externa (k6, JMeter) em cenário de listagem de pedidos e criação concorrente.
