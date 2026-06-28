# RNFs — Desempenho, Disponibilidade e Tolerância a Falhas

Documentação das implementações de Requisitos Não Funcionais no MVP **Raízes do Nordeste**.

---

## 1. Disponibilidade

### Spring Boot Actuator

| Endpoint | Uso |
|---|---|
| `GET /actuator/health` | Status geral (UP/DOWN) + componentes |
| `GET /actuator/health/liveness` | Probe de vida (Kubernetes/Docker) |
| `GET /actuator/health/readiness` | Probe de prontidão (inclui MySQL) |
| `GET /actuator/info` | Metadados da aplicação |

Endpoints públicos (sem JWT) para monitoramento e orquestradores.

### Health do banco

O health check inclui validação do **DataSource MySQL**. Se o banco cair, `readiness` indica indisponibilidade.

---

## 2. Desempenho em horários de pico

### Virtual threads

```properties
spring.threads.virtual.enabled=true
```

Permite maior concorrência em operações de I/O (JPA, HTTP) com menor consumo de threads da JVM.

### Teste de carga (k6)

Script: `scripts/k6-load-test.js`

Pré-requisito: [k6](https://k6.io/docs/get-started/installation/) instalado e API rodando.

```bash
k6 run scripts/k6-load-test.js
```

Com URL customizada:

```bash
k6 run -e BASE_URL=http://localhost:8080 scripts/k6-load-test.js
```

Cenário padrão: **20 usuários virtuais por 30 segundos** em endpoints públicos (`/actuator/health`, `/unidades`, `/produtos`).

Critérios configurados:
- Taxa de falha < 5%
- p95 de latência < 800 ms

---

## 3. Tolerância a falhas — integração de pagamento (mock)

### Desacoplamento

`PagamentoGatewayMockClient` simula chamada ao provedor externo antes de persistir a solicitação.

Fluxo:
1. Cliente chama `POST /pagamentos/solicitar/{pedidoId}`
2. API contacta gateway mock (com latência simulada)
3. Em sucesso, persiste `Pagamento` com `transactionId`
4. Callback externo em `POST /pagamentos/callback` (idempotente)

### Retry (Resilience4j)

- Até **3 tentativas** com intervalo de **300 ms**
- Retenta apenas falhas transitorias (`PagamentoGatewayIndisponivelException`)

### Circuit Breaker

- Janela deslizante de 10 chamadas
- Abre com **50% de falhas**
- Permanece aberto **15 segundos**
- Fallback retorna **503** com mensagem amigável

### Idempotência do callback

Callback com `transactionId` já processado retorna erro de negócio, evitando dupla baixa de estoque ou duplo acúmulo de pontos.

### Demonstração de retry (opcional)

Em `application.properties`:

```properties
pagamento.gateway.simular-falha-transitoria=true
pagamento.gateway.falhas-transitorias-antes-sucesso=2
```

As 2 primeiras tentativas falham; a 3ª (retry) conclui com sucesso.

---

## 4. Segurança e auditoria (referência)

| RNF | Implementação |
|---|---|
| LGPD | Consentimento no registro e fidelidade — ver `docs/LGPD.md` |
| Controle de acesso | JWT + `@PreAuthorize` |
| Senha hash | BCrypt |
| Auditoria | `AuditoriaService` + `GET /auditoria` |
| Documentação | Swagger `/swagger-ui.html` |

---

## 5. Limitações conhecidas

- Alta disponibilidade multi-instância não configurada (MVP single-node).
- Circuit breaker e retry aplicados na **solicitação** ao gateway, não no callback recebido.
- Teste k6 cobre endpoints públicos; cenários autenticados exigem script ampliado.
