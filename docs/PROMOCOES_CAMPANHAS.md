# Promoções e Campanhas — Proposta Conceitual (RF25)

Documento de evolução futura para o MVP **Raízes do Nordeste**. Não implementado no código atual; descreve regras de negócio e modelo de dados para uma próxima iteração.

---

## Objetivo

Permitir campanhas promocionais por unidade ou rede, com desconto automático ou cupom, respeitando estoque, canal de origem e vigência.

---

## Entidades propostas

| Entidade | Campos principais |
|---|---|
| `Campanha` | id, nome, descricao, dataInicio, dataFim, ativa |
| `RegraCampanha` | campanhaId, tipo (PERCENTUAL, VALOR_FIXO, LEVE_PAGUE), valor, produtoId (opcional), unidadeId (opcional), canalPedido (opcional) |
| `Cupom` | codigo, campanhaId, usoMaximo, usosAtuais, clienteId (opcional) |

---

## Regras de aplicação

1. **Vigência:** campanha só aplica se `dataInicio <= agora <= dataFim` e `ativa = true`.
2. **Escopo:** regra pode restringir por `unidadeId`, `produtoId` e/ou `canalPedido`.
3. **Prioridade:** uma campanha por item; em conflito, prevalece maior desconto para o cliente.
4. **Acúmulo:** promoções não acumulam com resgate de fidelidade (aplicar o maior benefício).
5. **Estoque:** desconto não altera baixa de estoque; quantidades permanecem as do pedido.
6. **Auditoria:** registrar `APLICAR_CAMPANHA` com id da campanha e valor descontado.

---

## Fluxo no pedido (futuro)

```
POST /pedidos
  → validar itens e estoque
  → calcular valor bruto
  → aplicar campanhas elegíveis (CampanhaService)
  → aplicar resgate fidelidade (se houver)
  → persistir valorTotal e metadados da campanha
```

Endpoint adicional sugerido:

```
POST /campanhas/validar-cupom   { "codigo": "NORDESTE10", "unidadeId": 1 }
```

---

## Exemplo de regra

**Campanha:** "Semana do Baião"  
**Regra:** 10% de desconto em produtos da categoria "Pratos" na unidade 1, canal `APP` ou `WEB`, de 01/06 a 07/06.

---

## Integração com multicanalidade

Cada `CanalPedido` (`APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB`) pode ter campanhas exclusivas, alinhado ao requisito de origem multicanal do pedido.

---

## Fora do escopo do MVP

- Motor de recomendação
- Push notification de campanha
- Integração com gateway de pagamento para split de desconto
