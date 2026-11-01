# Listas de compras para notificações

O Core fornece candidatos; o Notify define antecedências, textos, deduplicação e envio.

## Consulta interna

`GET /internal/notifications/shopping-lists?from=2026-10-31&to=2026-11-07&page=0&size=100`

Exige `X-Notify-Token`, com o mesmo token configurado em `notify.integration.token`.
JWT de usuário não substitui esse token. Somente GET é permitido; a rota antiga de eventos continua bloqueada.

`from` e `to` são datas ISO obrigatórias, com limites inclusivos. O intervalo admite até 31 dias de diferença.
`page` começa em zero; `size` aceita 1 a 500, com padrão 100. Datas e paginação inválidas retornam 400.
Ausência de token ou token inválido retorna 401.

```json
{
  "content": [
    {
      "profileId": 12,
      "shoppingListId": "lista-id",
      "title": "Mercado",
      "date": "2026-11-07",
      "completed": false
    }
  ],
  "hasNext": false
}
```

## Seleção e conclusão

- Somente listas com data no intervalo e proprietário HOUSEHOLD ativo são retornadas.
- Listas sem data não geram candidatos. A data continua opcional e sua atualização mantém o comportamento atual:
  omitir `date` ou enviar null preserva uma data existente; não há remoção da data pela API.
- `completed` é true apenas quando existe pelo menos um item e todos os itens estão marcados (`checked=true`).
  Listas vazias, parcialmente marcadas ou com nenhum item marcado retornam false.
- Listas completas continuam elegíveis. O Notify deve apenas mudar a mensagem, por exemplo:
  “A lista Mercado está marcada para amanhã!” ou
  “Você concluiu a compra de todos os itens da lista Mercado? Ela está marcada para amanhã!”.
- A conclusão é calculada na consulta; não há novo estado persistido nem bloqueio automático da lista pela data.

A consulta é ordenada por data e ID. O índice `idx_shopping_list_notification_date` cobre `date` e `_id`;
o Core tem criação automática de índices habilitada. A projeção lê apenas os dados do candidato e a marcação dos itens.
Os proprietários são validados em lote no PostgreSQL após a página de listas ser consultada no MongoDB.
Por isso uma página pode ter `content` vazio e `hasNext=true`: o consumidor deve continuar enquanto `hasNext` for true.
A paginação não representa um snapshot; alterações concorrentes podem mover registros entre páginas.

## Destino ao tocar na notificação

`shoppingListId` é o ID da lista, que pode ser consultada por `GET /api/shopping-lists/{id}` com o JWT do proprietário.
Na integração do Notify, esse valor deve ser usado como `sourceId` de um tipo específico de notificação de lista.
O aplicativo deve reconhecer o tipo e abrir a página da lista usando esse ID. O Core não define rotas de tela do aplicativo.
Se a lista tiver sido excluída, a consulta retorna 404; o aplicativo deve tratar esse caso.

## Validação

Os testes de serviço cobrem conclusão, perfis elegíveis, páginas filtradas e validação de parâmetros.
Os testes HTTP cobrem contrato, defaults, autenticação e bloqueio de escrita e da antiga rota de eventos.
O teste `ShoppingListRepositoryMongoTest` cobre os limites inclusivos, ausência de data, projeção dos itens e paginação.
Ele exige uma instância MongoDB de testes explicitamente indicada por `-Dmongodb.test.uri=mongodb://localhost:27017`;
cria e remove somente uma base temporária própria com prefixo `nexus_shopping_list_test_`.
