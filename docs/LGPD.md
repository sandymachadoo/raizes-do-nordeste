# LGPD — Tratamento de Dados Pessoais (Back-end)

Documento de conformidade mínima para o MVP **Raízes do Nordeste**, demonstrando como o back-end trata dados pessoais conforme a Lei nº 13.709/2018 (LGPD).

---

## 1. Escopo

Este documento descreve:

- Quais **dados pessoais** são coletados
- **Finalidade** e **base legal** de cada tratamento
- Como o **consentimento** é registrado
- **Controles técnicos** implementados na API
- **Retenção** e estratégia de **anonimização**

Aplica-se aos endpoints de autenticação, usuários, pedidos, fidelidade e auditoria.

---

## 2. Dados pessoais coletados

| Dado | Onde é coletado | Armazenamento | Sensibilidade |
|---|---|---|---|
| Nome | `POST /auth/registro`, `POST /usuarios` | Tabela `usuarios.nome` | Pessoal |
| E-mail | Registro, login, auditoria | `usuarios.email` (único) | Pessoal / identificador |
| Telefone | Registro (opcional) | `usuarios.telefone` | Pessoal |
| Senha | Registro / criação de usuário | `usuarios.senha_hash` (BCrypt) | Dado sensível — **nunca** em texto puro |
| Consentimento LGPD | `POST /auth/registro` | `usuarios.consentimento_lgpd`, `data_consentimento_lgpd` | Metadado de compliance |
| Perfil (role) | Sistema / ADMIN | `usuarios.role` | Controle de acesso, não exposto indevidamente |
| Histórico de pedidos | `POST /pedidos` | `pedidos` + `itens_pedido` (vinculado ao cliente) | Pessoal (comportamento/consumo) |
| Pontos de fidelidade | Pagamentos e resgates | `fidelidade`, `movimento_fidelidade` | Pessoal (programa de benefícios) |
| Trilha de auditoria | Ações na API | `auditorias.usuario_email`, ação, detalhes, data | Pessoal (e-mail do operador) |

**Dados que não são expostos nas respostas da API:**

- `senha_hash` — permanece apenas no banco
- Entidades JPA completas — a API usa DTOs na borda

---

## 3. Finalidade e base legal

| Tratamento | Finalidade | Base legal (LGPD) |
|---|---|---|
| Cadastro de cliente | Criar conta, identificar usuário e permitir pedidos | **Consentimento** (art. 7º, I) + **execução de contrato** (art. 7º, V) |
| Login / JWT | Autenticar e autorizar acesso à API | **Execução de contrato** (art. 7º, V) |
| Pedidos | Processar compra, pagamento mock e entrega | **Execução de contrato** (art. 7º, V) |
| Programa de fidelidade | Acumular e resgatar pontos | **Consentimento** (art. 7º, I) — exige `consentimentoLgpd` |
| Usuários internos (ADMIN, GERENTE…) | Operação da franquia | **Legítimo interesse** / relação de trabalho (art. 7º, IX / contrato) |
| Auditoria | Segurança, rastreabilidade e accountability | **Legítimo interesse** (art. 7º, IX) + **obrigação legal/regulatória** quando aplicável |
| Logs de login e ações sensíveis | Detectar acesso indevido e incidentes | **Legítimo interesse** (art. 7º, IX) |

---

## 4. Registro de consentimento

### 4.1 Cadastro de cliente

Endpoint: `POST /auth/registro`

**Requisitos:**

- Campo `consentimentoLgpd` deve ser `true` (`@AssertTrue` no DTO)
- Se `false` ou ausente → **HTTP 400** com mensagem padronizada

**Persistência:**

```json
{
  "consentimentoLgpd": true,
  "dataConsentimentoLgpd": "2026-06-27T10:30:00"
}
```

Campos na entidade `Usuario`:

- `consentimentoLgpd` (Boolean)
- `dataConsentimentoLgpd` (LocalDateTime)

**Auditoria:** ação `REGISTRO_CLIENTE` registrada em `auditorias`.

### 4.2 Uso do programa de fidelidade

Endpoints: `GET /fidelidade/saldo`, `/historico`, `POST /fidelidade/resgate`, resgate em `POST /pedidos`.

O `FidelidadeService` valida `consentimentoLgpd == true`. Caso contrário:

- **HTTP 400** — *"Consentimento LGPD necessário para usar o programa de fidelidade."*

### 4.3 Usuários internos

Criados via `POST /usuarios` (ADMIN). Por padrão `consentimentoLgpd = false`; fidelidade não se aplica a perfis internos.

---

## 5. Controles técnicos mínimos

### 5.1 Hashing de senha

- Algoritmo: **BCrypt** (`BCryptPasswordEncoder`)
- Campo: `senha_hash` — senha em texto nunca é persistida
- Configuração: `SecurityConfig`

### 5.2 Autenticação por token

- **JWT** stateless após `POST /auth/login`
- Header: `Authorization: Bearer {token}`
- Expiração configurável: `jwt.expiration-ms` (padrão 24h)
- Implementação: `JwtService`, `JwtAuthenticationFilter`

### 5.3 Autorização por perfil

Perfis: `ADMIN`, `GERENTE`, `ATENDENTE`, `COZINHA`, `CLIENTE`.

Controle via `@PreAuthorize` nos controllers. Exemplos:

| Recurso | Quem acessa |
|---|---|
| `POST /pedidos` | CLIENTE |
| `GET /auditoria` | ADMIN |
| `POST /unidades` | ADMIN |
| `GET /fidelidade/*` | CLIENTE (com consentimento) |

Acesso negado → **HTTP 403** — *"Acesso negado."*

### 5.4 Logs / auditoria de ações sensíveis

Entidade `Auditoria` persiste:

- `usuarioEmail`
- `acao` (ex.: LOGIN, CRIAR_PEDIDO, RESGATE_FIDELIDADE)
- `detalhes`
- `dataHora`

Consulta restrita: `GET /auditoria` (ADMIN).

**Ações auditadas (exemplos):**

| Ação | Momento |
|---|---|
| REGISTRO_CLIENTE | Cadastro com LGPD |
| LOGIN | Autenticação |
| CRIAR_PEDIDO | Novo pedido |
| SOLICITAR_PAGAMENTO | Início fluxo pagamento |
| RESGATE_FIDELIDADE | Uso de pontos |
| CRIAR_USUARIO | Admin cria perfil interno |

### 5.5 Minimização e contratos na API

- Respostas usam **DTOs** — sem vazamento de `senhaHash`
- Cliente acessa **apenas seus pedidos** (filtro por usuário autenticado)
- Erros padronizados (`ErrorResponse`) — sem stack trace ao cliente

---

## 6. Retenção e anonimização

### 6.1 Política de retenção (MVP)

| Dado | Retenção | Justificativa |
|---|---|---|
| Conta de cliente ativa | Enquanto a conta existir | Execução do serviço |
| Pedidos | Vinculados ao cliente; mantidos para histórico operacional | Contrato + obrigações fiscais simuladas |
| Auditoria | **12 meses** (recomendado em produção) | Segurança e investigação de incidentes |
| Token JWT | Até expiração (sessão stateless) | Não persistido no banco |

> No MVP acadêmico, a exclusão automática por job não está implementada; a política está **documentada** para evolução.

### 6.2 Estratégia de anonimização (quando aplicável)

**Cenário:** solicitação de exclusão do titular (art. 18, LGPD).

**Procedimento proposto (evolução):**

1. Desativar conta (`ativo = false`)
2. Anonimizar campos identificáveis:
   - `nome` → `"Usuário Anonimizado"`
   - `email` → `anonimo_{uuid}@removido.local`
   - `telefone` → `null`
3. Manter pedidos com **referência anonimizada** para integridade operacional/fiscal
4. Registrar ação `ANONIMIZAR_TITULAR` na auditoria

**No MVP atual:** procedimento **documentado**; endpoint dedicado (`DELETE /usuarios/me` ou similar) é evolução futura.

### 6.3 Incidentes e segurança

- Senhas: irreversíveis (hash)
- Comunicação: HTTPS recomendado em produção
- Acesso a auditoria: somente ADMIN

---

## 7. Direitos do titular (referência)

| Direito (art. 18) | Como atender no MVP |
|---|---|
| Confirmação / acesso | Cliente autenticado consulta pedidos e fidelidade |
| Correção | Evolução: `PUT /usuarios/me` |
| Eliminação | Evolução: fluxo de anonimização (seção 6.2) |
| Portabilidade | Evolução: export JSON sob demanda |
| Revogação de consentimento | Evolução: flag + bloqueio de fidelidade; pedidos já executados mantêm base contratual |

---

## 8. Evidências no código

| Controle | Arquivo / componente |
|---|---|
| Consentimento no registro | `RegistroClienteRequestDTO`, `AuthService` |
| Campos LGPD na entidade | `Usuario.java` |
| Validação fidelidade | `FidelidadeService.validarConsentimentoLgpd()` |
| BCrypt | `SecurityConfig` |
| JWT | `JwtService`, `JwtAuthenticationFilter` |
| Perfis | `Role`, `@PreAuthorize` nos controllers |
| Auditoria | `AuditoriaService`, `AuditoriaController` |

---

## 9. Testes recomendados (Postman)

| Teste | Resultado esperado |
|---|---|
| Registro sem `consentimentoLgpd: true` | 400 |
| Registro com consentimento | 201 + `dataConsentimentoLgpd` preenchido |
| Fidelidade sem consentimento | 400 |
| `GET /auditoria` como CLIENTE | 403 |
| Login com senha errada | 401 (sem revelar se e-mail existe além da mensagem genérica) |

Ver também: `PLANO_DE_TESTES.md` — CT-01, CT-09, CT-10, CT-11.

---

## 10. Referências

- Lei nº 13.709/2018 (LGPD)
- Documentação da API: `docs/API_ENDPOINTS.md`
- Swagger: `/swagger-ui.html`
