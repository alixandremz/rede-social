# rede-social

API REST de **rede social** feita para **estudar Spring Boot e Spring Web**.
Foco deste projeto: **relacionamentos entre usuários** (seguir/deixar de seguir),
**curtidas**, **agregações** (`count`, `group by`) e a montagem de um **feed** por
subconsulta.

Tem quatro entidades: **Usuario**, **Post**, **Curtida** e **Seguidor**. Um usuário
escreve posts, pode seguir outros usuários e curtir posts.

---

## O que estou estudando aqui

| Área | Conceitos praticados |
|------|----------------------|
| Auto-relacionamento | seguir usuário ↔ usuário via entidade **Seguidor** (`@ManyToOne` para o mesmo tipo) |
| `@ManyToOne` | post → autor, curtida → post/usuario, seguidor → seguidor/seguido, tudo `LAZY` |
| `@OneToMany` | post → curtidas, com `cascade = ALL` e `orphanRemoval = true` |
| Restrições | `@UniqueConstraint` para impedir curtir/seguir duas vezes |
| Agregações | `count` por usuário/post e `group by ... order by count(...)` para os populares |
| Feed | `@Query` com **subconsulta** (`where autor.id in (select ... seguido ...)`) |
| N+1 | por que acontece, como enxergar no log SQL e como resolver com `@BatchSize` |
| Spring Web (REST) | `@RestController`, verbos, `@PathVariable`, `@RequestBody`, `@RequestParam`, `ResponseEntity`, códigos HTTP (200, 201, 204, 400, 404, 409) |
| Spring Data JPA | `JpaRepository`, query derivada (`existsBy`, `countBy`, `findBy...OrderBy...`) |
| Paginação | `Pageable`, `PageRequest`, `Sort`, `Page`, invólucro `PaginaResponse` |
| Bean Validation | `@NotBlank`, `@Size`, `@NotNull`, `@Valid`, `@Min`/`@Max` |
| Tratamento de erros | `@RestControllerAdvice`, `@ExceptionHandler`, `409` para violação de banco |

---

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Bean Validation
- H2 em memória (com SQL logado)
- Maven (via Maven Wrapper `./mvnw`, não precisa ter Maven instalado)

---

## Modelo de dados

```
Usuario ──(autora)──► Post
Post ──(um-para-muitos)──► Curtida ──(muitos-para-um)──► Usuario
Usuario ──(Seguidor)──► Usuario   (seguidor ─► seguido)
```

- **Usuario**: `id`, `nome` (único), `criadoEm`.
- **Post**: `id`, `autor`, `conteudo`, `criadoEm`, `curtidas`.
- **Curtida**: `id`, `post`, `usuario`, `criadoEm`. Único por (`post_id`, `usuario_id`).
- **Seguidor**: `id`, `seguidor`, `seguido`, `criadoEm`. Único por (`seguidor_id`, `seguido_id`).

`Seguidor` é uma **entidade** (e não um `@ManyToMany` com `Usuario`) para guardar a data em que
o "seguir" aconteceu e ficar simétrico às curtidas.

---

## Endpoints

### Usuários

| Método | Caminho | Descrição | Sucesso | Erros |
|--------|---------|-----------|---------|-------|
| GET | `/usuarios` | lista todos (ordem alfabética) | `200` | |
| GET | `/usuarios/{id}` | detalhe com contagens (seguidores, seguindo, posts) | `200` | `404` |
| POST | `/usuarios` | cria um usuário | `201` | `400`, `409` |
| POST | `/usuarios/{id}/seguindo/{alvoId}` | `id` passa a seguir `alvoId` | `204` | `404`, `409` |
| DELETE | `/usuarios/{id}/seguindo/{alvoId}` | `id` deixa de seguir `alvoId` | `204` | `404` |
| GET | `/usuarios/{id}/seguidores` | quem segue esse usuário | `200` | `404` |
| GET | `/usuarios/{id}/seguindo` | quem esse usuário segue | `200` | `404` |
| GET | `/usuarios/{id}/feed` | posts de quem ele segue (paginado) | `200` | `400`, `404` |

### Posts

| Método | Caminho | Descrição | Sucesso | Erros |
|--------|---------|-----------|---------|-------|
| GET | `/posts` | lista paginada (resumo) | `200` | `400` |
| GET | `/posts/populares` | os 10 posts mais curtidos | `200` | |
| GET | `/posts/{id}` | um post | `200` | `404` |
| POST | `/posts` | cria um post | `201` | `400`, `404` |
| POST | `/posts/{id}/curtidas` | curte um post | `204` | `404`, `409` |
| DELETE | `/posts/{id}/curtidas/{usuarioId}` | descurte | `204` | `404` |

### Criar usuário e post

```bash
curl -X POST http://localhost:8080/usuarios \
  -H 'Content-Type: application/json' -d '{"nome":"Ana"}'
# 201, Location: /usuarios/1

curl -X POST http://localhost:8080/posts \
  -H 'Content-Type: application/json' \
  -d '{"autorId":1,"conteudo":"meu primeiro post"}'
```

### Seguir, curtir e ver o feed

```bash
curl -i -X POST   http://localhost:8080/usuarios/1/seguindo/2   # 204
curl -i -X DELETE http://localhost:8080/usuarios/1/seguindo/2   # 204

curl -i -X POST http://localhost:8080/posts/1/curtidas \
  -H 'Content-Type: application/json' -d '{"usuarioId":2}'       # 204

curl http://localhost:8080/usuarios/1/feed
```

```json
{
  "conteudo": [
    { "id": 1, "autorId": 2, "autorNome": "Bruno",
      "conteudo": "post do bruno", "criadoEm": "...", "totalCurtidas": 1 }
  ],
  "pagina": 0, "tamanho": 10, "total": 1, "totalPaginas": 1
}
```

### Detalhe do usuário

```bash
curl http://localhost:8080/usuarios/1
```

```json
{ "id": 1, "nome": "Ana", "criadoEm": "...",
  "seguidores": 1, "seguindo": 2, "posts": 3 }
```

### Os mais curtidos

```bash
curl http://localhost:8080/posts/populares
```

---

## Decisões e detalhes técnicos

- **Restrições no banco, não só no código.** Curtir e seguir têm `@UniqueConstraint`, então
  mesmo se duas requisições passarem pela checagem juntas, o banco barra a duplicata — que vira
  `409` via `DataIntegrityViolationException`.
- **`cascade` + `orphanRemoval`.** O post é dono da coleção de curtidas: apagar o post apaga as
  curtidas junto.
- **`open-in-view=false`.** O lazy só é resolvido dentro da transação do service. Todo DTO é
  montado ali dentro, com segurança.
- **Feed por subconsulta.** `where p.autor.id in (select s.seguido.id from Seguidor s where
  s.seguidor.id = :usuarioId)`. O próprio autor não entra no próprio feed (só posts de quem ele
  segue).
- **Populares com `group by`.** `left join p.curtidas c group by p order by count(c) desc`,
  limitado a 10 com `PageRequest`. `left join` garante que post com **zero** curtidas também
  apareça no fim.
- **Contagens derivadas.** `seguidores`, `seguindo`, `posts` e `totalCurtidas` são calculados
  com `countBy...` no service, e não guardados em coluna — sem risco de dessincronizar.

---

## O problema N+1 (de novo)

`totalCurtidas` é `post.getCurtidas().size()`. Se cada post buscasse sua coleção de curtidas
sozinho, uma página de 10 posts faria 1 + 10 consultas. Por isso a coleção `curtidas` usa
`@BatchSize(size = 30)`: o Hibernate busca **lote em lote** (`where post_id in (...)`).

O SQL fica ligado (`spring.jpa.show-sql=true`) justamente para você contar as consultas. Apague
o `@BatchSize`, rode os testes e compare.

> Observação: neste Hibernate, `@BatchSize` **não** pode ser usado em `@ManyToOne` (só em
> coleções). É por isso que o autor não tem a anotação — ele é resolvido pelo mesmo lote do
> contexto de persistência.

---

## Como rodar

Usa H2 em memória; os dados somem ao parar a aplicação.

```bash
cd rede-social
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O SQL aparece no console. Console do H2 em
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:rede`, usuário `sa`, sem senha).

Também há um `Dockerfile` (multi-stage, usuário sem privilégios):

```bash
docker build -t rede-social .
docker run -p 8080:8080 rede-social
```

---

## Testes

```bash
./mvnw test
```

Cobrem:

- criação de usuário (`201`) e usuário inexistente (`404`);
- detalhe do usuário com contagens de seguidores/seguindo/posts;
- seguir a si mesmo → `409`; seguir duas vezes → `409`;
- deixar de seguir (`204`) e deixar de seguir quem não segue (`404`);
- curtir aumenta `totalCurtidas`; curtir duas vezes → `409`; descurtir zera;
- **feed** mostra apenas posts de quem o usuário segue;
- `/posts/populares` ordena por curtidas;
- listagem paginada de posts;
- post com autor inexistente → `404`; conteúdo em branco → `400`.

---

## Próximos passos (ideias para continuar estudando)

- Medir o N+1 com estatísticas do Hibernate e um teste que conta as queries.
- Feed com posts do próprio usuário **e** de quem ele segue, ordenado por data.
- Paginação de seguidores/seguindo (hoje retornam lista completa).
- Busca de usuários por nome e posts por texto.
- Trocar H2 por **PostgreSQL** + `docker-compose` e usar **Testcontainers** nos testes.
- Migrations com **Flyway**.
- **Spring Security**: identidade do usuário vinda do token, e não do corpo/URL.
