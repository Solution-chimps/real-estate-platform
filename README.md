# Real Estate Platform - Backend

API REST da plataforma imobiliaria Constantino Imoveis & Patrimonio. Atende o portal publico (consulta de imoveis e contato) e o backoffice (gestao de imoveis, fotos e contatos) do frontend Angular em `../constantino-imoveis-web`.

- Regras de desenvolvimento: [`CLAUDE.md`](./CLAUDE.md)
- Documentacao tecnica (arquitetura, seguranca, modelo de dados, API, configuracao, decisoes): [`docs/`](./docs/README.md)

## Stack

- Java 21, Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Jackson 3)
- Spring Data JPA + Flyway (H2 em desenvolvimento, MySQL em producao)
- Spring Security com JWT HMAC em cookie HttpOnly, CSRF por cookie, MFA TOTP obrigatorio, Argon2id
- springdoc-openapi (Swagger UI apenas no perfil `dev`)
- Maven Wrapper

## Primeira execucao (perfil `dev`)

1. Copie `.env.example` para `.env` e preencha. Todas as variaveis marcadas como obrigatorias precisam existir; a aplicacao falha ao subir sem elas.

   ```sh
   openssl rand -base64 48   # APP_JWT_SECRET
   openssl rand -base64 32   # APP_MFA_ENCRYPTION_KEY
   ```

2. Gere o certificado local (TLS e obrigatorio tambem em desenvolvimento):

   ```sh
   APP_SSL_KEYSTORE_PASSWORD=<senha> ./scripts/generate-dev-cert.sh      # Linux/macOS/Git Bash
   set APP_SSL_KEYSTORE_PASSWORD=<senha> && scripts\generate-dev-cert.cmd  # Windows cmd
   ```

   O keystore vai para `./data/dev-keystore.p12` (ignorado pelo git).

3. Exporte as variaveis do `.env` no shell ou na configuracao de execucao da IDE e suba:

   ```sh
   ./mvnw spring-boot:run
   ```

   A API responde em `https://localhost:8443`. O certificado e autoassinado; aceite-o no navegador ou configure o proxy do Angular com `"secure": false`.

No primeiro start o Flyway cria o schema, `AdminUserInitializer` cria o usuario de `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` (apenas se nao existir nenhum usuario) e `DevDataSeeder` carrega imoveis e contatos de exemplo.

Ferramentas do perfil `dev`:

- Swagger UI: `https://localhost:8443/swagger-ui.html`
- Console H2: `https://localhost:8443/h2-console` (JDBC `jdbc:h2:file:./data/dev-db`, usuario `sa`, senha vazia)

## Rodando com o frontend

O frontend (`../constantino-imoveis-web`) roda em `https://localhost:4200` e descobre a API pelo seu `public/environment.json` (`apiBaseUrl: https://localhost:8443`). Para o navegador aceitar a conversa entre os dois:

1. `APP_CORS_ALLOWED_ORIGINS=https://localhost:4200` no ambiente da API (origens diferentes exigem CORS com credenciais).
2. Abra `https://localhost:8443/api/properties` uma vez no navegador e aceite o certificado autoassinado.
3. Ambos em HTTPS: os cookies de sessao sao `Secure` e `SameSite=Strict`, e `https://localhost:4200` e `https://localhost:8443` contam como o mesmo site.

Fluxo verificado de ponta a ponta no perfil `dev`: `GET /api/auth/csrf` -> `POST /api/auth/login` -> `POST /api/auth/mfa/setup` -> `POST /api/auth/mfa/verify` -> `GET /api/admin/properties`.

## Perfil `mysql`

```sh
SPRING_PROFILES_ACTIVE=mysql ./mvnw spring-boot:run
```

Usa `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD` e, por padrao, sobe o MySQL de `compose.yaml` via Docker Compose (`APP_DOCKER_COMPOSE=false` para desligar).

## Testes

```sh
./mvnw verify
```

A suite usa o perfil `test` (H2 em memoria) com fixtures proprios em `src/test/resources/application-test.yaml`.

## Fluxo de autenticacao

1. `GET /api/auth/csrf` recebe o cookie `XSRF-TOKEN`; toda requisicao mutante envia o valor no header `X-XSRF-TOKEN`.
2. `POST /api/auth/login` `{ email, password }` devolve `{ mfaRequired: true, enrollmentRequired }` e o cookie de desafio `constantino_mfa` (5 minutos). Nenhuma sessao e criada nesta etapa.
3. Primeiro acesso: `POST /api/auth/mfa/setup` devolve `{ secret, otpauthUri }` para cadastrar no aplicativo autenticador. O segredo e mostrado uma unica vez.
4. `POST /api/auth/mfa/verify` `{ code }` valida o TOTP, troca o desafio pela sessao (`constantino_session`, 8 horas) e devolve o usuario.
5. `GET /api/auth/me` e `POST /api/auth/logout`.

Todos os cookies sao `HttpOnly; Secure; SameSite=Strict; Path=/api`.

## Endpoints

| Metodo | Caminho | Acesso | Descricao |
|---|---|---|---|
| GET | `/api/properties` | publico | Imoveis publicados; filtros `purpose`, `type`, `neighborhood`, `query`; paginado |
| GET | `/api/properties/neighborhoods` | publico | Bairros com imovel publicado |
| GET | `/api/properties/{id}` | publico | Imovel publicado |
| POST | `/api/contacts` | publico | Registra interesse ou contato geral |
| GET | `/api/photos/{filename}` | publico | Foto enviada pelo backoffice |
| GET | `/api/admin/properties` | admin | Todos os imoveis, paginado |
| GET | `/api/admin/properties/{id}` | admin | Imovel por id |
| POST | `/api/admin/properties` | admin | Cria imovel (`201` + `Location`) |
| PUT | `/api/admin/properties/{id}` | admin | Atualiza imovel |
| PATCH | `/api/admin/properties/{id}/status` | admin | `{ status }`: draft, published, archived |
| DELETE | `/api/admin/properties/{id}` | admin | Remove imovel |
| POST | `/api/admin/photos` | admin | Upload multipart `file` (JPEG/PNG/WebP, 10 MB) |
| GET | `/api/admin/contacts` | admin | Contatos recebidos, paginado |
| GET | `/api/admin/contacts/summary` | admin | `{ unread }` |
| PATCH | `/api/admin/contacts/{id}/read` | admin | Marca como lido |

Erros seguem RFC 9457 (`application/problem+json`); validacao devolve `errors` por campo.
