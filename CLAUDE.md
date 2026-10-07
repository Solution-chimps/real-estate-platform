# CLAUDE.md

Regras obrigatorias para qualquer alteracao de codigo neste repositorio. Nao sao sugestoes.

Este e o unico documento de regras do projeto. `AGENTS.md` aponta para ele e nao duplica conteudo. Nao crie regra em outro arquivo: se uma regra muda, ela muda aqui.

---

## 1. Estrutura

Este repositorio e o backend: **real-estate-platform**, Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Jackson 3), Java 21, Maven. Expoe a API REST consumida pelo frontend `../constantino-imoveis-web` (Angular), que tem seu proprio `CLAUDE.md`.

Camadas, cada uma respeitando seu limite:

```
com.br.real_estate_platform
  controller/   HTTP: mapeamento, validacao de entrada (@Valid), status code. Nenhuma regra de negocio.
  service/      regra de negocio, transacao, orquestracao. Nao conhece HttpServletRequest.
  repository/   Spring Data JPA, Specifications. Nenhuma logica alem de consulta.
  entity/       entidades JPA e enums de dominio. Nunca saem do backend.
  dto/          records de entrada e saida da API.
  mapper/       entity <-> dto. Unico lugar que conhece os dois lados.
  security/     filtros, JWT, cookies, CSRF, TOTP, cifragem de segredos.
  config/       propriedades tipadas (AppProperties), beans transversais (Clock, OpenAPI, conversores).
  exception/    excecoes de dominio e o @RestControllerAdvice.
  validation/   constraints Bean Validation customizadas.
resources/
  application.yaml          defaults seguros; segredos so por variavel de ambiente
  application-dev.yaml      H2 em arquivo, console H2, Swagger
  application-mysql.yaml    MySQL via env + Docker Compose
  db/migration/             Flyway, fonte unica do schema
```

Estado atual: API completa para imoveis (publica e admin), contatos, fotos e autenticacao em dois fatores. Dados em H2 no perfil `dev`, MySQL no perfil `mysql`. Nenhum endpoint e exposto sem passar pela `SecurityFilterChain` de `security/SecurityConfig.java`.

---

## 2. Principio geral

**Quem planta gambiarra, colhe bug.**

- Nao existe "depois eu arrumo". Codigo entra pronto ou nao entra.
- Proibido contornar um erro mascarando o sintoma. Corrija a causa.
- Proibido `@SuppressWarnings` para calar o compilador, cast inseguro e `Optional.get()` sem verificacao. Se o tipo esta errado, o tipo se conserta.
- Proibido desabilitar teste, validacao ou regra de seguranca para fazer o build passar.
- Se a solucao correta exige mais tempo do que foi pedido, pare e relate. Nao entregue um paliativo sem avisar.
- Nao use emojis em codigo, comentario, commit, log, documentacao ou resposta da API.

---

## 3. Comentarios e legibilidade

**Codigo bom e codigo que se le sem explicacao.** Comentario que descreve *o que* o codigo faz e sinal de que o codigo deveria ter sido escrito melhor.

Antes de comentar, tente nesta ordem: renomear, extrair metodo, tipar (record, enum, tipo nomeado). Comente so quando a informacao nao cabe no codigo, e explique **por que**, nunca o que:

- Decisao nao obvia, sobretudo quando a alternativa obvia esta errada.
- Regra de negocio de origem externa (lei, contrato, exigencia do cliente). Cite a origem.
- Contorno de bug de terceiro, com link e condicao para remover.
- Restricao de seguranca ou performance que o proximo dev quebraria sem saber.
- Algoritmo nao trivial: registre a RFC, a formula ou a invariante.

Proibido: comentario que repete o codigo, Javadoc em metodo autoexplicativo, banner, separador, autoria, data, codigo comentado, `TODO`/`FIXME` (pendencia real se relata, nao se enterra), comentario desatualizado.

Formato: uma ou duas linhas acima do trecho, em ingles, comecando pelo motivo.

---

## 4. Java

### 4.1 Assinaturas

- **Sempre declare o modificador de acesso e o tipo de retorno.** Sem excecao.
- Campos de dependencia: `private final`, injetados por construtor. Lombok `@RequiredArgsConstructor` e o padrao. Proibido `@Autowired` em campo e injecao por setter.
- Parametro, variavel local e campo que nao sao reatribuidos sao tratados como imutaveis; prefira `final` em campos e evite reatribuir parametros.
- Metodo utilitario sem estado: classe `final` com construtor privado e metodos `static`.

### 4.2 Tipagem

- DTO de entrada e saida e `record`. Nunca exponha entidade JPA em controller.
- Enum de dominio com valor publico em kebab-case (`@JsonValue`/`@JsonCreator` + `fromValue`), persistido como `EnumType.STRING`. Nunca `ORDINAL`.
- Dinheiro e area em `BigDecimal` com `precision`/`scale` explicitos. Nunca `double`.
- Datas: `Instant` para auditoria (armazenado em UTC, `hibernate.timezone.default_storage=NORMALIZE_UTC`), `LocalDate` para datas de negocio. Hora atual sempre via `java.time.Clock` injetado, nunca `Instant.now()` direto fora do bean `Clock`.
- Identificador de entidade: `UUID` gerado na aplicacao, coluna `CHAR(36)` via `@JdbcTypeCode(SqlTypes.CHAR)`.
- `Optional` e tipo de retorno, nunca de parametro ou campo.

### 4.3 Lombok

- Permitido: `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Slf4j`.
- Proibido `@Data`, `@EqualsAndHashCode` e `@ToString` em entidade JPA: `equals`/`hashCode` sobre todos os campos dispara carga de relacao lazy e quebra em colecoes. Proibido `@Builder` em entidade.

---

## 5. Camadas e transacoes

- `controller` recebe e devolve `dto`, chama um `service`, define status HTTP. Nunca acessa `repository`.
- `service` e a fronteira transacional: `@Transactional(readOnly = true)` na classe e `@Transactional` nos metodos que escrevem. Nunca abra transacao em controller.
- `entity` nunca atravessa o `service` para fora: todo retorno de `service` e `dto`, convertido no `mapper`.
- `mapper` e um `@Component` sem estado, sem acesso a repositorio.
- Um servico, uma responsabilidade. `PropertyService` nao envia e-mail, `PhotoStorageService` nao conhece imovel.
- Regras de transicao de estado ficam no enum (`PropertyStatus.canTransitionTo`), nao espalhadas em `if` no servico.

---

## 6. Persistencia

- **Flyway e a unica fonte do schema.** `spring.jpa.hibernate.ddl-auto` e `validate` em todo perfil e nunca muda. Alteracao de schema e uma nova migration `V<n>__<descricao>.sql`; migration ja aplicada nunca e editada.
- SQL de migration portavel entre H2 e MySQL. Tipo de timestamp via placeholder `${timestampType}` (definido por perfil). Nomes em snake_case.
- `spring.jpa.open-in-view` e `false`. Carregue o que a resposta precisa dentro da transacao do servico.
- N+1: resolva com `@EntityGraph`, `join fetch` ou `@BatchSize` quando houver paginacao (fetch join com paginacao pagina em memoria). Nunca itere chamando o repositorio.
- Consulta dinamica com `Specification`. Proibido concatenar string em JPQL/SQL; todo valor do usuario e parametro vinculado e texto de `LIKE` e escapado.
- Colecao de valores (`@ElementCollection`) com `@OrderColumn`; a entidade expoe `replaceX(List)` em vez de setter que troca a referencia.
- Contador ou sequencia de negocio (codigo `CST-0001`) usa linha com `PESSIMISTIC_WRITE` dentro da transacao do chamador (`Propagation.MANDATORY`). Nunca `max(coluna) + 1`.

---

## 7. API

- Prefixo `/api`. Publico sem autenticacao: `GET /api/properties/**`, `GET /api/photos/**`, `POST /api/contacts`. Administrativo: `/api/admin/**`, exige `ROLE_ADMIN`. Autenticacao: `/api/auth/**`.
- Caminhos e chaves JSON em ingles e camelCase. Mensagens de erro para o usuario em portugues.
- Erro sempre como `ProblemDetail` (RFC 9457) via `exception/ApiExceptionHandler.java`. Nunca stack trace, mensagem de excecao bruta, nome de classe ou detalhe de infraestrutura na resposta. Erro inesperado devolve `errorId` e e logado com ele.
- Validacao de entrada com Bean Validation (`@Valid`) no controller. A validacao do servidor e a que vale; a do frontend e usabilidade.
- Listagem e paginada (`Pageable`, `PageResponse`), com tamanho maximo definido em `spring.data.web.pageable.max-page-size`. Nunca devolva lista sem limite.
- Criacao devolve `201` com `Location`. Remocao devolve `204`. Mudanca parcial e `PATCH`.
- Resposta nunca ecoa segredo, hash de senha ou segredo TOTP. O segredo TOTP aparece exatamente uma vez, na resposta de `/api/auth/mfa/setup`.

---

## 8. Seguranca

### 8.1 Autenticacao e sessao

- Stateless. `SessionCreationPolicy.STATELESS`; nao existe `JSESSIONID`.
- Login em duas etapas obrigatorias: senha (`POST /api/auth/login`) emite apenas um desafio curto (cookie `constantino_mfa`, 5 minutos, claim `purpose=mfa`, sem papel). A sessao real (cookie `constantino_session`, claim `purpose=session`) so nasce em `POST /api/auth/mfa/verify` com codigo TOTP valido. O decoder do resource server rejeita qualquer token cujo `purpose` nao seja `session`.
- MFA e via aplicativo autenticador (TOTP RFC 6238, SHA-1, 6 digitos, 30 s, janela de 1 passo). Proibido SMS ou e-mail como segundo fator. Primeiro login exige enrolamento em `/api/auth/mfa/setup`.
- Anti-replay: o ultimo passo TOTP aceito e persistido (`mfa_last_used_step`); codigo de passo igual ou anterior e recusado.
- Todo cookie emitido pela API e `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api`. `cookie-secure` so e `false` no perfil de teste.
- JWT assinado com HMAC-SHA256 pelo `JwtEncoder` do Spring Security (Nimbus). Segredo com no minimo 32 caracteres, vindo de `APP_JWT_SECRET`. Nao adicione biblioteca de JWT.
- Limite de tentativas por conta para senha e para TOTP (`LoginAttemptService`). Resposta `429` quando excedido.

### 8.2 Senhas e segredos

- Hash de senha: Argon2id (19 MiB, 2 iteracoes, paralelismo 1) via `DelegatingPasswordEncoder`, com BCrypt custo 12 aceito para migracao. Proibido MD5, SHA-1, SHA-256 puro ou qualquer hash sem sal e sem fator de custo.
- Segredo que precisa ser recuperado (seed TOTP) e cifrado em repouso com AES-256-GCM (`security/SecretEncryptor.java`), chave em `APP_MFA_ENCRYPTION_KEY`. Nunca armazene seed em claro.
- Proibido criptografia artesanal. Use JCA/Spring Security. Proibido ECB, chave estatica no codigo, IV reutilizado.
- Segredo, senha, chave e certificado vem de variavel de ambiente. Nunca em `application*.yaml` versionado, codigo-fonte ou log. A unica excecao sao os valores de `src/test/resources/application-test.yaml`, usados exclusivamente pela suite automatizada e marcados como tal.
- Nao logue senha, token, cookie, codigo TOTP, corpo de requisicao autenticada ou dado pessoal.

### 8.3 Transporte e navegador

- HTTPS em todos os perfis, inclusive `dev` (`server.ssl.*` com keystore local gerado por `scripts/generate-dev-cert`). Nenhuma URL `http://` em codigo ou configuracao.
- CSRF ativo com `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN` legivel pelo SPA, header `X-XSRF-TOKEN`) e `SpaCsrfTokenRequestHandler`. Nao desabilite CSRF fora da chain do console H2 em `dev`.
- Headers: HSTS, `X-Frame-Options: DENY`, CSP `default-src 'none'`, `X-Content-Type-Options: nosniff` (padrao do Spring Security). Nao relaxe sem justificativa escrita.
- CORS permite apenas origens de `APP_CORS_ALLOWED_ORIGINS`, com credenciais. Nunca `*`.
- `anyRequest().denyAll()`. Endpoint novo precisa de regra explicita em `SecurityConfig`.

### 8.4 Upload

- Tipo de arquivo decidido pelos magic bytes (`PhotoContentTypeDetector`), nunca pelo `Content-Type` ou extensao enviados pelo cliente. Aceitos: JPEG, PNG, WebP.
- Nome de arquivo gerado (`UUID.ext`); o nome do cliente e descartado. Leitura valida o nome contra `PhotoNaming.FILENAME_PATTERN` e confirma que o caminho resolvido continua dentro do diretorio de fotos.
- Tamanho limitado por `spring.servlet.multipart.*`. Fotos servidas com `Content-Disposition: inline` e cache imutavel.

### 8.5 Vulnerabilidades: prioridade maxima

Vulnerabilidade encontrada tem precedencia sobre qualquer tarefa. Pare, relate (classificacao, arquivo e linha, vetor, dado exposto, correcao proposta) antes de corrigir, nunca commite correcao silenciosa e nunca publique exploit funcional. Segredo exposto em codigo ou historico e Critico: trate como comprometido e rotacione.

---

## 9. Testes

Teste **somente onde e realmente necessario**. Cobertura nao e meta.

Escreva teste para: autorizacao e regras da `SecurityFilterChain`; fluxo de autenticacao e MFA; cifragem, token e TOTP (use vetores publicados, por exemplo RFC 6238 Apendice B); regra de negocio com ramificacao (transicao de status, limite de tentativas); validador customizado; deteccao de tipo de arquivo; mapper com transformacao real; regressao de bug.

Nao escreva teste para: `contextLoads`, getter/setter, record, mapeamento trivial, comportamento do Spring ou de biblioteca.

- Integracao com `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`. Perfil `test` usa H2 em memoria e os fixtures de `application-test.yaml`.
- Requisicao mutante em teste usa `.with(csrf())`. Nao desabilite CSRF para testar.
- Tempo controlado por `Clock` injetado; nunca `Thread.sleep`.
- Teste nao acessa rede nem disco fora de `java.io.tmpdir`.

---

## 10. Configuracao e ambientes

- Propriedades da aplicacao sao tipadas em `config/AppProperties.java` (`@ConfigurationProperties("app")`, `@Validated`). Nunca `@Value` espalhado.
- Perfis: `dev` (padrao; H2 em arquivo em `./data`, console H2, Swagger), `mysql` (MySQL + Docker Compose), `test` (so na suite). Swagger e console H2 ficam desligados fora de `dev`.
- Toda variavel nova entra em `.env.example` com descricao e forma de gerar.
- `./data/` e `.env` sao locais e ignorados pelo git.

---

## 11. Convencoes

### Idioma

Tudo que pertence a codebase e escrito em ingles: pacote, classe, metodo, variavel, coluna, tabela, chave JSON, caminho de endpoint, comentario e mensagem de commit. Tudo que o usuario final ve e escrito em portugues: `detail` de `ProblemDetail`, mensagens de validacao, textos de seed.

### Nomenclatura

| Item | Padrao | Exemplo |
|---|---|---|
| Pacote | minusculo | `service`, `security` |
| Classe, record, enum | PascalCase | `PropertyService`, `LoginRequest` |
| Metodo, variavel | camelCase | `listPublished`, `tokenTtl` |
| Constante | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS` |
| Tabela e coluna | snake_case | `property_photo`, `created_at` |
| Endpoint | kebab-case, plural | `/api/admin/properties/{id}/status` |
| Migration | `V<n>__<snake_case>.sql` | `V2__add_property_owner.sql` |
| Variavel de ambiente | `APP_` + UPPER_SNAKE | `APP_JWT_SECRET` |

### Commits

Conventional Commits: `feat:`, `fix:`, `refactor:`, `perf:`, `test:`, `chore:`, `docs:`. Mensagem em ingles, no imperativo, sem emoji, sem ponto final. Correcao de vulnerabilidade usa `fix:` e descreve o impacto sem detalhar a exploracao.

**Sem trailer de coautoria.** Commit nao leva `Co-Authored-By`, "Generated with" nem qualquer assinatura de ferramenta ou agente. A mensagem termina no ultimo paragrafo descritivo.

---

## 12. Checklist antes de entregar

- [ ] Todo metodo tem modificador de acesso e tipo de retorno explicitos; dependencias por construtor.
- [ ] Nenhuma entidade sai de `service`; controller so fala `dto`.
- [ ] Schema alterado somente por nova migration Flyway; `ddl-auto` continua `validate`.
- [ ] Endpoint novo tem regra explicita em `SecurityConfig` e teste de acesso.
- [ ] Nenhum segredo em yaml versionado, codigo ou log; variavel nova documentada em `.env.example`.
- [ ] Erros passam pelo `ApiExceptionHandler`; nenhuma mensagem tecnica vaza.
- [ ] Nenhum `Instant.now()` fora do bean `Clock`; nenhum `Thread.sleep` em teste.
- [ ] `./mvnw verify` verde, sem teste desabilitado.
- [ ] Nenhum identificador em portugues; nenhuma mensagem de usuario em ingles.
- [ ] Nenhum emoji em codigo, commit, log ou resposta.
