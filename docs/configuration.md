# Configuracao e ambientes

## Perfis

| Perfil | Quando | Banco | Extras |
|---|---|---|---|
| `dev` (padrao) | desenvolvimento local | H2 em arquivo `./data/dev-db` | console H2 em `/h2-console`, Swagger UI, `DevDataSeeder`, log DEBUG do pacote da aplicacao |
| `mysql` | homologacao/producao ou local com Docker | MySQL 8.4 via `APP_DB_*` | Docker Compose ligado por padrao (`APP_DOCKER_COMPOSE=false` desliga) |
| `test` | suite automatizada | H2 em memoria | fixtures de `src/test/resources/application-test.yaml`, TLS desligado |

`spring.profiles.default: dev` faz o perfil `dev` valer quando nenhum e informado. Swagger e console H2 estao desligados em todos os outros perfis.

## Variaveis de ambiente

Todas as propriedades da aplicacao sao tipadas e validadas em `config/AppProperties.java`. Se uma obrigatoria faltar, a aplicacao nao sobe e o erro de bind aponta o campo.

| Variavel | Obrigatoria | Uso |
|---|---|---|
| `APP_JWT_SECRET` | sim | Chave HMAC-SHA256 dos JWT; minimo 32 caracteres (`openssl rand -base64 48`) |
| `APP_MFA_ENCRYPTION_KEY` | sim | Base64 de 32 bytes; AES-256-GCM dos seeds TOTP (`openssl rand -base64 32`) |
| `APP_SSL_KEYSTORE` | sim em `dev`/`mysql` | Caminho do PKCS12 (padrao `./data/dev-keystore.p12`) |
| `APP_SSL_KEYSTORE_PASSWORD` | sim em `dev`/`mysql` | Senha do keystore |
| `APP_SSL_ENABLED` | nao | `false` apenas se um proxy TLS na frente terminar HTTPS |
| `APP_PORT` | nao | Padrao `8443` |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_NAME`, `APP_ADMIN_PASSWORD` | primeiro start | Cria o primeiro administrador quando nao ha usuarios |
| `APP_CORS_ALLOWED_ORIGINS` | se o SPA estiver em outra origem | Lista separada por virgula, por exemplo `https://localhost:4200` |
| `APP_PHOTOS_DIR` | nao | Diretorio das fotos (padrao `./data/photos`) |
| `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD` | perfil `mysql` | Conexao MySQL |
| `MYSQL_ROOT_PASSWORD` | perfil `mysql` com Compose | Senha root do container |

`.env.example` lista todas com instrucoes. `.env` e `./data/` sao ignorados pelo git.

## TLS local

A regra do projeto e HTTPS em todo ambiente, inclusive desenvolvimento, para que cookies `Secure`/`SameSite=Strict` e HSTS se comportem como em producao. `scripts/generate-dev-cert.{sh,cmd}` cria um PKCS12 autoassinado para `localhost` em `./data`. Certificado e segredo, por isso nao e versionado.

O frontend em `ng serve --ssl` e a API em `https://localhost:8443` sao *same-site* (mesmo esquema e host, porta e ignorada), entao o cookie `SameSite=Strict` e enviado entre eles. Se o SPA rodasse em `http://localhost:4200`, o esquema diferente tornaria os sites distintos e o cookie seria bloqueado.

## Propriedades relevantes de `application.yaml`

| Propriedade | Valor | Motivo |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | Flyway e dono do schema |
| `spring.jpa.open-in-view` | `false` | Evita lazy loading fora da transacao e conexao presa durante a serializacao |
| `hibernate.timezone.default_storage` | `NORMALIZE_UTC` | `Instant` gravado em UTC em qualquer banco |
| `spring.servlet.multipart.max-file-size` | `10MB` | Limite de foto |
| `spring.data.web.pageable.max-page-size` | `100` | Protege contra listagens gigantes |
| `server.error.include-*` | `never` | Nenhum detalhe interno em erro |
| `app.security.token-ttl` | `PT8H` | Jornada de trabalho |
| `app.security.mfa-challenge-ttl` | `PT5M` | Tempo para abrir o autenticador |
| `app.security.max-login-attempts` / `login-lock-duration` | `5` / `PT15M` | Freio a forca bruta |

## Docker Compose

`compose.yaml` define um MySQL 8.4 com volume persistente e healthcheck. O suporte do Spring Boot a Docker Compose (`spring-boot-docker-compose`) sobe o servico ao iniciar a aplicacao no perfil `mysql` e deriva a conexao a partir dele. As senhas vem das mesmas variaveis de ambiente usadas pela aplicacao.
