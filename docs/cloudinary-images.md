# Imagens de perfil no Cloudinary

O Core recebe o arquivo, envia para o Cloudinary e salva somente a URL HTTPS em
`profile.profile_image_url`. HOUSEHOLD, STORE e COMPANY usam o mesmo fluxo.
Nao ha nova coluna nem migracao de banco para esta funcionalidade.

## Configuracao

Acrescente estas variaveis ao `.env` na pasta de onde o Core sera iniciado:

```dotenv
CLOUDINARY_CLOUD_NAME=seu_cloud_name
CLOUDINARY_API_KEY=sua_api_key
CLOUDINARY_API_SECRET=seu_api_secret
```

Obtenha os valores nas configuracoes de API do ambiente da sua conta Cloudinary.
Em producao, configure as mesmas variaveis nos segredos/variaveis do servico.
As credenciais ficam somente no backend. Nao e necessario upload preset: o SDK
realiza upload autenticado no servidor.

Sem essas credenciais, o Core continua iniciando e uploads retornam HTTP 503.
Remover o vinculo da foto continua funcionando sem Cloudinary configurado.

## Endpoints

Todas as rotas exigem `Authorization: Bearer <token>`.

| Metodo | Rota | Acao |
| --- | --- | --- |
| PUT | `/api/profile/image` | Envia/substitui a imagem do perfil do token |
| DELETE | `/api/profile/image` | Limpa a URL do perfil do token |
| PUT | `/api/companies/me/stores/{id}/image` | Envia/substitui a imagem de uma loja da empresa |
| DELETE | `/api/companies/me/stores/{id}/image` | Limpa a URL de uma loja da empresa |

As rotas de loja recebem o ID da loja, nao o ID do perfil. O Core valida que o
perfil autenticado e COMPANY, que a empresa esta ativa e que a loja pertence a ela.
O perfil que recebe a imagem tambem deve estar ativo.

PUT recebe `multipart/form-data` com um unico arquivo no campo `file`.
JPEG, PNG e WebP sao aceitos, com limite de 5 MiB e 16 megapixels. A validacao
le e decodifica o conteudo, independentemente da extensao ou Content-Type declarado.
O limite total da requisicao multipart e 6 MiB.

Resposta de PUT (HTTP 200):

```json
{"profileId":12,"profileImageUrl":"https://res.cloudinary.com/.../image.png"}
```

Resposta de DELETE (HTTP 200):

```json
{"profileId":12,"profileImageUrl":null}
```

`GET /api/profile` continua retornando `profileImageUrl`. Os retornos de loja
continuam retornando `storeImageUrl`, obtido do perfil associado.

O campo `profileImageUrl` foi removido do contrato de `PATCH /api/profile`.
Esse PATCH atualiza os outros dados; a imagem so e alterada pelas rotas acima.
Crie primeiro o perfil/loja no fluxo existente e depois envie a foto.

## Exemplo no front

```javascript
const form = new FormData();
form.append("file", arquivoSelecionado);
const response = await fetch(`${coreUrl}/api/profile/image`, {
  method: "PUT",
  headers: { Authorization: `Bearer ${token}` },
  body: form,
});
if (!response.ok) throw new Error("Nao foi possivel atualizar a foto");
const { profileImageUrl } = await response.json();
// Atualize o estado da tela com profileImageUrl.
```

Deixe o navegador definir Content-Type e o boundary do multipart.
Para remover, envie DELETE na mesma URL e atualize a tela com a resposta.

## Substituicao, remocao e falhas

Cada upload recebe um identificador novo no Cloudinary, sem sobrescrever fotos.
Esse identificador nao e persistido no banco. Trocar a foto altera apenas a URL;
remover a foto limpa apenas a URL. As imagens continuam armazenadas no Cloudinary.
Nao ha exclusao remota, fila de limpeza nem tentativa de exclusao ao apagar uma loja.

O upload acontece antes da atualizacao do banco, fora da transacao de banco.
Se o Cloudinary falhar, a URL anterior permanece. Se o upload funcionar e o banco
falhar, a URL anterior permanece no banco e a imagem enviada fica no Cloudinary.
Uma nova validacao de permissao ocorre antes de persistir a URL.

Erros principais: 400 arquivo vazio/invalido, 401 sem autenticacao, 403 perfil sem
permissao/inativo, 404 perfil ou loja nao encontrada/acessivel, 413 limite de upload,
502 falha no Cloudinary e 503 credenciais nao configuradas.

## Verificacao

```powershell
./mvnw.cmd test checkstyle:check
```

Os testes de imagem usam armazenamento simulado e imagens locais; nao enviam
arquivos para uma conta real. Para validar com sua conta, configure o `.env` e use
as rotas PUT/DELETE com um token valido.

Referencias: [SDK Java](https://cloudinary.com/documentation/java_integration),
[upload Java](https://cloudinary.com/documentation/java_image_and_video_upload).
