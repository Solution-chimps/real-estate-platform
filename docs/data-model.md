# Modelo de dados

Schema gerenciado exclusivamente pelo Flyway (`src/main/resources/db/migration`). Hibernate roda com `ddl-auto: validate` e falha ao subir se entidade e tabela divergirem.

## Tabelas

### app_user

| Coluna | Tipo | Observacao |
|---|---|---|
| id | CHAR(36) | UUID gerado na aplicacao |
| email | VARCHAR(255) | unico, armazenado em minusculas |
| name | VARCHAR(120) | |
| password_hash | VARCHAR(255) | `{argon2}$argon2id$...` (DelegatingPasswordEncoder) |
| role | VARCHAR(20) | `ADMIN` |
| enabled | BOOLEAN | |
| mfa_secret | VARCHAR(255) | base64(nonce + AES-GCM(seed)); nulo ate o enrolamento |
| mfa_enabled | BOOLEAN | vira `true` no primeiro codigo valido |
| mfa_last_used_step | BIGINT | ultimo passo TOTP aceito (anti-replay) |
| created_at | TIMESTAMP(6) | UTC |

### user_session

| Coluna | Tipo | Observacao |
|---|---|---|
| id | CHAR(36) | |
| user_id | CHAR(36) | FK `app_user`, `ON DELETE CASCADE` |
| token_hash | CHAR(64) | SHA-256 hex do token opaco do cookie `CFID`; unico; o token em si nunca e gravado |
| user_agent | VARCHAR(255) | auditoria |
| created_at | TIMESTAMP(6) | |
| last_seen_at | TIMESTAMP(6) | deslizante; sessao expira em `last_seen_at + 2 h` |
| expires_at | TIMESTAMP(6) | limite absoluto `created_at + 8 h` |
| revoked_at | TIMESTAMP(6) | preenchido no logout ou ao exceder 5 sessoes ativas |

Indices: `uk_user_session_token_hash` (busca por request), `idx_user_session_user (user_id, revoked_at, expires_at)` para o limite por usuario, `idx_user_session_expires` para a limpeza diaria. Criada em `V2__user_session.sql`.

### property

| Coluna | Tipo | Observacao |
|---|---|---|
| id | CHAR(36) | |
| code | VARCHAR(20) | `CST-0001`, unico, gerado por `PropertyCodeGenerator` |
| title, description | VARCHAR(160), VARCHAR(4000) | |
| purpose | VARCHAR(20) | `SALE`, `RENT`, `SHORT_STAY` |
| property_type | VARCHAR(20) | `APARTMENT`, `HOUSE`, `COMMERCIAL`, `LAND` (`type` evitado por ser palavra reservada em alguns bancos) |
| neighborhood, city | VARCHAR(120) | |
| price, condo_fee, property_tax | DECIMAL(14,2) | `condo_fee` e `property_tax` opcionais |
| area | DECIMAL(10,2) | m2 |
| bedrooms, suites, bathrooms, parking_spaces | INT | |
| available_from | DATE | opcional |
| featured | BOOLEAN | aparece na home |
| status | VARCHAR(20) | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| created_at, updated_at | TIMESTAMP(6) | |

Indices: `idx_property_status`, `idx_property_status_updated (status, updated_at)` para a listagem publica ordenada por atualizacao.

### property_feature e property_photo

Colecoes de valores (`@ElementCollection` com `@OrderColumn(position)`), chave primaria `(property_id, position)`, `ON DELETE CASCADE`. Guardam, respectivamente, caracteristicas em texto livre e caminhos de foto (`/api/photos/<uuid>.<ext>`). A ordem e significativa: a primeira foto e a capa.

Carregamento: `@BatchSize(50)` em ambas. A listagem e paginada e um `join fetch` com paginacao forcaria o Hibernate a paginar em memoria; com batch, uma pagina de 20 imoveis custa tres consultas.

### property_code_counter

Uma unica linha (`id = 1`) com `next_value`. `PropertyCodeGenerator.next()` le com `PESSIMISTIC_WRITE` dentro da transacao do `create`, formata `CST-%04d` e incrementa. Foi preferido a `SEQUENCE` porque MySQL nao tem sequencias, e a `MAX(code)+1` porque este tem condicao de corrida.

### contact

| Coluna | Tipo | Observacao |
|---|---|---|
| id | CHAR(36) | |
| property_id | CHAR(36) | nulo em contato geral; `ON DELETE SET NULL` preserva o historico se o imovel for removido |
| name, phone, email | VARCHAR | e-mail em minusculas |
| message | VARCHAR(2000) | |
| is_read | BOOLEAN | `read` evitado por ser palavra reservada |
| created_at | TIMESTAMP(6) | |

`ContactRepository.findAll(Pageable)` usa `@EntityGraph("property")` para trazer o titulo do imovel sem N+1.

## Portabilidade H2 / MySQL

A migration e unica. O tipo de timestamp varia por placeholder Flyway: `${timestampType}` e `TIMESTAMP(6)` no padrao (H2) e `DATETIME(6)` no perfil `mysql` (o `TIMESTAMP` do MySQL tem limite em 2038). `CHAR(36)`, `DECIMAL`, `BOOLEAN` e `VARCHAR` sao equivalentes nos dois.

Instantes sao gravados em UTC (`hibernate.timezone.default_storage=NORMALIZE_UTC`), para que o valor independa do fuso do servidor de banco.

## Evolucao

Nova mudanca de schema = novo arquivo `V<n>__<descricao>.sql`. Migration ja aplicada nunca e editada. `V1__init.sql` (schema base) e `V2__user_session.sql` (sessoes) ja existem; qualquer ajuste entra como `V3`.
