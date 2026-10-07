# Arquitetura

## Visao geral

O backend e uma API REST stateless em Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Jackson 3) sobre Java 21. Ele atende dois publicos com a mesma aplicacao:

- **Portal publico**: consulta de imoveis publicados, bairros, fotos e registro de contato. Sem autenticacao.
- **Backoffice**: gestao completa de imoveis, upload de fotos e leitura de contatos. Exige sessao autenticada com senha e TOTP.

Nao ha sessao de servlet (`JSESSIONID`). A sessao e uma linha em `user_session` referenciada por um token opaco no cookie `CFID` (`HttpOnly; Secure; SameSite=Strict`); o browser guarda e reenvia o cookie sozinho e o frontend nunca o toca. Qualquer no da API resolve a sessao pelo banco, entao a escala horizontal nao exige replicacao de sessao em memoria.

## Camadas

```
HTTP  ->  controller  ->  service  ->  repository  ->  banco
             |  dto         |  entity       |  JPA / Specification
             |              |  mapper
             v              v
        ApiExceptionHandler (ProblemDetail)
```

| Pacote | Responsabilidade | O que nunca faz |
|---|---|---|
| `controller` | Mapeia rotas, valida entrada com `@Valid`, define status HTTP e cabecalhos (cookies, `Location`) | Regra de negocio, acesso a repositorio |
| `service` | Regra de negocio e fronteira transacional (`@Transactional`) | Conhecer `HttpServletRequest`, devolver entidade |
| `repository` | Spring Data JPA, `Specification` para filtros dinamicos | Logica alem de consulta |
| `entity` | Entidades JPA e enums de dominio | Sair do backend |
| `dto` | Records de entrada/saida da API, com Bean Validation | Conter logica |
| `mapper` | Conversao entity <-> dto | Acesso a banco |
| `security` | Filter chain, filtro de sessao `CFID`, JWT do desafio MFA, cookies, TOTP, cifragem | Regra de negocio |
| `config` | `AppProperties` tipadas, `Clock`, conversores de enum, OpenAPI | |
| `exception` | Excecoes de dominio e o `@RestControllerAdvice` | |
| `validation` | Constraints customizadas (`@PhoneNumber`) | |

## Fluxo de uma requisicao administrativa

1. `CorsFilter` valida a origem (apenas as de `APP_CORS_ALLOWED_ORIGINS`).
2. `OriginVerificationFilter` exige, em metodos mutantes, um `Origin` (ou `Referer`) igual a origem da API ou a uma origem permitida; sem isso responde `403`.
3. `SessionCookieAuthenticationFilter` le o cookie `CFID`, calcula o SHA-256 e busca a sessao ativa (`SessionService.authenticate`: nao revogada, dentro do limite absoluto e da janela de inatividade). Encontrando, desliza `last_seen_at` e autentica com `ROLE_<papel>`; nao encontrando, expira o cookie na resposta.
4. `AuthorizationFilter` aplica as regras de `SecurityConfig`: `/api/admin/**` exige `ROLE_ADMIN`; o resto e explicitamente permitido ou negado (`anyRequest().denyAll()`).
5. O controller valida o DTO e chama o servico.
6. O servico abre a transacao, aplica a regra, usa o mapper e devolve um DTO.
7. Qualquer excecao vira `ProblemDetail` no `ApiExceptionHandler`; falhas de autenticacao e autorizacao sao encaminhadas ao mesmo handler por `ProblemAuthenticationEntryPoint`, para que todos os erros tenham o mesmo formato.

## Tempo

A hora atual vem sempre do bean `Clock` (`TimeConfig`). Servicos, geracao de token, TOTP e limitador de tentativas recebem o `Clock` por construtor. Isso permite testar expiracao, janela de codigo e bloqueio sem `Thread.sleep`.

## Enumeracoes

Os enums de dominio (`PropertyPurpose`, `PropertyType`, `PropertyStatus`) carregam um valor publico em kebab-case (`short-stay`) exposto por `@JsonValue`/`@JsonCreator` e persistido como `EnumType.STRING`. `WebConfig` registra conversores para que o mesmo valor funcione em query string. O frontend usa exatamente esses valores, entao nao existe tabela de traducao entre os dois lados.

## Inicializacao

Dois `ApplicationRunner` ordenados:

1. `AdminUserInitializer`: cria o primeiro administrador a partir de `APP_ADMIN_*` apenas quando a tabela de usuarios esta vazia. Sem as variaveis, loga um aviso e segue (a API sobe, mas o backoffice nao tem login).
2. `DevDataSeeder` (perfil `dev`): carrega seis imoveis e dois contatos de exemplo quando nao ha imoveis, e posiciona o contador de codigos depois dos codigos usados no seed.
