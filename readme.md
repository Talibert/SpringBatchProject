# 🚀 Spring Boot 3 Clean Architecture & Event-Driven Starter Kit

![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-black?style=flat-square&logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![ArchUnit](https://img.shields.io/badge/ArchUnit-1.3-yellow?style=flat-square)
![OpenAPI 3](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-green?style=flat-square&logo=swagger)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-red?style=flat-square&logo=flyway)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)

Template base pronto para produção voltado para criação rápida de novos microsserviços e APIs corporativas em Java 21 e Spring Boot 3. 

Projetado seguindo **Clean Architecture**, **Domain-Driven Design (DDD)**, **Event-Driven Architecture (EDA)** com **Apache Kafka**, versionamento de banco com **Flyway**, autenticação stateless com **JWT**, guardrails arquiteturais automatizados via **ArchUnit**, testes de integração com **EmbeddedKafka** e **Testcontainers**, e documentação interativa com **OpenAPI 3 (Swagger)**.

---

## 🏛️ Princípios e Arquitetura

### 1. Separação Estrita de Camadas (Clean Architecture & DDD)

```
                       ┌─────────────────────────────────────┐
                       │          Infra (Adapters)           │
                       │  Controllers, JPA, Kafka, Security  │
                       └──────────────────┬──────────────────┘
                                          │ depende de
                                          ▼
                       ┌─────────────────────────────────────┐
                       │        Application (UseCases)       │
                       │     Commands, Queries, Results      │
                       └──────────────────┬──────────────────┘
                                          │ depende de
                                          ▼
                       ┌─────────────────────────────────────┐
                       │           Domain (Core)             │
                       │ Entities, Value Objects, Aggregates │
                       │    Domain Events, Repositories      │
                       └─────────────────────────────────────┘
```

- **`domain`**: Núcleo isolado da aplicação. Contém entidades ricas, Agregados, Value Objects imutáveis e contratos de repositórios (interfaces). **Não possui dependência de frameworks** (zero anotações Spring, zero JPA, zero libs HTTP).
- **`application`**: Casos de uso de negócio (`CreateUserUseCase`, `ChangeUserPasswordUseCase`, `LoginUseCase`). Orquestra a execução, controla transações de aplicação e gerencia fluxos via DTOs dedicados (`Command`, `Query`, `Result`).
- **`infra`**: Camada de entrega e detalhes tecnológicos:
  - `controller`: Endpoints REST, mapeamento de requisições (`toCommand()`) e respostas (`from(result)`).
  - `persistence`: Entidades JPA, repositórios Spring Data e implementações dos contratos de repositório do domínio.
  - `security`: Filtro JWT, validação de tokens e hashing seguro com BCrypt.
  - `kafka`: Publicação de eventos de domínio (`KafkaDomainEventPublisher`) e listeners assíncronos (`UserEventsConsumer`).
  - `config`: Beans de configuração (OpenAPI, Spring Security, Seeds de inicialização).
  - `exception`: Tratamento global de exceções centralizado (`GlobalExceptionHandler`).

---

### 2. Fluxo Orientado a Eventos (Event-Driven com Kafka)

O projeto adota o padrão **Pull Model** para eventos de domínio em Aggregates:
1. **Mutação no Agregado:** Quando um estado de negócio relevante é alterado (ex: criação de usuário ou alteração de senha), a entidade de domínio registra internamente o evento correspondente (`user.registerEvent(new UserCreatedEvent(...))`).
2. **Drenagem pelo Caso de Uso:** O Use Case persiste o agregado e extrai a lista de eventos pendentes (`user.pullEvents()`).
3. **Publicação Desacoplada:** O caso de uso delega a publicação para a interface `DomainEventPublisher`.
4. **Envio ao Kafka:** A implementação de infraestrutura `KafkaDomainEventPublisher` identifica o tópico correto através do `KafkaTopicRegistry` e publica o evento serializado em JSON.
5. **Consumo Assíncrono:** Consumidores Kafka (`@KafkaListener`) processam os eventos assincronamente em seus respectivos tópicos.

---

### 3. Guardrails Automatizados de Arquitetura (ArchUnit)

O projeto inclui uma suíte automatizada de testes arquiteturais ([`CleanArchitectureTest`](file:///src/test/java/com/example/api_docker/architecture/CleanArchitectureTest.java)) que roda em **~0.5s** durante o `mvn test`:

| Regra | Objetivo |
|---|---|
| **Pureza do Domínio** | Impede qualquer import de `application`, `infra`, `org.springframework..` ou `jakarta.persistence..` no pacote `domain`. |
| **Isolamento da Aplicação** | Impede que a camada `application` importe pacotes de `infra` ou do Spring Web (`org.springframework.web..`). |
| **Injeção Segura** | Proíbe injeção direta em campos (`@Autowired` ou `@Value` em private fields), forçando injeção por construtor. |
| **Inversão de Dependência (DIP)** | Garante que qualquer classe em `domain` que termine com `Repository` seja obrigatoriamente uma `interface`. |
| **Padronização de Pacotes** | Garante que classes terminadas em `Controller` estejam em `infra.controller` e `UseCase` em `application..usecase`. |

---

### 4. Paginação Desacoplada e Agnóstica a Framework

Em vez de vazar classes proprietárias do Spring Data (`Pageable`, `Page`) para o núcleo da aplicação, o projeto utiliza abstrações puras:
- **`PaginationRequest` (Domain):** Especificação pura de página (`page`), tamanho (`size`), ordenação (`sortBy`) e direção (`sortDirection`), com proteções embutidas contra valores negativos ou tamanhos abusivos (`size > 100`).
- **`PageResult<T>` (Domain):** Envelope genérico imutável contendo os itens e metadados (`page`, `size`, `totalElements`, `totalPages`, `isFirst`, `isLast`) com suporte a transformação funcional via `.map()`.
- **`PageResponse<T>` (Infra):** DTO padronizado de resposta REST para o frontend, documentado com esquemas OpenAPI.
- **`UserRepositoryImpl` (Infra):** Converte a abstração de domínio em `PageRequest.of(...)` do Spring Data JPA e mapeia o resultado de volta para `PageResult<User>`.

---

### 5. Versionamento de Schema com Flyway (Zero `ddl-auto=update` em Produção)

Em ambientes profissionais, o `ddl-auto=update` do Hibernate é perigoso (não gera histórico, não permite rollback e falha em alterações destrutivas ou renomeações). 

O projeto adota o **Flyway**:
- **Migrations SQL Imutáveis:** Armazenadas em `src/main/resources/db/migration/` no padrão `V<versao>__<descricao>.sql` (ex: `V1__create_users_table.sql`).
- **Validação Estrita com Hibernate (`ddl-auto=validate`):** O Hibernate nunca altera o banco; ele apenas valida se as entidades JPA (`@Entity`) estão 100% em sincronia com o schema gerado pelo Flyway.
- **Testes Automatizados de Migration ([`FlywayMigrationTest.java`](file:///src/test/java/com/example/api_docker/infra/persistence/FlywayMigrationTest.java)):** Valida a execução de todas as migrations, estado `SUCCESS`, integridade de tabelas, colunas e chaves primárias.

---

### 6. Ecossistema Spring Batch (Jobs Coletores e Calculadores)

O projeto foi preparado para orquestrar cargas de trabalho em lote (Batch Processing) de alta performance e resiliência, divididas em duas categorias principais de Jobs:

* **Jobs Coletores (`infra.batch.job.collector`):** Responsáveis pela ingestão e extração de dados brutos provenientes de fontes externas (APIs de terceiros, arquivos CSV/JSON, filas ou bancos legados), persistindo-os de maneira estruturada no banco para processamento posterior.
* **Jobs Calculadores (`infra.batch.job.calculator`):** Atuam sobre os dados previamente ingeridos para realizar agregações analíticas, consolidações matemáticas e cálculos periódicos (como indicadores de negócio, relatórios ou fechamentos).

Para entender a modelagem completa das tabelas do banco de dados geradas pelo Flyway, consulte [docs/diagrama_banco_spring_batch.md](docs/diagrama_banco_spring_batch.md).

#### 🔄 Ciclo de Vida e Cooperação entre as Classes

#### 🔄 Passo a Passo: Como as Classes Cooperam na Execução de um Job

A orquestração de qualquer job (seja coletor ou calculador) segue um fluxo desacoplado de 6 etapas, coordenado entre as classes criadas:

##### 1. Descoberta e Injeção Dinâmica dos Jobs (`Map<String, Job>`)
* **Como funciona:** O Spring Framework possui o recurso nativo de resolução de coleções. No [`BatchJobLauncherService`](src/main/java/com/example/api_docker/infra/batch/service/BatchJobLauncherService.java), o construtor declara:
  ```java
  public BatchJobLauncherService(JobLauncher jobLauncher, JobExplorer jobExplorer, Map<String, Job> jobsByName)
  ```
* O Spring escaneia todo o contexto da aplicação, localiza todos os `@Bean` que retornam instâncias de `Job` (definidos em classes como [`CollectorJobConfig`](src/main/java/com/example/api_docker/infra/batch/job/collector/CollectorJobConfig.java) e [`CalculatorJobConfig`](src/main/java/com/example/api_docker/infra/batch/job/calculator/CalculatorJobConfig.java)) e injeta automaticamente esse mapa, onde a chave é o nome do bean (`"collectorJob"`, `"calculatorJob"`, etc.) e o valor é o próprio objeto do Job.
* **Benefício:** Para adicionar um novo job coletor ou calculador no projeto, **não é necessário tocar no serviço de disparo**. Ele descobre o novo job sozinho pelo nome.

##### 2. Disparo e Prevenção de Bloqueio (`run.id`)
* **Origem:** O disparo pode vir de um endpoint REST autenticado, de um agendador (`@Scheduled`), de um consumidor Kafka ou de um comando CLI.
* O chamador executa `batchJobLauncherService.runJob("collectorJob", parametrosCustom)`.
* **Garantia de Execução Única:** No Spring Batch, uma mesma instância lógica de job (`JobInstance`) com os mesmos parâmetros identificadores só pode ser executada uma única vez se terminar com sucesso (`COMPLETED`). Se tentarmos rodar novamente com os mesmos parâmetros, o framework impede a execução.
* Para contornar isso e permitir que jobs sejam disparados sempre que requisitados, o `BatchJobLauncherService` injeta automaticamente dois parâmetros a cada chamada:
  * `run.id`: timestamp em milissegundos (`System.currentTimeMillis()`).
  * `dataHoraDisparo`: timestamp ISO UTC (`Instant.now()`).
* Em seguida, o serviço delega a execução para o `JobLauncher.run(job, parameters)`.

##### 3. Criação de Metadados pelo `JobRepository`
* Antes de executar qualquer lógica de negócio, o `JobLauncher` aciona o `JobRepository`.
* O `JobRepository` realiza operações atômicas no banco de dados (tabelas criadas pela migration `V2`):
  1. Cria ou recupera a instância lógica em `BATCH_JOB_INSTANCE`.
  2. Cria uma nova tentativa física de execução em `BATCH_JOB_EXECUTION` com status `STARTING`.
  3. Grava todos os parâmetros recebidos em `BATCH_JOB_EXECUTION_PARAMS`.

##### 4. Hook de Início do Listener (`beforeJob`)
* Como registramos `.listener(jobExecutionListener)` na construção dos jobs, o Spring Batch aciona o método `beforeJob()` do [`JobCompletionNotificationListener`](src/main/java/com/example/api_docker/infra/batch/listener/JobCompletionNotificationListener.java).
* O listener extrai os metadados da execução (`jobName`, `jobExecutionId`, parâmetros) e emite logs estruturados indicando o início da operação para auditoria e monitoramento.

##### 5. Execução dos Steps (Tasklets ou Chunks)
* O job inicia seus passos sequenciais (definidos via `.start(step)` ou `.next(step)`):
  * **No Job Coletor:** O step tipicamente conecta em fontes externas (APIs HTTP, arquivos CSV/S3, mensageria) e persiste os registros brutos em tabelas de staging no PostgreSQL.
  * **No Job Calculador:** O step lê os dados brutos com paginação (`ItemReader`), aplica regras matemáticas/agregações (`ItemProcessor`) e grava os resultados consolidados nas tabelas analíticas finais (`ItemWriter`).
* A cada chunk ou tasklet finalizado, o `JobRepository` atualiza as métricas em `BATCH_STEP_EXECUTION` (linhas lidas, gravadas, commits, rollbacks e eventuais skips de erro).
* Se ocorrer algum erro recuperável, o checkpoint é armazenado no `BATCH_STEP_EXECUTION_CONTEXT` permitindo retomada posterior (*restartability*).

##### 6. Hook de Término, Auditoria e Retorno (`afterJob`)
* Ao concluir todos os passos (seja com sucesso ou por falha), o `JobRepository` atualiza o status final da execução em `BATCH_JOB_EXECUTION` para `COMPLETED` ou `FAILED`.
* O Spring Batch aciona o método `afterJob()` do [`JobCompletionNotificationListener`](src/main/java/com/example/api_docker/infra/batch/listener/JobCompletionNotificationListener.java):
  * O listener calcula a duração exata subtraindo `endTime - startTime`.
  * Se `COMPLETED`: emite log com tempo total em milissegundos.
  * Se `FAILED`: emite log de erro e extrai a descrição da falha (`ExitDescription`) com a causa raiz.
* Por fim, o `JobLauncher` retorna o objeto `JobExecution` preenchido para o `BatchJobLauncherService`, que o devolve ao chamador com todos os identificadores e o status final.

---

## 📁 Estrutura de Diretórios

```
src/main/java/com/example/api_docker/
├── domain/
│   ├── shared/             # Classes base (AggregateRoot, DomainEvent, EntityId, ValueObject)
│   │   └── pagination/     # PaginationRequest e PageResult<T> (Abstrações puras)
│   └── user/               # Agregado User, Email, Password, Eventos e UserRepository
├── application/
│   ├── auth/               # Commands, Results e UseCase de Autenticação / Login
│   └── user/               # Commands, Queries (ListUsersQuery), Results e UseCases
└── infra/
    ├── batch/              # Ecossistema Spring Batch (Jobs Coletores, Calculadores, Listener, Service)
    ├── config/             # Configurações do Spring (OpenApiConfig, SecurityConfig, UserSeedConfig)
    ├── controller/         # Controllers REST, DTOs de entrada e PageResponse<T>
    ├── exception/          # GlobalExceptionHandler e ErrorResponse padronizado
    ├── kafka/              # Publisher, Topic Registry e Consumers Kafka
    ├── persistence/        # Entidades JPA e Repositórios Spring Data
    └── security/           # Filtro JWT, Token Generator e BCrypt

src/main/resources/
└── db/migration/           # Scripts SQL do Flyway (V1__users, V2__spring_batch_tables)
```

---

## ⚡ Como Executar o Projeto

### Pré-requisitos
- **Docker** e **Docker Compose**
- **Java 21** (necessário apenas para rodar localmente fora do container)

---

### Opção A: Desenvolvimento Local (API no Host / IDE + Infra no Docker)

Ideal para o dia a dia de desenvolvimento rápido com live reload e debugging no IntelliJ/VSCode:

1. **Crie a rede Docker compartilhada e suba a infraestrutura:**
   ```bash
   # Cria a rede compartilhada se ainda não existir
   docker network create api-network

   # Sobe o banco PostgreSQL e o Apache Kafka em modo KRaft (sem Zookeeper)
   docker-compose up -d
   ```

2. **Execute a aplicação Spring Boot:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *(Ou execute o método `main` da classe `ApiDockerApplication` diretamente pela sua IDE).*

---

### Opção B: Deploy e Containerização Completa (Production-Ready com `compose.deploy`)

Para rodar todo o ecossistema (Aplicação + Banco + Kafka) 100% conteinerizado de forma idêntica à produção:

O projeto utiliza uma estratégia de **Multi-stage Build** no [`Dockerfile`](file:///Dockerfile):
- **Estágio 1 (Build):** Imagem Maven com Java 21 compila o código e gera o `.jar`.
- **Estágio 2 (Runtime):** O `.jar` compilado é copiado para uma imagem enxuta baseada em `eclipse-temurin:21-jre`, garantindo performance, inicialização rápida e menor superfície de vulnerabilidades.

#### Passo a Passo para Subir o Ambiente Completo:

1. **Crie a rede compartilhada:**
   ```bash
   docker network create api-network
   ```

2. **Suba os serviços de infraestrutura (PostgreSQL e Kafka):**
   ```bash
   docker-compose up -d
   ```

3. **Compile e inicie o container da aplicação:**
   ```bash
   docker-compose -f docker-compose.deploy.yml up --build -d
   ```

#### Comandos Úteis do Container da Aplicação:

- **Acompanhar os logs da aplicação:**
  ```bash
  docker logs -f spring-app-course
  ```

- **Atualizar a aplicação após alterações no código:**
  ```bash
  # O parâmetro --build força uma nova compilação no container
  docker-compose -f docker-compose.deploy.yml up --build -d
  ```

- **Parar a aplicação e a infraestrutura:**
  ```bash
  # Para a aplicação
  docker-compose -f docker-compose.deploy.yml down

  # Para o banco e kafka
  docker-compose down
  ```

---

### 🔑 Credenciais Padrão (Seed Admin)
Ao inicializar a aplicação (seja localmente ou via container), um administrador é provisionado automaticamente:
- **Email:** `admin@course.com`
- **Senha padrão:** `MaluZoe` *(customizável via variável de ambiente `ADMIN_SEED_PASSWORD`)*
- **Health Check & Probes (Actuator):** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 🗄️ Gerenciamento de Banco de Dados: Flyway & Utilitário DBInstall

O projeto utiliza **Flyway** para controle de versão e migrações do banco de dados PostgreSQL, em conjunto com `spring.jpa.hibernate.ddl-auto=validate`. Isso assegura que o Hibernate **nunca altere o schema em tempo de execução**, garantindo estabilidade absoluta e rastreabilidade total de mudanças.

### ⚡ Utilitário `DBInstall` (Gerador de DDL para Migrations)

Ao criar uma nova entidade JPA ou novos relacionamentos no projeto (como `@ManyToOne`, `@OneToMany`, Foreign Keys, Chaves Primárias ou Constraints), você **não precisa escrever o script DDL SQL do zero manualmente**.

A classe [`DBInstall`](file:///Users/taliberti/Development/Personal/BaseProject/src/main/java/com/example/api_docker/infra/tools/DBInstall.java) (`infra.tools.DBInstall`) foi criada especificamente para isso:
- 🔍 **Escaneamento Automático:** Varre o classpath em busca de todas as classes anotadas com `@Entity`.
- 🐘 **Dialeto PostgreSQL:** Utiliza a engine de DDL do Hibernate 6 configurada com `PostgreSQLDialect` e convenção `snake_case` (`CamelCaseToUnderscoresNamingStrategy`).
- 🔗 **Geração Completa:** Gera instruções DDL para `CREATE TABLE`, `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE CONSTRAINT`, `INDEX` e `SEQUENCE`.
- 🔌 **Execução 100% Offline:** Não requer conexão com banco de dados ativa para gerar o script.

#### Como Executar o `DBInstall`:

1. **Pela sua IDE (IntelliJ IDEA, Eclipse, VSCode):**
   - Navegue até `src/main/java/com/example/api_docker/infra/tools/DBInstall.java`.
   - Clique com o botão direito e selecione **Run 'DBInstall.main()'** (ou clique no ícone de Play verde ao lado de `main`).

2. **Via Linha de Comando (Terminal):**
   ```bash
   ./mvnw compile exec:java -Dexec.mainClass="com.example.api_docker.infra.tools.DBInstall"
   ```

#### Exemplo de Saída no Console:
```sql
================================================================================
 🚀 DBInstall - Gerador de DDL SQL para Migrations (Dialeto: PostgreSQL)
================================================================================

🔍 Entidades encontradas (1):
   - com.example.api_docker.infra.persistence.user.UserJpaEntity

--------------------------------------------------------------------------------
--- INÍCIO DO DDL GERADO (Copie para sua migration Flyway) ---
--------------------------------------------------------------------------------

create table users (
    user_id uuid not null,
    created_at timestamp(6) not null,
    email varchar(255) not null unique,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    password_hash varchar(255) not null,
    primary key (user_id)
);

--------------------------------------------------------------------------------
--- FIM DO DDL GERADO ---
--------------------------------------------------------------------------------
```

#### Passo a Passo para Criar uma Nova Migration:
1. Crie ou altere a entidade JPA em `infra/persistence/`.
2. Execute o `DBInstall`.
3. Copie o SQL gerado para o novo arquivo em:
   `src/main/resources/db/migration/V<numero>__<descricao>.sql` (ex: `V2__create_orders_table.sql`).
4. Execute `./mvnw test` para validar a migration e a conformidade do schema!

---

## 📖 Documentação Interativa & Testes (OpenAPI 3 / Swagger)

A API possui documentação interativa gerada automaticamente com suporte a autenticação via **Bearer Token (JWT)**:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Como testar endpoints protegidos no Swagger UI:
1. Acesse o Swagger UI no navegador.
2. Abra a seção **Autenticação** e execute o endpoint `POST /auth/login` com as credenciais do admin (`admin@course.com` / `MaluZoe`).
3. Copie o valor do campo `token` retornado no corpo da resposta.
4. No topo da página do Swagger, clique no botão verde **Authorize** (com ícone de cadeado).
5. Cole o token no campo de valor e confirme.
6. Todos os endpoints protegidos (`GET /user`, `GET /user/me`, `POST /user/register`, `PATCH /user/password`) agora podem ser executados diretamente pelo navegador!

### 🚀 Importação no Bruno ou Postman:
Você pode importar todas as rotas e tipos diretamente no **Bruno** ou **Postman**:
1. No seu client HTTP, selecione a opção **Import**.
2. Escolha importar via URL OpenAPI / Swagger.
3. Insira a URL: `http://localhost:8080/v3/api-docs`.

---

## 🧪 Estratégia e Execução de Testes

O projeto conta com uma pirâmide completa de testes automatizados:
1. **Testes de Arquitetura:** Validação contínua com ArchUnit (10 guardrails).
2. **Testes de Migrations (Flyway):** Validação de histórico, estado `SUCCESS` e conformidade DDL das tabelas e colunas.
3. **Testes Unitários:** Testes de domínio, Value Objects, paginação e Use Cases isolados com Mockito.
4. **Testes de Controller:** Testes de camada web com `MockMvc` e validações de DTO.
5. **Testes de Integração:** Testes de persistência e validação de schema Hibernate (`ddl-auto=validate`).
6. **Testes End-to-End (E2E) com Kafka:** Fluxo completo via `@EmbeddedKafka` validando: chamada HTTP -> Controller -> Banco de Dados -> Disparo de Evento -> Consumo pelo Listener Kafka.

Para executar todos os testes da aplicação:
```bash
./mvnw test
```

---

## 🛠️ Como Usar este Repositório como Novo Projeto (Starter Kit)

Para iniciar um novo projeto a partir deste repositório:

1. **Clonar ou Criar Repositório a partir deste:**
   ```bash
   git clone <URL_DESTE_REPOSITORIO> meu-novo-microsservico
   cd meu-novo-microsservico
   rm -rf .git
   git init
   ```
2. **Atualizar Metadados do Projeto no `pom.xml`:**
   - Altere `<groupId>`, `<artifactId>`, `<name>` e `<description>`.
3. **Ajustar Nome da Aplicação:**
   - Em `src/main/resources/application.properties`, altere `spring.application.name`.
4. **Modelar seu Novo Domínio:**
   - Crie seu agregado dentro de `domain/<novo-dominio>/` estendendo `AggregateRoot`.
   - Defina Value Objects e eventos de domínio.
   - Crie os UseCases em `application/<novo-dominio>/usecase/`.
   - Adicione os controllers e DTOs em `infra/controller/<novo-dominio>/`.
   - Registre os novos tópicos Kafka em `KafkaTopicRegistry.java` e `EventType.java`.
5. **Validar:**
   - Rode `./mvnw test` e tenha certeza de que todas as regras do ArchUnit continuam respeitadas!

---

## 📄 Licença
Distribuído sob a licença MIT. Consulte `LICENSE` para obter mais informações.