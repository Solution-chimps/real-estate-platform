# Seguranca

## Requisitos atendidos

| Requisito | Implementacao |
|---|---|
| Hash de senha forte | Argon2id (19 MiB, 2 iteracoes, p=1) via `DelegatingPasswordEncoder`; BCrypt custo 12 aceito para migracao |
| MFA obrigatorio, resistente a SIM swap | TOTP (RFC 6238) por aplicativo autenticador; nenhum SMS ou e-mail |
| Cookies seguros | Todos `HttpOnly; Secure; SameSite=Strict; Path=/api`; sem `JSESSIONID` |
| Segredos fora do codigo | `APP_JWT_SECRET`, `APP_MFA_ENCRYPTION_KEY`, senha do admin e do keystore apenas por variavel de ambiente |
| TLS em todos os ambientes | `server.ssl.*` ligado por padrao, inclusive em `dev` |

## Fluxo de autenticacao

```
SPA                                   API
 |-- GET /api/auth/csrf -------------->|  cookie XSRF-TOKEN (HttpOnly) + { headerName, token }
 |-- POST /api/auth/login ------------>|  Argon2id verify, limite de tentativas
 |<-- { mfaRequired, enrollmentRequired } + cookie constantino_mfa (5 min, purpose=mfa)
 |-- POST /api/auth/mfa/setup -------->|  (so no primeiro acesso) gera seed, cifra, devolve { secret, otpauthUri }
 |-- POST /api/auth/mfa/verify { code }|  valida TOTP, anti-replay, marca MFA ativo
 |<-- { name, email, role } + cookie constantino_session (8 h, purpose=session) + expira constantino_mfa
 |-- GET /api/admin/... --------------->|  cookie de sessao -> JWT -> ROLE_ADMIN
```

### Por que dois tokens

A senha sozinha nunca produz acesso a API. O cookie `constantino_mfa` carrega um JWT com `purpose=mfa`, sem papeis e com 5 minutos de vida; ele so e aceito pelos endpoints `/api/auth/mfa/*`, que o decodificam com um decoder dedicado (`mfaChallengeJwtDecoder`). O decoder do resource server exige `purpose=session`, entao um desafio apresentado como sessao e rejeitado antes de qualquer autorizacao. O teste `passwordAloneOnlyYieldsAnMfaChallengeThatCannotReachTheApi` garante isso.

### TOTP

- Algoritmo: HMAC-SHA1, 6 digitos, passo de 30 s, janela de 1 passo para cada lado (tolerancia a relogio).
- Implementacao propria em `security/Totp.java` sobre `javax.crypto.Mac`, validada contra os vetores do Apendice B da RFC 6238 em `TotpTest`. Nao e criptografia inventada: e a construcao padrao implementada por todos os aplicativos autenticadores; evitou-se uma dependencia externa para um algoritmo de 40 linhas.
- Seed de 20 bytes gerado com `SecureRandom`, exibido ao usuario em Base32 uma unica vez (`/api/auth/mfa/setup`) junto com a URI `otpauth://` para QR code.
- **Anti-replay**: o ultimo passo aceito e gravado em `app_user.mfa_last_used_step`; qualquer codigo de passo igual ou anterior e recusado, mesmo dentro da janela.
- Comparacao de codigos com `MessageDigest.isEqual` (tempo constante).

### Seed TOTP em repouso

O seed precisa ser recuperado para validar codigos, entao nao pode ser hasheado. Ele e cifrado com AES-256-GCM (`security/SecretEncryptor.java`): nonce aleatorio de 12 bytes por operacao, tag de 128 bits, chave de 32 bytes vinda de `APP_MFA_ENCRYPTION_KEY`. Vazamento do banco sem a chave nao expoe os seeds.

### Limite de tentativas

`LoginAttemptService` mantem, em memoria, as falhas por conta (e-mail para senha, `mfa:<e-mail>` para TOTP). Apos `max-login-attempts` (5) falhas em `login-lock-duration` (15 min) a API responde `429`. O contador zera no sucesso. E por instancia; com mais de um no, migrar para um store compartilhado (Redis) sem mudar a interface.

## Sessao

- JWT HMAC-SHA256 assinado e verificado pelo `NimbusJwtEncoder`/`NimbusJwtDecoder` do proprio Spring Security. Claims: `iss`, `sub` (e-mail), `iat`, `exp`, `purpose`, `roles`, `amr=["pwd","otp"]`, `name`.
- Vida de 8 h (`token-ttl`). Nao ha refresh nem revogacao server-side: logout apenas expira o cookie no navegador. Se revogacao imediata virar requisito, a opcao e uma denylist por `jti` com TTL igual ao do token.
- Cookie `constantino_session`: `HttpOnly` (JS nao le), `Secure` (so HTTPS), `SameSite=Strict` (nunca enviado em navegacao ou requisicao iniciada por outro site), `Path=/api`.

## CSRF

Cookies `SameSite=Strict` ja bloqueiam a maior parte dos ataques CSRF, mas o token sincronizador continua ativo como segunda camada:

- Repositorio `CookieCsrfTokenRepository` com cookie `XSRF-TOKEN` **HttpOnly**. O SPA nao le o cookie: ele chama `GET /api/auth/csrf`, que devolve o token mascarado (`XorCsrfTokenRequestAttributeHandler`, protecao BREACH) e o nome do header.
- Toda requisicao `POST/PUT/PATCH/DELETE` precisa do header `X-XSRF-TOKEN`; o filtro desmascara e compara com o cookie.
- O cookie `HttpOnly` foi escolhido porque o frontend pode estar em outro host (`www` vs `api`) e nao conseguiria ler o cookie de qualquer forma; com o token no corpo da resposta, o cookie nao precisa ser legivel por script.

## Cabecalhos de resposta

- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Frame-Options: DENY` e `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` (a API nao serve HTML)
- `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` (padrao do Spring Security)
- Fotos: `Cache-Control: public, max-age=31536000, immutable` e `Content-Disposition: inline`.

## CORS

Apenas as origens listadas em `APP_CORS_ALLOWED_ORIGINS`, com credenciais, metodos `GET, POST, PUT, PATCH, DELETE, OPTIONS` e headers `Content-Type, Accept, X-XSRF-TOKEN`. Nunca `*`.

## Autorizacao

`anyRequest().denyAll()`: endpoint novo e negado ate ganhar regra explicita. O erro de autenticacao (`401`) e de autorizacao (`403`) passam por `ProblemAuthenticationEntryPoint`, que delega ao `HandlerExceptionResolver` para que a resposta seja o mesmo `ProblemDetail` dos demais erros.

## Upload de fotos

- O `Content-Type` e a extensao enviados pelo cliente sao ignorados. `PhotoContentTypeDetector` le os primeiros 12 bytes e aceita apenas assinaturas JPEG, PNG e WebP.
- O nome do arquivo e `UUID.ext` gerado no servidor; o original e descartado.
- Leitura valida o nome contra `^<uuid>\.(jpg|png|webp)$` e confirma que o caminho normalizado continua dentro do diretorio de fotos (sem path traversal).
- Tamanho limitado a 10 MB por arquivo (`spring.servlet.multipart.*`); excesso responde `413`.

## Erros

`ApiExceptionHandler` converte tudo em `ProblemDetail` (RFC 9457). Mensagens de usuario em portugues; nenhuma stack trace, nome de classe ou SQL. Erros inesperados recebem um `errorId` que aparece na resposta e no log para correlacao.

## Credenciais de teste

`src/test/resources/application-test.yaml` contem um segredo JWT e uma chave AES usados exclusivamente pela suite automatizada, marcados como tal. Sao a unica excecao a regra "nenhum segredo versionado" e nunca devem ser reutilizados.
