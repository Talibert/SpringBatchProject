# Project Guidelines & Architecture Rules (`AGENTS.md`)

Este documento define as diretrizes de código, convenções de nomenclatura, arquitetura e testes obrigatórias para qualquer agente de IA ou desenvolvedor trabalhando neste repositório.

---

## 1. Estilo de Código e Convenções Java (Java 21)

- **Proibido o uso de `var`:** Nunca utilize inferência de tipos com a palavra-chave `var` (nem no código de produção em `src/main`, nem nos testes em `src/test`). Sempre declare explicitamente o tipo da variável.
- **Injeção de Dependências por Construtor:** Nunca utilize injeção por campo (`@Autowired` ou `@Value` diretamente em atributos privados). Toda injeção deve ocorrer exclusivamente via construtor.
- **Records Idiomáticos:** Ao criar `record` para DTOs ou Payloads de transporte, mantenha-os enxutos, sem construtores compactos defensivos desnecessários, salvo em *Value Objects* do domínio que exijam invariantes obrigatórias (ex: `UserId`, `ExchangeRateId`).
- **Idioma do Domínio e Persistência:** Nomes de classes, métodos, atributos e tabelas do domínio e da persistência devem ser padronizados em **inglês** (ex: `User`, `ExchangeRate`, `Currency`, `ExchangeRateRepository`), enquanto mensagens de validação, logs e descrições de testes (`@DisplayName`) permanecem em **português**.

---

## 2. Clean Architecture & DDD (Validado via ArchUnit)

O projeto segue uma separação estrita em 3 camadas sob `com.example.api_docker`:

1. **`domain` (`..domain..`)**:
   - **Pureza absoluta:** Não pode depender de `application` nem de `infra`.
   - **Zero dependências de Framework:** Proibido importar pacotes do Spring (`org.springframework..`), Jakarta Persistence (`jakarta.persistence..`) ou Jackson (`com.fasterxml.jackson..`).
   - **Entidades Ricas:** Criação de novas instâncias deve passar por métodos fábrica estáticos (`create(...)`) que validam invariantes de negócio e lançam `DomainException`, e reidratação do banco via `restore(...)`.
   - **Portas de Repositório:** Interfaces de repositório residem no domínio (`*Repository`) e nunca expõem entidades JPA.

2. **`application` (`..application..`)**:
   - Orquestra casos de uso (`*UseCase` em `..application..usecase..`).
   - Depende apenas de `domain`. Nunca deve depender de `infra` ou de `org.springframework.web..`.

3. **`infra` (`..infra..`)**:
   - Implementa adaptadores externos: Controllers REST (`..infra.controller..`), Persistência JPA (`..infra.persistence..`), Mensageria Kafka (`..infra.kafka..`), Clientes HTTP (`..infra.http..`) e Jobs do Spring Batch (`..infra.batch..`).
   - A conversão de objetos externos para o domínio deve sempre gerar a **entidade de domínio** (ex: `payload.toDomain()`), nunca convertendo direto de um DTO externo para uma entidade JPA (`*JpaEntity`).

---

## 3. Convenção de Sufixos de Objetos de Dados

Para evitar ambiguidade entre fronteiras arquiteturais, respeite rigorosamente os sufixos:

| Sufixo | Camada / Pacote | Finalidade |
| :--- | :--- | :--- |
| **`*Request`** | `infra.controller.*.request` | Dados recebidos nos endpoints REST da nossa API. |
| **`*Response`** | `infra.controller.*.response` | Dados devolvidos pelos endpoints REST da nossa API. |
| **`*Command` / `*Query`** | `application.*.command` / `query` | Entradas dos UseCases na camada de aplicação. |
| **`*Result`** | `application.*.result` | Saídas retornadas pelos UseCases da camada de aplicação. |
| **`*Payload`** | `infra.**.dto` | Contratos técnicos de dados recebidos/enviados para **APIs externas** (ex: `BcbCotacaoDolarPayload`). Deve conter o método `.toDomain()` quando aplicável. |
| **`*JpaEntity`** | `infra.persistence.*` | Mapeamento relacional de tabelas do banco de dados (`@Entity`). |

---

## 4. Clientes HTTP e Downloads (`HttpDownloadClient`)

- O componente [`HttpDownloadClient`](src/main/java/com/example/api_docker/infra/http/HttpDownloadClient.java) tem responsabilidade única de **transporte HTTP bruto** (`downloadAsString`, `downloadAsBytes`).
- Ele **não** deve conhecer DTOs de negócio, nem ser parametrizado com Generics de domínio.
- Clientes específicos de APIs externas (ex: `BcbCotacaoDolarClient`) injetam o `HttpDownloadClient`, baixam o conteúdo bruto e fazem o parse/extração usando `ObjectMapper`.

---

## 5. Configuração de Jobs e Steps do Spring Batch (`infra.batch`)

- **Steps (`Tasklet`) em `infra`:** As classes de Step que implementam `Tasklet` (ex: `UsdCollectorStep`) pertencem à camada `infra.batch.job.*.step`, são anotadas com `@Component` e atuam como *Driving Adapters*: extraem os `JobParameters` e delegam a regra de negócio/persistência para um `*UseCase` da camada de `application`.
- **Evitar `@Bean` para objetos `Step`:** Nas classes `*JobConfig`, exponha apenas o `Job` como `@Bean(name = ...)`. A construção do objeto `Step` (`new StepBuilder(...)`) deve ser feita em um método privado auxiliar (`buildStep(...)`) dentro do próprio `*JobConfig`, evitando colisão de nomes de beans com o `@Component` do `Tasklet` e ambiguidade de injeção entre múltiplos steps.

---

## 6. Endpoint Unificado de Consulta de Recursos Coletados (`infra.controller.query`)

- **Proibido criar um Controller REST por recurso coletado:** Todas as consultas paginadas de dados coletados pelos jobs passam exclusivamente pelo [`ResourceQueryController`](src/main/java/com/example/api_docker/infra/controller/query/ResourceQueryController.java) (`GET /query/{resource}`).
- **Padrão Strategy via `ResourceQueryProvider<T>`:**
  - Cada recurso/tabela de negócio coletada (ex: `exchange-rates`) deve ter sua própria implementação de [`ResourceQueryProvider<T>`](src/main/java/com/example/api_docker/infra/controller/query/provider/ResourceQueryProvider.java) anotada com `@Component` em `infra.controller.query.provider`.
  - O método `resourceName()` define o identificador na URL em *kebab-case* e no plural (ex: `"exchange-rates"`).
  - O provider converte o [`ResourceQueryFilterRequest`](src/main/java/com/example/api_docker/infra/controller/query/request/ResourceQueryFilterRequest.java) em uma `*Query` da camada `application`, aciona o `*UseCase` correspondente e mapeia o `PageResult<*Result>` para `PageResponse<*Response>`, garantindo um contrato JSON limpo e fortemente tipável por `interface` no frontend (Vue).

---

## 7. Diretrizes de Testes e CI/CD

- **Testes de Arquitetura:** Sempre garanta que `CleanArchitectureTest` passe após criar novas classes.
- **Testes de Integração do Spring Batch:** Testes que executam jobs reais devem herdar de [`BatchIntegrationAbstractTests`](src/test/java/com/example/api_docker/BatchIntegrationAbstractTests.java) e registrar novos repositórios JPA no método `@AfterEach cleanUpDatabase()` caso novas tabelas de negócio sejam criadas.
- **Migrations Flyway:** Ao adicionar uma nova migration em `src/main/resources/db/migration/V*__*.sql`, atualize o `FlywayMigrationTest` para refletir a nova versão atual e validar a tabela criada.
- **Testes de Integração com APIs Externas Reais:**
  - Devem ficar no **mesmo pacote** da classe testada (preservando visibilidade de pacote).
  - Devem obrigatoriamente ser anotados com `@Tag("external")`.
  - Na execução padrão (`./mvnw test`), o Maven Surefire ignora a tag `external` para não acoplar a pipeline de CI/CD à disponibilidade de serviços externos.
  - Para executar um teste externo manualmente:
    ```bash
    ./mvnw test -Dtest=NomeDoTeste -Dtest.excluded.groups=none
    ```
