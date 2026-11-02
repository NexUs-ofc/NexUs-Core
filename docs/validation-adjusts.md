# Ajustes de validação e evidências do Core

## Escopo

Esta alteração cobre os itens 1, 2, 3 e 6 da análise de requisitos, a parte do Core dos itens 4 e 5 e a execução dos testes de integração do item 11.

| Item | Implementação |
| --- | --- |
| 1 | Nomes opcionais no PATCH de perfil e loja rejeitam texto vazio ou composto apenas por espaços, inclusive espaços Unicode. Omitir o nome continua permitido. |
| 2 | Duplicidades verificadas pelo serviço e violações de integridade retornam 409. Falhas de acesso a dados continuam retornando 500 com mensagem segura. |
| 3 | JSON inválido, conversão de parâmetros, parâmetros ausentes e validações retornam ApiError. Métodos e formatos não suportados retornam 405 e 415; 405 preserva o cabeçalho Allow. |
| 4 | springdoc atualizado para 3.1.1, compatível com Spring Boot 4.1. |
| 5 | Operações e parâmetros descritos no OpenAPI, com modelos, exemplos e respostas HTTP. O catálogo público, as rotas internas e as administrativas mantêm seus esquemas de segurança próprios. |
| 6 | A consulta de faltas chama fn_get_items_below_minimum com o perfil autenticado. O resultado preserva os campos foodId, foodName, foodImageUrl e minimumQuantity. |
| 11 | Testes de persistência MongoDB e testes de functions e procedures PostgreSQL incluídos na execução de integração e no CI. |

## Decisões

O PATCH usa uma restrição própria para distinguir ausência de valor de texto em branco. Isso mantém a atualização parcial sem exigir campos que o cliente não pretende alterar.

Conflitos de unicidade e relacionamento usam 409, incluindo disputas entre requisições concorrentes que ultrapassem as verificações anteriores ao salvamento. Mensagens internas do banco não são enviadas ao cliente.

A consulta nativa de faltas reutiliza a regra SQL e usa uma projeção que inclui a imagem do alimento em uma única consulta. Lotes vencidos deixam de compor o saldo disponível. A ordenação considera a quantidade faltante, o nome e o identificador do alimento.

A definição da função precisa estar aplicada no banco. A alteração não recria nem modifica rotinas do banco em produção.

As descrições comuns e as respostas de erro são centralizadas na configuração OpenAPI. As descrições das operações ficam nas anotações dos respectivos controllers. O modelo ApiError é registrado explicitamente para que as referências do contrato sejam resolvidas.

## Política de sessão mantida

Por decisão de escopo, os itens 7 e 8 permanecem como política aceita: o Core valida localmente o JWT até a expiração. O logout no Auth e uma mudança posterior de status não invalidam imediatamente o token no Core.

O requisito de autenticação e autorização com Spring Security continua implementado. A aplicação verifica assinatura, emissor e validade do JWT e preserva as regras existentes de perfil e propriedade dos recursos. A revogação imediata não foi declarada como funcionalidade.

## Como verificar

Use Java 17 ou superior. A execução de integração recebe as conexões de dois bancos de teste:

```text
./mvnw verify checkstyle:check -Dmongodb.test.uri=mongodb://localhost:27017 -Dpostgresql.test.url=jdbc:postgresql://localhost:5432/nexus_test
```

O usuário e a senha padrão do PostgreSQL de teste são nexus_test. Podem ser substituídos pelas propriedades postgresql.test.username e postgresql.test.password.

Os testes MongoDB criam bancos com identificadores aleatórios e os removem no encerramento. Os testes PostgreSQL criam e removem um schema próprio, sem depender do schema public.

A suíte PostgreSQL verifica:

- Exclusão dos lotes vencidos do cálculo de faltas, inclusão de alimentos sem saldo e isolamento por perfil.
- Consulta de vencimentos pela function através do repositório JPA.
- Geração mensal idempotente e exclusão de empresas inativas.
- Atualização de inadimplência e reativação após quitação, pelas chamadas reais de BillingJobs.

As definições em src/test/resources/postgres-routines.sql são uma cópia de teste das quatro rotinas da [migration V8 do NexUs-DB](https://github.com/NexUs-ofc/NexUs-DB/blob/ea73091988b6ba6d78cc0e12456872a34bd4bfd2/migrations/V8__profile_restrictions_store_hours_billing_and_indexes.sql#L81-L269). O schema de suporte é reduzido aos dados necessários; esses testes não substituem uma validação de todas as migrations.

Os testes de contrato acessam /v3/api-docs e verificam descrições, parâmetros, respostas e referências de schemas. Os testes de validação fazem requisições aos controllers reais e verificam rejeição de nomes em branco, erros de entrada e conflitos.

### Resultado local em 02/11/2026

A execução de verify e checkstyle:check terminou com sucesso: 354 testes, zero falhas, zero erros e zero ignorados. Foram usados JDK 26.0.1 com alvo Java 17, MongoDB 8.2 e PostgreSQL 18.6 temporários. O CI mantém JDK 17.

Os testes MongoDB incluem os casos antes ignorados e suas invocações parametrizadas. As quatro verificações PostgreSQL foram executadas com chamadas reais às rotinas. Não foram usados os bancos da organização.

## Fora desta alteração

- Compatibilidade e documentação próprias do Auth.
- Revisão da documentação própria do Notify.
- As três falhas de inicialização do Firebase e o teste sensível ao tempo de Retry-After, ambos no Notify.
- Publicação da branch, merge e atualização do ambiente executável.
