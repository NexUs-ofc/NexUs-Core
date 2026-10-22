# Análise de consumo, descarte e saldo do estoque NexUs

Data: 18/10/2026. Análise estática das versões main consultadas no GitHub. Este documento avalia alternativas; não aplica migrations nem modifica serviços.

## 1. Conclusão

Dividir pantry_item funciona para exibir quantidades por status, mas transforma uma tabela de lotes disponíveis em uma mistura de saldo e histórico. No ecossistema atual isso exige corrigir filtros, a identificação dos lotes, métricas por linha, datas, chamadas da IA e integração com rankings.

Para totais acumulados sem histórico por período, dois contadores adicionais são mais simples. Para consumo/descarte por mês, correções, auditoria e rankings confiáveis, recomendo preservar pantry_item.id e registrar movimentos com quantidade e data. O problema da quantidade zero desaparece quando quantity é entendido como saldo: o movimento registra quanto foi consumido.

A V9 local criada nesta conversa continua sendo um rascunho de status ACTIVE/DEPLETED mais pantry_item_movement. Não apliquei essa migration; o estado do banco em execução não foi consultado. O arquivo não implementa sozinho a consistência entre saldo, movimentos, consultas e métricas. O modelo posterior de divisão de registros foi explicado, mas não implementado.

## 2. O que existe no código

| Componente | Situação confirmada | Implicação |
|---|---|---|
| NexUs-AI | Lê e escreve PostgreSQL diretamente; soma entradas com mesmo alimento e validade; exclui itens para dar baixa. | Alterar apenas Core não corrige o fluxo do chat. |
| NexUs-Core | Cria lotes; permite substituir quantidade, alterar validade e excluir; incrementa Redis ao registrar pelo Core. | Ações explícitas de consumo/descarte ainda precisam ser criadas. |
| NexUs-Notify | Busca candidatos no Core por HTTP; usa itemId para deduplicar alertas. | O filtro de disponibilidade deve ser aplicado no Core/SQL, preservando identidade. |
| Ceris-Mobile | Estoque e detalhe ainda sem fonte de dados; já há chat com IA e integração de listas com Core. | Não há uma chamada atual de estoque para apenas ajustar; será uma integração nova. O chat já é afetado pelas mudanças da IA. |
| NexUs-DB | pantry_item tem quantity > 0; functions de validade/mínimo e views de BI. | Arquivar saldo zero exige alterar a constraint e as consultas que contam linhas. |
| NexUs-BI-AI | Consulta relatórios mensais no MongoDB; PostgreSQL é usado para validar empresa. | O principal impacto é no produtor dos relatórios, e na definição dos indicadores apresentados. |

Fontes: [tools de estoque da IA](https://github.com/NexUs-ofc/NexUs-AI/blob/ade88d53780cac495d1d0079d55d0d18e4ad9c8b/app/tools/pg_tools.py#L136), [StockService](https://github.com/NexUs-ofc/NexUs-Core/blob/330f0be40485496b172d4c4dfd1eddb2a549dfdd/src/main/java/com/example/nexuscore/service/StockService.java#L91), [CoreClient do Notify](https://github.com/NexUs-ofc/NexUs-Notify/blob/f7a59c54f79b0d7b2a0a401880fbcd79994bb95f/src/main/java/com/example/nexusnotify/infra/core/CoreClient.java#L30), [EstoqueViewModel](https://github.com/NexUs-ofc/Ceris-Mobile/blob/1c600d64ea37d84e2c5054aa4900bf9a32750c93/app/src/main/java/com/example/ceris/viewmodel/EstoqueViewModel.kt#L5), [relatórios do BI-AI](https://github.com/NexUs-ofc/NexUs-BI-AI/blob/04c4385c910c903bf7470ba78570a20b8f20c5de/app/repository/report_repository.py#L41).

## 3. Inventário de consultas e operações impactadas

### NexUs-AI

Arquivo principal: [app/repository/pgsql/stock.py](https://github.com/NexUs-ofc/NexUs-AI/blob/ade88d53780cac495d1d0079d55d0d18e4ad9c8b/app/repository/pgsql/stock.py#L22).

| Método | Alteração necessária com divisão por status |
|---|---|
| find_equivalente, linha 46 | Buscar somente ACTIVE. Sem isso, nova compra pode ser somada a uma linha CONSUMED ou DISCARDED. Também definir se mesma validade significa mesmo lote ou somente agrupamento visual. |
| get_stock, linha 106 | Buscar somente ACTIVE; receitas e planejamento devem usar esse conjunto disponível. |
| get_expired_products, linha 131 | Filtrar ACTIVE. A consulta atual inclui vencidos e itens com validade até hoje + 7 dias; manter a semântica declarada da ferramenta. |
| get_missing_products, linha 160 | Somar apenas estoque disponível. Corrigir também o INNER JOIN atual: alimentos configurados sem nenhuma linha de estoque não aparecem. Vencidos hoje são contados nessa consulta, diferentemente da function V8. |
| get_category_info, linha 205 | Somar somente ACTIVE quando o relatório representar estoque atual. |
| get_brand_info, linha 246 | Mesmo filtro para o total por marca. |
| find_by_id, linha 73 | Para uma ação de saída, validar dono, disponibilidade e saldo; consultar histórico é uma operação diferente. |
| save / remove_product | Substituir exclusão usada como baixa por uma operação transacional com tipo e quantidade. O commit hoje acontece dentro dos métodos do repositório. |

São seis consultas de seleção/agregação com alteração direta de disponibilidade, além da consulta por ID e dos caminhos de escrita.

Em [pg_tools.py](https://github.com/NexUs-ofc/NexUs-AI/blob/ade88d53780cac495d1d0079d55d0d18e4ad9c8b/app/tools/pg_tools.py#L136), add_product soma quantidade à linha equivalente e remove_product recebe somente o ID. Não há parâmetro de quantidade nem motivo na remoção. Portanto, uma frase como “consumi 2” não pode ser implementada corretamente apenas acrescentando uma coluna SQL.

A [skill de atualização guiada](https://github.com/NexUs-ofc/NexUs-AI/blob/ade88d53780cac495d1d0079d55d0d18e4ad9c8b/skills/estoque/estoque-atualizacao-guiada/SKILL.md#L83) usa remove_product no exemplo “consumi os dois”. A skill de registro, as descrições das tools e o catálogo em app/core/agents.py devem passar a distinguir consumir, descartar, adicionar e corrigir.

Recomendação: concentrar as escritas no Core e fazer a IA chamar o mesmo contrato do mobile. Outra opção é uma function transacional compartilhada no banco, mas os efeitos em Redis/Mongo ainda precisam de um responsável único. Duplicar a regra em Java e Python mantém o risco de divergência.

A leitura de nota fiscal em [receipt.py](https://github.com/NexUs-ofc/NexUs-AI/blob/ade88d53780cac495d1d0079d55d0d18e4ad9c8b/app/controller/receipt.py#L27) apenas devolve a extração nessa versão; não encontrei gravação de estoque nesse caminho. Uma futura confirmação dos itens extraídos deve usar a mesma operação central de entrada.

### NexUs-Core

| Local | Impacto |
|---|---|
| PantryItemRepository.findByProfileIdOrderByExpiryDateAscIdDesc | Filtrar estoque disponível. |
| findByProfileIdAndExpiryDateBefore | Excluir histórico consumido/descartado ou saldo encerrado. |
| findByExpiryDateBefore | Ajustar a declaração se usada; não identifiquei chamada no fluxo consultado. |
| findExpiredByStatus | O status atual da consulta é o do PROFILE. Acrescentar disponibilidade do ITEM sem remover a regra de perfil ativo. |
| findExpiringProducts | Usa fn_get_expiring_products; corrigir a function e preservar contrato/paginação. |
| PantryProductSettingRepository.findMissingByProfileId | Somar somente estoque disponível; alinhar a regra de validade com a function do BD. |
| StockService.add / registerFood | Distinguir entrada real de qualquer linha criada para histórico; histórico não pode disparar recordRegistered. |
| StockService.update | Substituir quantidade não informa se houve consumo, descarte, compra ou correção. Exigir ação explícita para essas situações. |
| StockService.remove | Exclusão apaga o lote e não representa consumo/descarte. Reservar para remoção de cadastro incorreto, com política para itens que já possuem histórico. |
| ExpiredProductRankingJob | Somente lotes ainda presentes; manter identidade estável para deduplicação. |

Fontes: [PantryItemRepository](https://github.com/NexUs-ofc/NexUs-Core/blob/330f0be40485496b172d4c4dfd1eddb2a549dfdd/src/main/java/com/example/nexuscore/repository/PantryItemRepository.java#L15), [consulta de mínimo](https://github.com/NexUs-ofc/NexUs-Core/blob/330f0be40485496b172d4c4dfd1eddb2a549dfdd/src/main/java/com/example/nexuscore/repository/PantryProductSettingRepository.java#L16), [StockService](https://github.com/NexUs-ofc/NexUs-Core/blob/330f0be40485496b172d4c4dfd1eddb2a549dfdd/src/main/java/com/example/nexuscore/service/StockService.java#L131), [job de vencidos](https://github.com/NexUs-ofc/NexUs-Core/blob/330f0be40485496b172d4c4dfd1eddb2a549dfdd/src/main/java/com/example/nexuscore/service/ExpiredProductRankingJob.java#L27).

Uma nova ação precisa conferir: perfil autenticado, item pertencente ao perfil, quantidade inteira positiva, saldo suficiente, item disponível e repetição da mesma operação. Saldo e histórico devem ser gravados juntos. Bloqueio de linha ou atualização condicional impede duas requisições concorrentes de consumirem o mesmo saldo.

### NexUs-Notify

O caminho confirmado é:

Notify CoreClient.products → GET /internal/notifications/products → Core PantryItemRepository.findExpiringProducts → BD fn_get_expiring_products.

Não há consulta direta de pantry_item no Notify. Para eliminar itens encerrados dos próximos ciclos, o filtro pertence à function/Core. O DTO atual ProductCandidate contém profileId, itemId, foodName e expiryDate, sem quantidade; uma baixa parcial não exige alterar o DTO, pois a mensagem atual não promete uma quantidade específica.

A [ProductExpiryRule](https://github.com/NexUs-ofc/NexUs-Notify/blob/f7a59c54f79b0d7b2a0a401880fbcd79994bb95f/src/main/java/com/example/nexusnotify/service/rule/ProductExpiryRule.java#L21) usa itemId como source_id e uma chave baseada na validade e distância em dias. [NotificationWriter](https://github.com/NexUs-ofc/NexUs-Notify/blob/f7a59c54f79b0d7b2a0a401880fbcd79994bb95f/src/main/java/com/example/nexusnotify/service/NotificationWriter.java#L16) deduplica por perfil, tipo, source_id e alert_key.

Consequências:
- Preservar o ID da linha ativa evita gerar um novo alerta para o mesmo lote só porque parte foi consumida.
- Linhas históricas criadas por divisão nunca podem voltar como candidatas.
- Trocar também o ID do saldo restante pode gerar alertas repetidos.
- A geração busca candidatos e depois salva/envia: consumir tudo entre essas etapas pode produzir um alerta desatualizado. Definir se consistência eventual basta ou se é necessária revalidação.
- Filtrar os próximos ciclos não remove notificações já salvas no MongoDB; elas podem permanecer como histórico. Push já entregue não é desfeito por uma migration.
- Reabrir o mesmo lote com a mesma validade pode continuar deduplicado para o mesmo aviso. A regra de reabertura precisa ser explícita.
- As notificações de eventos e lojas não consultam o estoque nos caminhos examinados; não têm impacto direto dessa mudança.

### Ceris-Mobile

A [EstoqueViewModel](https://github.com/NexUs-ofc/Ceris-Mobile/blob/1c600d64ea37d84e2c5054aa4900bf9a32750c93/app/src/main/java/com/example/ceris/viewmodel/EstoqueViewModel.kt#L5) e a DetalheProdutoViewModel declaram que ainda não têm fonte de dados. Não há StockAPI/StockRepository no inventário da main consultada. O detalhe configura apenas a apresentação.

As chamadas relacionadas que já existem:
- [ChatAPI](https://github.com/NexUs-ofc/Ceris-Mobile/blob/1c600d64ea37d84e2c5054aa4900bf9a32750c93/app/src/main/java/com/example/ceris/api/ChatAPI.kt#L24): POST chat/ para a IA. O contrato de conversa pode permanecer, enquanto as tools passam a executar baixas corretas.
- NotifyAPI: listagem de notificações e marcação como lida; o contrato não precisa mudar somente por adicionar o status de estoque.
- ShoppingListAPI: integração com listas no Core; não implementa baixa de estoque. Marcar uma compra ou gerar uma receita não deve consumir estoque implicitamente.

O [modelo Produto](https://github.com/NexUs-ofc/Ceris-Mobile/blob/1c600d64ea37d84e2c5054aa4900bf9a32750c93/app/src/main/java/com/example/ceris/model/Produto.kt#L9) possui SituacaoProduto.FRESCO/VENCENDO/VENCIDO. Isso é condição de validade, diferente de consumo/descarte. Um item pode estar ACTIVE e VENCIDO ao mesmo tempo.

Para a tela de estoque: criar API/repositório e DTOs, adicionar ações de consumo/descarte com quantidade, tratar saldo insuficiente/conflito/repetição e recarregar a lista após alterações pelo chat ou botões. A divisão de linhas ou gravação de movimentos deve ficar no backend. Ocultar um card no aparelho não altera o estoque no servidor.

### NexUs-DB e dados analíticos

[V1, pantry_item](https://github.com/NexUs-ofc/NexUs-DB/blob/830acd60edebd560e579e8b93c71d7c9386e79e5/migrations/V1__initial_schema.sql#L62) exige quantity > 0 e não impõe unicidade por alimento/validade. Divisão por status cabe estruturalmente, mas modifica o significado de cada linha.

[V8, funções de estoque](https://github.com/NexUs-ofc/NexUs-DB/blob/830acd60edebd560e579e8b93c71d7c9386e79e5/migrations/V8__profile_restrictions_store_hours_billing_and_indexes.sql#L81):
- fn_get_expiring_products: hoje filtra perfil ACTIVE e janela de validade, sem estado do item.
- fn_get_items_below_minimum: soma estoque com validade hoje/futura ou ausente e inclui alimentos configurados sem saldo. Acrescentar a condição de disponibilidade do item.

[datamart_estrela.sql](https://github.com/NexUs-ofc/NexUs-DB/blob/830acd60edebd560e579e8b93c71d7c9386e79e5/script/03_bi_views/datamart_estrela.sql#L80):
- bi.fato_estoque: lê todas as linhas de pantry_item; precisa representar somente saldo presente.
- bi.fato_estoque_minimo: soma todas as quantidades; precisa da regra de disponibilidade/validade acordada.
- bi.fato_movimento: lê bi_hist.registro_estoque, não pantry_item.
- D3/D4/D5/D7/D8: dependem de fato_estoque e da semântica de uma linha.
- D6: depende do estoque mínimo.
- D1/D9: dependem do histórico e precisam de uma definição explícita sobre eventos, lotes ou unidades.
- vw_volume_tabelas conta linhas físicas para volume técnico. Uma linha extra aumenta esse volume, mas isso não significa nova compra.

O histórico bi_hist.registro_estoque já possui consumed_quantity, discarded_quantity, event_date, purchase_date e estimated_loss. No código consultado, é criado pelo script do datamart/dataload e preenchido pela simulação de [bi.py](https://github.com/NexUs-ofc/NexUs-DB/blob/830acd60edebd560e579e8b93c71d7c9386e79e5/script/dataload/src/bi.py#L404). Não encontrei uma integração de produção que transforme as atuais baixas do Core/IA nesse histórico.

Acrescentar status a pantry_item não alimenta automaticamente bi.fato_movimento. Tampouco uma nova tabela de movimentos passa a alimentá-lo sem adaptar a view ou a carga. Deve haver uma fonte oficial de movimentos, evitando gravar duas cópias independentes e depois somá-las.

O seed padrão e a migração legada podem continuar inserindo com default ACTIVE. O dataload BI precisa preservar a distinção entre dados simulados e reais. Reusar bi_hist como tabela operacional exige promovê-la a contrato Flyway, adicionar identidade/idempotência/vínculo ao lote e permitir descartes sem vencimento.

### NexUs-BI-AI

[report_repository.py](https://github.com/NexUs-ofc/NexUs-BI-AI/blob/04c4385c910c903bf7470ba78570a20b8f20c5de/app/repository/report_repository.py#L41) consulta documentos MongoDB por mês/ano ou os mais recentes. [company_repository.py](https://github.com/NexUs-ofc/NexUs-BI-AI/blob/04c4385c910c903bf7470ba78570a20b8f20c5de/app/repository/company_repository.py#L1) usa PostgreSQL para verificar empresa ativa. Não há SQL de pantry_item nesse serviço.

Assim:
- Se o formato dos relatórios mensais for preservado, a consulta do BI-AI pode continuar igual.
- Se um KPI mudar de “eventos” para “unidades”, atualizar rótulos, descrição, versão da métrica e, se necessário, FAQ/prompts.
- Números incorretos na consolidação serão repetidos pelo assistente; o juiz não recalcula o banco operacional.
- Relatórios de meses já fechados não são corrigidos automaticamente por mudar a tabela.
- Não localizei nos seis repositórios examinados o produtor completo dos documentos mensais do MongoDB. Seu código/job e a configuração real de produção ainda precisam ser identificados para fechar esse trecho do impacto.

## 4. Por que dividir linhas pode alterar os números

Considere uma entrada de 5 embalagens. O usuário consome 2 e descarta 3.

Na divisão por status, sobram duas linhas históricas: CONSUMED 2 e DISCARDED 3.
- COUNT(*) = 2: número de registros.
- SUM(quantity) = 5: número de embalagens que passaram pelo lote.
- SUM(quantity) filtrado por ACTIVE = 0: saldo disponível.
- SUM(quantity) filtrado por DISCARDED = 3: descarte.
- Um cadastro/entrada original continua sendo um, mesmo que haja duas ou mais linhas físicas.

A [D1 atual](https://github.com/NexUs-ofc/NexUs-DB/blob/830acd60edebd560e579e8b93c71d7c9386e79e5/script/03_bi_views/datamart_estrela.sql#L133) usa contagens de eventos. Se receber um evento de consumo e um de descarte, resulta em 50%. Caso o KPI desejado seja percentual de unidades descartadas, seria 3 / (2 + 3) = 60%.

Fazer três descartes de uma unidade ou um descarte de três unidades deve produzir o mesmo indicador por quantidade. A fórmula por evento não tem essa propriedade. Isso também precisa ser revisto com uma tabela separada de movimentos: o desenho da tabela, sozinho, não define a métrica.

| Pergunta de negócio | Medida adequada |
|---|---|
| Quantos produtos diferentes tenho? | COUNT(DISTINCT food_id) do saldo disponível. |
| Quantas embalagens tenho? | SUM(quantity) disponível. |
| Quantos lotes/entradas foram registrados? | Identidade estável da entrada/lote; não COUNT das linhas divididas. |
| Quantas ações de consumo ocorreram? | COUNT de movimentos de consumo. |
| Quantas embalagens foram consumidas? | SUM da quantidade de movimentos de consumo. |
| Quanto foi descartado no mês? | SUM do descarte pela data da ação. |
| Quanto venceu e ainda está em casa? | Saldo disponível com validade passada. |
| Quanto foi perdido em R$? | Quantidade descartada e preço/base de custo confiável; ausência de preço não significa perda zero. |

Validade e descarte precisam ser conceitos diferentes. Um produto pode vencer e continuar guardado; outro pode ser descartado antes de vencer. O histórico atual aceita item_expired e item_consumed, portanto um descarte por dano/sobra precisa de representação própria ou de um motivo adicional.

quantity é inteiro no estoque e o catálogo tem package_quantity/unidade. Consumir uma embalagem de cinco é suportável sem mudar o tipo numérico. Consumir 200 ml de uma embalagem de 1 l é outro requisito: exige saldo na unidade de conteúdo e conversões. Somar embalagens de 200 g e 5 kg não produz uma massa comparável sem normalização.

## 5. Redis: impactos confirmados e condições de duplicação

Fonte: [ProductRankingService](https://github.com/NexUs-ofc/NexUs-Core/blob/330f0be40485496b172d4c4dfd1eddb2a549dfdd/src/main/java/com/example/nexuscore/service/ProductRankingService.java#L29).

Hoje:
- Cada registro pelo Core chama recordRegistered.
- A pontuação incrementa sempre em 1, independentemente de quantity.
- A deduplicação usa ranking:event:{tipo}:{pantry_item_id}.
- Os tipos atuais são REGISTERED e EXPIRED; não há ranking de consumo/descarte.
- O job varre vencidos e recordExpired aplica uma vez por ID.
- Chaves diárias têm retenção de 90 dias; os marcadores de deduplicação criados pelo Lua não recebem TTL nesse código.
- A data do ranking diário é a data da execução, não necessariamente a data real do vencimento.

Inserir uma linha no PostgreSQL, por si só, não executa o Java nem incrementa Redis. Os problemas aparecem se:
1. A divisão reaproveitar add/registerFood: uma linha histórica conta como novo cadastro.
2. O job examinar as linhas históricas já consumidas/descartadas: elas contam como vencidas posteriormente.
3. O saldo restante ganhar um ID novo: um mesmo lote físico pode receber nova contagem.
4. Um futuro ranking de consumo reutilizar a deduplicação por itemId: o primeiro consumo parcial pode bloquear os seguintes.
5. Alterar a pontuação para quantidade sem redefinir a chave do evento: mudanças/repetições do mesmo lote não serão contabilizadas como esperado.

Com movimentos, usar ID da operação/movimento para deduplicar eventos repetíveis; para o marco “este lote venceu”, uma identidade estável do lote pode continuar adequada. Definir se cada ranking mede frequência de entradas, lotes, unidades, pessoas ou saldo atual.

Também há uma divergência anterior à mudança: a IA grava diretamente no banco e não chama recordRegistered do Core. Ela ainda soma compras com mesma validade, enquanto o Core cria novos itens. Portanto, o ranking de cadastros atual não representa necessariamente todas as entradas feitas pelo chat.

As chamadas a Redis e ao registro Mongo acontecem dentro de métodos Java anotados com transação de banco. A transação PostgreSQL não desfaz um incremento já realizado em Redis. Uma falha posterior pode deixar um evento sem o lote correspondente. Recomenda-se publicar após commit e garantir reprocessamento/reconciliação; uma outbox na mesma transação é a opção mais robusta.

Não apagar nem recalcular rankings existentes sem definir a métrica e a estratégia de migração. Uma nova versão de chave evita misturar contagens antigas de lotes com novas contagens de unidades.

## 6. Comparação das alternativas

| Modelo | Simplicidade inicial | Histórico por mês | Efeito na identidade | Custo no ecossistema |
|---|---|---|---|---|
| Status único em todo o lote | Alta | Insuficiente para ações parciais | Preserva ID | Não resolve consumo/descarte mistos. |
| Dividir linhas por status | Alta no SQL básico | Possível com data por ação | Precisa de vínculo de origem | Filtros em todos os leitores e métricas que tratam linha como lote. |
| Saldo + consumed_quantity + discarded_quantity | Alta para totais | Não preserva quando ocorreram as várias ações | Preserva ID | Boa opção de MVP se só os totais acumulados forem necessários. |
| Saldo + movimentos com tipo/quantidade/data | Média | Sim | Preserva ID do lote e identifica cada ação | Maior mudança inicial, regras mais claras para o BI e integrações. |
| Reaproveitar bi_hist como fonte operacional | Média | Sim, após adaptação | Vínculo ao lote precisa ser acrescentado | Reduz duplicação conceitual, mas hoje a estrutura é criada por scripts analíticos/seed, não um contrato operacional completo. |

A divisão não é inerentemente incorreta: com status, data de ação, identidade do lote de origem, transações e métricas corrigidas, funciona. Porém, esses acréscimos tornam a tabela uma mistura de lotes e eventos. A vantagem de “só um campo” deixa de representar o custo real.

## 7. Desenho recomendado para o sistema atual

1. pantry_item mantém o lote e a quantidade restante. Se houver status armazenado, usar ACTIVE/DEPLETED; consumido e descartado qualificam a ação, pois o mesmo lote pode ter ambos.
2. Movimento registra pantry_item_id, tipo, quantidade positiva e data. Usar uma chave estável da operação para impedir a mesma ação de ser executada novamente em retries.
3. “Consumi 2” reduz saldo 5 → 3 e grava CONSUMED 2. “Descartei 3” reduz 3 → 0 e grava DISCARDED 3. O histórico responde 2 consumidas e 3 descartadas; quantity = 0 responde que não há saldo.
4. Core é responsável pelas mutações; mobile e IA enviam ação e quantidade ao mesmo contrato.
5. A transação grava saldo e movimento. Publicação para ranking/atividade tem garantia de reprocessamento. Não manter duas fontes independentes de histórico.
6. BI lê a fonte oficial de movimentos, mantendo datas e o significado das métricas. Relatórios mensais têm versão/competência e regra para dados tardios.

Schema mínimo dessa alternativa:
- pantry_item.quantity: alterar a constraint para permitir zero.
- pantry_item.status: opcional, 1 coluna nova se o estado não for derivado do saldo.
- Nova tabela de movimentos: id, pantry_item_id, movement_type, quantity, occurred_at; mais request_id recomendado para retries confiáveis.
- Motivo do descarte e dados de correção podem ser acrescentados conforme o escopo. Entradas somadas a um lote precisam de eventos de entrada se o requisito for reconstruir compras/cadastros por período.

Para divisão por status com rastreabilidade mínima:
- status, data da ação e vínculo à identidade de origem são três responsabilidades adicionais.
- quantity mantém a regra positiva.
- A criação do histórico deve copiar a data de entrada original e não chamar o fluxo de novo cadastro.
- Se só status for adicionado, o modelo atende a exibição dos totais, mas deixa data/origem e métricas sem solução completa.

## 8. Cenários que a implementação deve tratar

| Situação | Regra / impacto |
|---|---|
| Consumir 1 de 5 | Saldo 4; histórico registra consumo 1; estoque e receitas usam 4. |
| Consumir as 5 | Saldo 0; histórico registra 5, não zero. |
| Consumir 2 e descartar 3 | Guardar os dois destinos e suas quantidades. Não escolher um único motivo final para o lote. |
| Consumir várias vezes | Cada operação distinta é aceita; repetição da mesma requisição não é uma nova ação. |
| Consumir mais que o saldo | Rejeitar sem alterar saldo/histórico. |
| Quantidade zero, negativa ou fracionária | Rejeitar no contrato atual de embalagens inteiras. |
| Mobile e chat simultâneos | Serializar/condicionar alteração do saldo. Ambos devem usar a mesma regra. |
| Reenvio após timeout | Mesmo request_id e mesmo conteúdo devolvem o mesmo resultado; conteúdo diferente com a mesma chave deve gerar conflito. |
| Cadastro de alimento com mesma validade | Definir política de lote/entrada; nunca somar numa linha histórica. |
| Vários lotes e validades | Escolher lote explicitamente ou aplicar regra documentada de prioridade por vencimento. |
| Falta de validade | Continua possível consumir/descartar; não inferir vencimento. |
| Vence hoje | Não confundir a janela “vence hoje” com o critério “já venceu antes de hoje”. Alinhar datas/fuso. |
| Vencido ainda guardado | Continua presente fisicamente; pode ser mostrado como vencido e excluído do estoque utilizável para receitas/mínimo. |
| Descarte antes do vencimento | Não classificar automaticamente como item_expired. |
| Dano, doação, perda ou transferência | Definir tipos/motivos somente quando existirem como funcionalidade; não chamar toda saída de consumo. |
| Correção manual de contagem | Registrar como correção; não inferir consumo/descarte de uma diferença de quantidade. |
| Desfazer uma ação | Preferir operação compensatória rastreável; revisar projeções e relatórios se já publicados. |
| Alterar validade após evento de vencimento | Redis atual não desfaz automaticamente a contagem anterior; definir correção/reconciliação. |
| Item de outro perfil | Rejeitar sem alteração; usar perfil autenticado, nunca o indicado livremente pela IA/cliente. |
| Notificação entre busca e consumo | Definir consistência eventual/revalidação; preservar ID ativo reduz duplicação. |
| Histórico já notificado | Manter como histórico ou marcar encerramento; mudança SQL não cancela push entregue. |
| Exclusão física do lote | Não perder movimentos por cascata; distinguir correção de cadastro de baixa real. |
| Falha após atualizar saldo | A transação deve desfazer também o movimento; Redis/Mongo requerem publicação confiável independente. |
| Consumo em meses diferentes | Dois contadores totais e uma “última data” não permitem repartir corretamente os meses. |
| Ação informada com atraso | Distinguir quando ocorreu e quando foi registrada, se necessário; definir reabertura ou revisão de competência. |
| Mudança de cidade/endereço | Definir se BI/ranking preservam a localização da época ou refletem a atual. |
| Mudança de tamanho da embalagem | Preservar base de quantidade/custo dos movimentos históricos se o catálogo for mutável. |
| Falta de preço | Não inventar perda financeira; marcar dado desconhecido. |
| Dados antigos apagados | Não reconstruir consumo/descarte por adivinhação. Definir início de cobertura do histórico. |
| Dados simulados | Separar origem simulada e real para não misturar indicadores operacionais. |
| Crescimento do histórico | Indexar lote/data e consultas mensais; testar volume antes de adicionar índices indiscriminadamente. |
| Lotes mudando durante varreduras paginadas | Revisar paginação por offset; um item pode sair do conjunto entre páginas. Keyset ou snapshot torna a varredura mais previsível. |

## 9. Ordem de implementação e validação

1. Definir o que cada ranking/KPI mede e escolher o modelo; não ativar a V9 como está apenas por conter um enum.
2. Preparar schema compatível e dados existentes, sem inventar histórico que já foi apagado.
3. Atualizar leitores de estoque no Core, IA, functions e views para a regra de disponibilidade.
4. Implementar uma operação de saída transacional e idempotente com autorização por perfil.
5. Fazer IA e mobile utilizarem esse mesmo contrato; atualizar skills e remover exclusão como sinônimo de consumo.
6. Integrar histórico à fato_movimento e ao processo que produz os documentos mensais do MongoDB.
7. Ajustar e versionar ranking se sua unidade mudar; verificar consistência com entradas feitas pelo chat.
8. Só então habilitar ações para usuários. Schema aditivo pode ser instalado antes, mas leitores antigos não podem conviver com histórico visível sem filtros.
9. Validar consumo parcial/total/misto, concorrência, retries, reversão, notificações e BI por quantidade/período. A mesma quantidade descartada em uma ou três ações deve produzir o mesmo KPI por unidades.
10. Validar saldos/contagens antes e depois com dados conhecidos e verificar o plano das consultas se o histórico crescer.

Complexidade relativa: baixa para schema simples; média para a regra de baixa centralizada e mobile; média/alta para alinhar IA, histórico de BI e confiabilidade Redis. A dificuldade principal é a semântica e a integração, não criar a coluna status. Sem executar os serviços e identificar o produtor mensal dos relatórios, não há base para prometer um prazo exato.

## 10. Referências das versões consultadas

- NexUs-AI: main em [ade88d53780c](https://github.com/NexUs-ofc/NexUs-AI/commit/ade88d53780cac495d1d0079d55d0d18e4ad9c8b).
- Ceris-Mobile: main em [1c600d64ea37](https://github.com/NexUs-ofc/Ceris-Mobile/commit/1c600d64ea37d84e2c5054aa4900bf9a32750c93).
- NexUs-BI-AI: main em [04c4385c910c](https://github.com/NexUs-ofc/NexUs-BI-AI/commit/04c4385c910c903bf7470ba78570a20b8f20c5de).
- NexUs-DB: main em [830acd60edeb](https://github.com/NexUs-ofc/NexUs-DB/commit/830acd60edebd560e579e8b93c71d7c9386e79e5).
- NexUs-Notify: main em [f7a59c54f79b](https://github.com/NexUs-ofc/NexUs-Notify/commit/f7a59c54f79b0d7b2a0a401880fbcd79994bb95f).
- NexUs-Core: main em [330f0be40485](https://github.com/NexUs-ofc/NexUs-Core/commit/330f0be40485496b172d4c4dfd1eddb2a549dfdd).

Limite da análise: código remoto da main e arquivos locais já discutidos. Não foi feita consulta a bancos reais, reconstrução de Redis, chamada a serviços em produção ou aplicação de migration. Configuração implantada e código em branches não examinadas podem divergir desses snapshots.

