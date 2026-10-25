O endpoint POST /api/stock/{itemId}/movements registra uma saída real do estoque do perfil autenticado.

```json
{
  "type": "CONSUMED",
  "quantity": 2
}
```

Os únicos tipos aceitos são CONSUMED (consumo) e DISCARDED (descarte, mesmo sem vencimento). A quantidade representa embalagens inteiras, deve ser positiva e não pode ultrapassar o saldo do lote.

A resposta 201 contém id, pantryItemId, type, quantity, remainingQuantity e eventDate. O servidor define a data do movimento. Item inexistente retorna 404, item de outro perfil retorna 403 e quantidade inválida ou superior ao saldo retorna 400.

O saldo e o movimento em bi_hist.registro_estoque são gravados na mesma transação PostgreSQL. O item é bloqueado para escrita durante movimentações, correções e remoções, evitando que operações concorrentes consumam o mesmo saldo.

Quando o saldo chega a zero, pantry_item é preservado para manter a referência dos movimentos. Itens com saldo zero não aparecem no estoque disponível, nos vencidos, nas notificações de validade ou na varredura de vencimentos. Eles continuam contribuindo com zero para os cálculos de estoque mínimo.

PATCH /api/stock/{id} continua sendo uma correção do saldo, agora aceitando zero e sem gerar consumo ou descarte. DELETE /api/stock/{id} passa a zerar o saldo sem excluir o lote e sem presumir consumo ou descarte.

A estrutura do banco deve ser atualizada antes de publicar esta versão do Core. O contrato é versionado no repositório NexUs-DB. As views dependentes recebem somente os ajustes de compatibilidade; as fórmulas dos indicadores de BI serão revisadas depois.
