# API

Prefixo `/api`. JSON em camelCase. Enums em kebab-case: `purpose` = `sale | rent | short-stay`; `type` = `apartment | house | commercial | land`; `status` = `draft | published | archived`. Datas `createdAt`/`updatedAt` em ISO-8601 UTC; `availableFrom` em `YYYY-MM-DD`.

A especificacao OpenAPI gerada pelo springdoc fica em `/v3/api-docs` e o Swagger UI em `/swagger-ui.html`, apenas no perfil `dev`.

## Autenticacao (`/api/auth`)

| Metodo | Caminho | Corpo | Resposta |
|---|---|---|---|
| POST | `/login` | `{ email, password }` | `200 { mfaRequired: true, enrollmentRequired }` + cookie `constantino_mfa`; `401` credencial invalida; `429` muitas tentativas |
| POST | `/mfa/setup` | (cookie `constantino_mfa`) | `200 { secret, otpauthUri }`; `409` ja configurado; `401` desafio expirado |
| POST | `/mfa/verify` | `{ code }` (6 digitos) | `200 { name, email, role }` + `Set-Cookie: CFID=<token opaco>` (8 h); `401` codigo invalido ou reuso; `409` sem setup |
| POST | `/alive` | (cookie `CFID`) | `204`; renova a janela de inatividade de 2 h |
| GET | `/me` | (cookie `CFID`) | `200 { name, email, role }` |
| POST | `/logout` | (cookie `CFID`) | `204`; revoga a sessao no servidor e expira os cookies |

## Imoveis publicos (`/api/properties`)

| Metodo | Caminho | Descricao |
|---|---|---|
| GET | `/` | Pagina de imoveis **publicados**. Query: `purpose`, `type`, `neighborhood`, `query` (busca em titulo, bairro, codigo e descricao, sem distincao de maiusculas), `page`, `size` (max 100), `sort` (padrao `updatedAt,desc`) |
| GET | `/neighborhoods` | Lista ordenada de bairros com imovel publicado |
| GET | `/{id}` | Imovel publicado; `404` se nao existir ou nao estiver publicado |

## Imoveis administrativos (`/api/admin/properties`, `ROLE_ADMIN`)

| Metodo | Caminho | Descricao |
|---|---|---|
| GET | `/` | Todos os imoveis, paginado |
| GET | `/{id}` | Qualquer status |
| POST | `/` | Cria; `201` com `Location`. Corpo: `PropertyRequest` |
| PUT | `/{id}` | Substitui todos os campos. Mudanca de `status` obedece as transicoes abaixo (`409` se invalida) |
| PATCH | `/{id}/status` | `{ status }` |
| DELETE | `/{id}` | `204` |

`PropertyRequest`:

```json
{
  "title": "Casa contemporanea com jardim",
  "description": "texto com pelo menos 20 caracteres",
  "purpose": "sale",
  "type": "house",
  "neighborhood": "Santa Paula",
  "city": "Sao Caetano do Sul",
  "price": 1850000,
  "condoFee": null,
  "propertyTax": 6400,
  "area": 280,
  "bedrooms": 3, "suites": 3, "bathrooms": 4, "parkingSpaces": 2,
  "features": ["Jardim privativo", "Churrasqueira"],
  "photos": ["/api/photos/6f1c...-....jpg"],
  "availableFrom": null,
  "featured": true,
  "status": "published"
}
```

`photos` aceita apenas caminhos emitidos por `/api/admin/photos`.

Transicoes de status (`PropertyStatus.canTransitionTo`):

```
draft     -> published, archived
published -> draft, archived
archived  -> draft
```

Um imovel arquivado precisa voltar a rascunho e ser revisado antes de ser publicado de novo.

## Fotos

| Metodo | Caminho | Acesso | Descricao |
|---|---|---|---|
| POST | `/api/admin/photos` | admin | multipart `file`; `201 { url: "/api/photos/<uuid>.<ext>" }`; `415` se nao for JPEG/PNG/WebP; `413` acima de 10 MB |
| GET | `/api/photos/{filename}` | publico | bytes da imagem com cache imutavel |

## Contatos

| Metodo | Caminho | Acesso | Descricao |
|---|---|---|---|
| POST | `/api/contacts` | publico | `{ propertyId?, name, phone, email, message }`; `201`; `404` se `propertyId` nao for de imovel publicado |
| GET | `/api/admin/contacts` | admin | Paginado, padrao `createdAt,desc`; inclui `propertyTitle` |
| GET | `/api/admin/contacts/summary` | admin | `{ unread }` |
| PATCH | `/api/admin/contacts/{id}/read` | admin | Marca como lido |

## Paginacao

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
```

`size` acima de 100 e reduzido a 100 (`spring.data.web.pageable.max-page-size`). `sort` aceita `campo,asc|desc`; campo inexistente responde `400`.

## Erros

Formato RFC 9457, `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Revise os campos informados",
  "instance": "/api/admin/properties",
  "errors": { "title": "size must be between 5 and 160" }
}
```

| Status | Quando |
|---|---|
| 400 | Validacao (`errors` por campo), JSON malformado, parametro invalido, `sort` desconhecido |
| 401 | Sem sessao, sessao inativa por mais de 2 h ou alem de 8 h, revogada, credencial invalida, codigo TOTP invalido, desafio MFA expirado |
| 403 | `Origin`/`Referer` ausente ou nao permitido em requisicao mutante, ou sem papel |
| 404 | Recurso inexistente ou nao publicado |
| 409 | Transicao de status invalida, MFA ja/nao configurado |
| 413 | Upload acima do limite |
| 415 | Arquivo nao e imagem suportada |
| 429 | Muitas tentativas de login ou TOTP |
| 500 | Erro inesperado; `errorId` para correlacao no log |
