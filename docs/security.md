# Seguranca

## Requisitos atendidos

| Requisito | Implementacao |
|---|---|
| Hash de senha forte | Argon2id (19 MiB, 2 iteracoes, p=1) via `DelegatingPasswordEncoder`; BCrypt custo 12 aceito para migracao |
| MFA obrigatorio, resistente a SIM swap | TOTP (RFC 6238) por aplicativo autenticador; nenhum SMS ou e-mail |
| Sessao opaca gerenciada pelo servidor | Cookie `CFID` com token aleatorio de 256 bits; so o hash SHA-256 fica no banco; inatividade 2 h, limite absoluto 8 h, revogacao real |
| Cookies seguros | Todos `HttpOnly; Secure; SameSite=Strict; Path=/api`; sem `JSESSIONID`; o frontend nunca le nem manipula cookie |
| Segredos fora do codigo | `APP_JWT_SECRET`, `APP_MFA_ENCRYPTION_KEY`, senha do admin e do keystore apenas por variavel de ambiente |
| TLS em todos os ambientes | `server.ssl.*` ligado por padrao, inclusive em `dev` |

## Fluxo de autenticacao

```
SPA                                   API
 |-- GET /api/auth/csrf -------------->|  cookie XSRF-TOKEN (HttpOnly) + { headerName, token }
 |-- POST /api/auth/login ------------>|  Argon2id verify, limite de tentativas
 |<-- { mfaRequired, enrollmentRequired } + cookie constantino_mfa (JWT, 5 min, purpose=mfa)
 |-- POST /api/auth/mfa/setup -------->|  (so no primeiro acesso) gera seed, cifra, devolve { secret, otpauthUri }
 |-- POST /api/auth/mfa/verify { code }|  valida TOTP, anti-replay, abre sessao em user_session
 |<-- { name, email, role } + Set-Cookie: CFID=<token opaco> (Max-Age 8 h) + expira constantino_mfa
 |-- GET /api/admin/... --------------->|  browser envia CFID -> SHA-256 -> user_session ativa -> ROLE_ADMIN
 |-- POST /api/auth/alive (a cada 5 min)|  renova last_seen_at
 |-- POST /api/auth/logout ----------->|  revoked_at = agora + cookie expirado
```

### Sessao opaca (`CFID`)

- **Token**: 32 bytes de `SecureRandom` em base64url (43 caracteres). Nao carrega informacao; so serve de chave de busca.
- **Armazenamento**: `user_session.token_hash = SHA-256(token)`. Um dump do banco nao contem tokens utilizaveis. Como o token tem 256 bits de entropia, um hash rapido e suficiente (nao ha o que forcar por dicionario).
- **Inatividade**: `last_seen_at + 2 h` (`session-idle-timeout`). Toda requisicao autenticada desliza a janela; a gravacao e feita no maximo uma vez por minuto por sessao para nao transformar cada `GET` em `UPDATE`.
- **Limite absoluto**: `expires_at = created_at + 8 h` (`session-absolute-ttl`). Vale mesmo com atividade continua; e o `Max-Age` do cookie.
- **Keep-alive**: `POST /api/auth/alive` forca o `last_seen_at` para agora. O SPA chama a cada 5 min enquanto o painel esta aberto; sem o painel aberto, a sessao morre em 2 h.
- **Revogacao**: logout grava `revoked_at`; a sessao deixa de valer imediatamente em qualquer aba. `SessionService.revokeAllForUser` existe para troca de senha ou bloqueio de conta.
- **Limite por usuario**: no maximo 5 sessoes ativas (`max-sessions-per-user`); ao abrir a sexta, a de `last_seen_at` mais antigo e revogada. Impede acumulo silencioso de sessoes a partir de uma credencial vazada.
- **Fixacao de sessao**: cada `mfa/verify` gera um token novo; nenhum token anterior ao login e aceito.
- **Cookie invalido**: se o browser enviar um `CFID` desconhecido, expirado ou revogado, a resposta traz `Set-Cookie: CFID=; Max-Age=0` e a requisicao segue anonima (401 nos endpoints protegidos).
- **Limpeza**: `SessionService.purgeStaleSessions` roda diariamente (03:00) e apaga sessoes expiradas ou revogadas ha mais de 7 dias.
- **User-Agent** e registrado por sessao para auditoria futura (listar/encerrar dispositivos).

O frontend nao sabe que o cookie existe: ele e emitido via `Set-Cookie`, guardado e reenviado pelo browser (`withCredentials`). Nenhum script le, armazena ou intercepta o valor.

### Por que o desafio MFA continua sendo JWT

O cookie `constantino_mfa` prova apenas que a senha foi aceita, dura 5 minutos e nao da acesso a nada alem de `/api/auth/mfa/*`. Um JWT assinado (HMAC-SHA256, claim `purpose=mfa`) resolve isso sem gravar nada no banco antes de a autenticacao estar completa. O decoder desses endpoints rejeita qualquer token sem `purpose=mfa`; o filtro de sessao, por sua vez, so aceita tokens que existam em `user_session`, entao um desafio apresentado como `CFID` falha a busca por hash. O teste `passwordAloneOnlyYieldsAnMfaChallengeThatCannotReachTheApi` garante isso.

### TOTP

- Algoritmo: HMAC-SHA1, 6 digitos, passo de 30 s, janela de 1 passo para cada lado.
- Implementacao propria em `security/Totp.java` sobre `javax.crypto.Mac`, validada contra os vetores do Apendice B da RFC 6238 em `TotpTest`.
- Seed de 20 bytes gerado com `SecureRandom`, exibido uma unica vez em Base32 junto com a URI `otpauth://`.
- **Anti-replay**: o ultimo passo aceito e gravado em `app_user.mfa_last_used_step`; codigo de passo igual ou anterior e recusado.
- Comparacao de codigos em tempo constante (`MessageDigest.isEqual`).

### Seed TOTP em repouso

Cifrado com AES-256-GCM (`security/SecretEncryptor.java`): nonce aleatorio de 12 bytes por operacao, tag de 128 bits, chave de 32 bytes em `APP_MFA_ENCRYPTION_KEY`.

### Limite de tentativas

`LoginAttemptService` mantem, em memoria, as falhas por conta (e-mail para senha, `mfa:<e-mail>` para TOTP). Apos 5 falhas em 15 min a API responde `429`. E por instancia; com mais de um no, migrar para um store compartilhado.

## CSRF

Cookies `SameSite=Strict` ja bloqueiam a maior parte dos ataques CSRF, mas o token sincronizador continua ativo como segunda camada:

- `CookieCsrfTokenRepository` com cookie `XSRF-TOKEN` **HttpOnly**. O SPA chama `GET /api/auth/csrf`, que devolve o token mascarado (`XorCsrfTokenRequestAttributeHandler`, protecao BREACH) e o nome do header.
- Toda requisicao `POST/PUT/PATCH/DELETE` precisa do header `X-XSRF-TOKEN`.
- O token **nao e rotacionado no login**: com a sessao resolvida a cada requisicao, a `CsrfAuthenticationStrategy` padrao do Spring trocaria o token em toda chamada autenticada e invalidaria o header que o SPA guarda. O token ja esta vinculado ao browser pelo cookie `HttpOnly; SameSite=Strict`, entao permanece estavel ate o cookie expirar (`NullAuthenticatedSessionStrategy` em `SecurityConfig`). O teste `csrfTokenObtainedBeforeLoginKeepsWorkingAfterTheSessionIsOpened` cobre o fluxo real.

## Cabecalhos de resposta

- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Frame-Options: DENY` e `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`
- `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` (padrao do Spring Security)
- Fotos: `Cache-Control: public, max-age=31536000, immutable` e `Content-Disposition: inline`.

## CORS

Apenas as origens de `APP_CORS_ALLOWED_ORIGINS`, com credenciais, metodos `GET, POST, PUT, PATCH, DELETE, OPTIONS` e headers `Content-Type, Accept, X-XSRF-TOKEN`. Nunca `*`.

## Autorizacao

`anyRequest().denyAll()`: endpoint novo e negado ate ganhar regra explicita. 401 e 403 passam por `ProblemAuthenticationEntryPoint`, que delega ao `HandlerExceptionResolver` para que a resposta seja o mesmo `ProblemDetail` dos demais erros.

## Upload de fotos

- Tipo decidido pelos magic bytes (`PhotoContentTypeDetector`): JPEG, PNG e WebP.
- Nome `UUID.ext` gerado no servidor; leitura confinada ao diretorio de fotos.
- 10 MB por arquivo; excesso responde `413`.

## Erros

`ApiExceptionHandler` converte tudo em `ProblemDetail`. Mensagens em portugues; nenhuma stack trace ou detalhe interno. Erros inesperados recebem `errorId` para correlacao com o log.

## Credenciais de teste

`src/test/resources/application-test.yaml` contem um segredo JWT e uma chave AES usados exclusivamente pela suite, marcados como tal. Sao a unica excecao a regra "nenhum segredo versionado".
