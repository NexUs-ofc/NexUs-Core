# Swagger / OpenAPI

Com o Core em execução, acesse `/swagger-ui.html`. A especificação JSON fica em `/v3/api-docs`.

Para testar as rotas `/api/**`, faça login no Auth e copie o JWT de acesso retornado.
No Swagger do Core, clique em **Authorize**, cole somente o token em **BearerAuth**, sem o prefixo
`Bearer`, e confirme. O Swagger enviará `Authorization: Bearer <token>` nas chamadas.
O token deve estar válido e ter o perfil exigido pela rota, como no Notify.

As demais credenciais disponíveis em **Authorize** correspondem às proteções já existentes:

| Rotas | Campo no Swagger | Header enviado | Credencial |
| --- | --- | --- | --- |
| `/api/**` | `BearerAuth` | `Authorization: Bearer <token>` | JWT de acesso do Auth |
| `/internal/notifications/**` | `NotifyTokenAuth` | `X-Notify-Token` | `NOTIFY_CORE_TOKEN` |
| `/admin/**` | `AdminKeyAuth` | `X-Admin-Key` | `ADMIN_API_KEY` |

## Configuração do `.env`

Nenhuma variável nova é necessária para habilitar o botão **Authorize**.

- `JWT_SECRET_BASE64` deve ter o mesmo valor configurado no Auth.
- `JWT_ISSUER` deve corresponder ao emissor usado pelo Auth (padrão: `nexus-auth`).
- `NOTIFY_CORE_TOKEN` é necessário para testar as rotas internas e deve ser compartilhado com o Notify.
- `ADMIN_API_KEY` é necessário para testar as rotas administrativas.

`JWT_SECRET_BASE64` é um segredo do servidor: nunca deve ser colado no Swagger.
O Core usa JWT nas rotas do aplicativo; não é necessário adicionar `API_KEY` ou `X-API-KEY`.
