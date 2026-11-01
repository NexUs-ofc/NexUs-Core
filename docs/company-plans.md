# Empresa e planos

`GET /api/profile` retorna `cnpj` quando o perfil autenticado é `COMPANY` e existe uma empresa vinculada. O CNPJ também é preservado na resposta de `PATCH /api/profile`. O campo é omitido para outros tipos de perfil ou quando não existe empresa vinculada.

`GET /api/companies/me` exige autenticação e retorna `id`, `cnpj`, `planId`, `planName`, `planPrice` e `storeCount` da empresa do perfil autenticado. Um parâmetro de consulta não permite selecionar outra empresa.

`GET /api/plans` é público para permitir a escolha durante o cadastro. Retorna uma lista com `id`, `planName`, `planPrice` e `description`, consultando somente planos com `is_active = true`, ordenados por preço e ID. Uma lista vazia significa que não há planos disponíveis. A descrição pode ser nula.

`PATCH /api/companies/me/plan` exige autenticação e recebe `{ "planId": <id do catálogo> }`. O Core recusa IDs inexistentes ou inativos e retorna a empresa com o plano confirmado quando a troca é concluída.

O cadastro no Auth deve usar o mesmo banco de planos do Core. O frontend deve enviar o ID retornado pelo catálogo, sem assumir nomes, preços ou uma sequência fixa de IDs.

# Publicação e diagnóstico na Discloud

O push de uma branch não substitui a publicação da aplicação. Gere um novo JAR desta versão e atualize o arquivo indicado por `MAIN` no `discloud.config` da aplicação existente. Preserve as variáveis de ambiente da produção. A Discloud orienta fazer o build local para aplicações Java e disponibilizar o serviço em `0.0.0.0:8080`.

Após publicar, confira os logs de inicialização e teste `/actuator/health`, o catálogo público e as consultas autenticadas. A documentação em `/v3/api-docs` deve incluir `/api/plans` e `/api/companies/me`.

Um erro 502 indica que o proxy não conseguiu obter uma resposta válida da aplicação. Verifique o estado da aplicação e os logs antes de reiniciar: processo encerrado, porta incorreta, erro de inicialização, falha de conexão com dependências e falta de memória exigem correções diferentes. O projeto usa `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MONGODB_URI`, `MONGODB_DATABASE`, `REDIS_URL` e `JWT_SECRET_BASE64`; confirme a configuração sem publicar os valores. Para o navegador local, configure `CORS_ALLOWED_ORIGINS` com `http://localhost:8080`.

Referências oficiais: [hospedagem de sites e APIs](https://docs.discloud.com/how-to-host/websites-and-apis), [discloud.config](https://docs.discloud.com/en/configurations) e [comandos para logs, status e reinicialização](https://discloud.github.io/cli/commands.html).
