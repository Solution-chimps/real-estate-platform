# Decisoes tecnicas

Registro das escolhas que nao sao obvias pelo codigo, com as alternativas consideradas.

## 1. Sessao opaca no servidor (cookie `CFID`), sem JWT de sessao

**Decisao**: a sessao e uma linha em `user_session`; o cookie `CFID` (`HttpOnly; Secure; SameSite=Strict`) carrega um token aleatorio de 256 bits cujo SHA-256 e a chave da linha. Inatividade de 2 h, limite absoluto de 8 h, keep-alive em `/api/auth/alive`, revogacao no logout, 5 sessoes por usuario.

**Alternativas**: JWT assinado em cookie (stateless, mas sem revogacao e sem inatividade real; foi a primeira versao); sessao HTTP do servlet com `JSESSIONID` (acopla ao container e a replicacao de sessao); token Bearer guardado pelo SPA (exposto a XSS, proibido pelas regras do frontend).

**Por que**: o padrao da empresa exige token opaco com ciclo de vida controlado pelo servidor e renovado por keep-alive. Com a sessao no banco, logout e bloqueio revogam de imediato, inatividade e limite absoluto sao aplicados com precisao e o token nao carrega nada que valha a pena decodificar. O custo e uma consulta indexada por requisicao autenticada, aceitavel para o volume do backoffice. O frontend nao sabe que o cookie existe: so o browser o guarda e reenvia.

## 2. Login em duas etapas: desafio JWT curto, sessao opaca depois

**Decisao**: a senha gera apenas um desafio JWT (`purpose=mfa`, 5 min, sem papeis) no cookie `constantino_mfa`. A sessao `CFID` nasce no `mfa/verify`.

**Alternativa**: abrir a sessao na senha e marcar uma flag "MFA pendente" verificada por filtro; ou gravar o desafio no banco.

**Por que**: um JWT assinado resolve o desafio sem gravar nada antes de a autenticacao estar completa. O filtro de sessao so aceita tokens presentes em `user_session`, entao um desafio apresentado como `CFID` falha a busca por hash por construcao, nao por checagem adicional em cada endpoint. O enrolamento obrigatorio fica natural: `setup` so aceita o desafio.

## 3. TOTP implementado no projeto

**Decisao**: `security/Totp.java` (RFC 6238 sobre HMAC-SHA1 do JCA) e `Base32.java`.

**Alternativa**: biblioteca de terceiros.

**Por que**: o algoritmo tem poucas dezenas de linhas, depende apenas de `javax.crypto` e e verificado contra os vetores oficiais da RFC em teste. Uma dependencia externa traria superficie de manutencao e licenca para pouco ganho. Nao ha criptografia inventada: HMAC vem do JCA.

## 4. Argon2id como hash padrao, BCrypt como legado

**Decisao**: `DelegatingPasswordEncoder` com id `argon2` (19 MiB, t=2, p=1) e `bcrypt` (custo 12) registrado.

**Por que**: Argon2id e a recomendacao atual da OWASP e resistente a GPU/ASIC; o prefixo `{argon2}` no hash permite trocar parametros ou algoritmo no futuro sem migracao em massa (os hashes antigos continuam validaveis). Exige Bouncy Castle no classpath.

## 5. Seed TOTP cifrado com AES-GCM, chave por ambiente

**Decisao**: `SecretEncryptor` com AES-256-GCM e nonce aleatorio.

**Alternativa**: guardar o seed em claro ("o banco ja e protegido").

**Por que**: um dump do banco com seeds em claro permite gerar codigos validos para qualquer conta. Com a chave fora do banco, o dump sozinho nao basta.

## 6. CSRF com token no corpo da resposta e cookie HttpOnly

**Decisao**: `GET /api/auth/csrf` devolve `{ headerName, token }`; cookie `XSRF-TOKEN` e `HttpOnly`.

**Alternativa**: padrao Angular `withXsrfConfiguration` lendo o cookie via `document.cookie`.

**Por que**: o frontend descobre o host da API em tempo de execucao e pode estar em outro host; nesse caso nao enxerga o cookie da API. Entregar o token no corpo funciona em qualquer topologia e ainda permite o cookie `HttpOnly`. `SameSite=Strict` ja e a primeira barreira; o token e a segunda.

## 7. Flyway com `ddl-auto: validate`

**Decisao**: schema versionado em SQL; Hibernate apenas valida.

**Alternativa**: `ddl-auto: update`.

**Por que**: `update` nao remove colunas, nao renomeia, nao cria indices compostos previsiveis e esconde divergencias entre ambientes. Migrations sao revisaveis em code review e identicas em todos os ambientes.

## 8. Contador de codigo com lock pessimista

**Decisao**: tabela `property_code_counter` lida com `PESSIMISTIC_WRITE` em `Propagation.MANDATORY`.

**Alternativas**: `SEQUENCE` (inexistente no MySQL), `MAX(code)+1` (condicao de corrida), UUID como codigo (ilegivel para a equipe).

**Por que**: codigo curto e sequencial e o que a imobiliaria usa no dia a dia; o lock de uma linha dentro da transacao do `create` garante unicidade sem depender do banco.

## 9. `@BatchSize` nas colecoes do imovel

**Decisao**: `features` e `photos` com `@BatchSize(50)`.

**Alternativa**: `join fetch`/`@EntityGraph`.

**Por que**: a listagem e paginada. Fetch join de colecao com `Pageable` faz o Hibernate paginar em memoria (`HHH90003004`). Batch mantem a paginacao no banco e limita a pagina a tres consultas.

## 10. `ProblemDetail` para todo erro, incluindo seguranca

**Decisao**: `ApiExceptionHandler` unico; `ProblemAuthenticationEntryPoint` delega 401/403 ao `HandlerExceptionResolver`.

**Por que**: o frontend trata um unico formato (`detail` para o usuario, `errors` por campo). Mensagens tecnicas nunca vazam, e erros inesperados ganham `errorId` para correlacao com o log.

## 11. Upload validado por assinatura de arquivo

**Decisao**: magic bytes decidem o tipo; nome gerado; leitura confinada ao diretorio.

**Por que**: `Content-Type` e extensao sao controlados pelo cliente. Um HTML renomeado para `.jpg` servido com o tipo errado vira vetor de XSS; a assinatura binaria nao.

## 12. Limite de tentativas em memoria

**Decisao**: `LoginAttemptService` com mapa concorrente por conta.

**Por que**: simples, sem dependencia, suficiente para uma instancia. A interface (`assertAllowed`/`recordFailure`/`reset`) permite trocar o armazenamento por Redis quando houver mais de um no.

## 13. Hora via `Clock` injetado

**Decisao**: nenhum `Instant.now()` fora do bean `Clock`.

**Por que**: expiracao de token, janela TOTP e bloqueio de tentativas sao testados com relogio fixo ou avancado manualmente, sem `Thread.sleep` e sem testes intermitentes.

## 14. TLS tambem em desenvolvimento

**Decisao**: `server.ssl` ligado por padrao; keystore local gerado por script.

**Por que**: cookies `Secure` e `SameSite=Strict` entre `localhost:4200` e `localhost:8443` so funcionam com o mesmo esquema. Desenvolver em HTTP esconderia exatamente os problemas que apareceriam em producao.

## 15. Identificadores em ingles, textos em portugues

**Decisao**: codigo, banco, JSON e endpoints em ingles; `detail`, mensagens de validacao e textos de seed em portugues.

**Por que**: alinhamento com o frontend (mesma regra no `CLAUDE.md` dele) e com o ecossistema das bibliotecas; o usuario final so ve portugues.
