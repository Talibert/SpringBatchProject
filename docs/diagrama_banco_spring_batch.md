# 📊 Modelo de Dados do Spring Batch (Metadados da Migration V2)

Este documento descreve a modelagem relacional de tabelas de metadados do **Spring Batch 5**, provisionada no banco de dados através da migration `V2__create_spring_batch_tables.sql`.

---

## 🧭 Diagrama Entidade-Relacionamento (ER)

```mermaid
erDiagram
    BATCH_JOB_INSTANCE ||--o{ BATCH_JOB_EXECUTION : "1..N (executado em)"
    BATCH_JOB_EXECUTION ||--o{ BATCH_JOB_EXECUTION_PARAMS : "1..N (parametrizado por)"
    BATCH_JOB_EXECUTION ||--o{ BATCH_STEP_EXECUTION : "1..N (composto de steps)"
    BATCH_JOB_EXECUTION ||--|| BATCH_JOB_EXECUTION_CONTEXT : "1..1 (contexto global)"
    BATCH_STEP_EXECUTION ||--|| BATCH_STEP_EXECUTION_CONTEXT : "1..1 (checkpoint do step)"

    BATCH_JOB_INSTANCE {
        bigint JOB_INSTANCE_ID PK "Identificador único gerado por BATCH_JOB_SEQ"
        bigint VERSION "Controle de concorrência otimista"
        varchar JOB_NAME "Nome lógico do Job (ex: collectorJob, calculatorJob)"
        varchar JOB_KEY "Hash identificador único dos parâmetros do Job"
    }

    BATCH_JOB_EXECUTION {
        bigint JOB_EXECUTION_ID PK "Identificador único da tentativa física (BATCH_JOB_EXECUTION_SEQ)"
        bigint VERSION "Controle de concorrência otimista"
        bigint JOB_INSTANCE_ID FK "Referência à instância lógica do Job"
        timestamp CREATE_TIME "Data/hora de criação do registro"
        timestamp START_TIME "Data/hora em que a execução iniciou"
        timestamp END_TIME "Data/hora em que a execução encerrou"
        varchar STATUS "Status do Batch (STARTING, STARTED, COMPLETED, FAILED, STOPPED)"
        varchar EXIT_CODE "Código de saída (COMPLETED, FAILED, NOOP)"
        varchar EXIT_MESSAGE "Stack trace ou mensagem detalhada em caso de erro"
        timestamp LAST_UPDATED "Última atualização do registro"
    }

    BATCH_JOB_EXECUTION_PARAMS {
        bigint JOB_EXECUTION_ID FK "Vínculo com a execução específica"
        varchar PARAMETER_NAME "Nome do parâmetro (ex: run.id, dataHoraDisparo)"
        varchar PARAMETER_TYPE "Tipo do parâmetro (java.lang.String, java.lang.Long, etc.)"
        varchar PARAMETER_VALUE "Valor persistido do parâmetro"
        char IDENTIFYING "Indica se o parâmetro diferencia a JobInstance (Y/N)"
    }

    BATCH_STEP_EXECUTION {
        bigint STEP_EXECUTION_ID PK "Identificador único do step (BATCH_STEP_EXECUTION_SEQ)"
        bigint VERSION "Controle de concorrência otimista"
        varchar STEP_NAME "Nome do step (ex: collectorStep, calculatorStep)"
        bigint JOB_EXECUTION_ID FK "Vínculo com a JobExecution mãe"
        timestamp START_TIME "Início do step"
        timestamp END_TIME "Fim do step"
        varchar STATUS "Status de execução do step"
        bigint COMMIT_COUNT "Número de transações comitadas"
        bigint READ_COUNT "Total de itens lidos pelo ItemReader"
        bigint FILTER_COUNT "Itens ignorados pelo ItemProcessor"
        bigint WRITE_COUNT "Itens persistidos pelo ItemWriter"
        bigint READ_SKIP_COUNT "Itens pulados na leitura por erro"
        bigint WRITE_SKIP_COUNT "Itens pulados na escrita por erro"
        bigint PROCESS_SKIP_COUNT "Itens pulados no processamento por erro"
        bigint ROLLBACK_COUNT "Transações abortadas (rollback)"
        varchar EXIT_CODE "Código de saída do step"
        varchar EXIT_MESSAGE "Mensagem ou stack trace do step"
        timestamp LAST_UPDATED "Última atualização"
    }

    BATCH_STEP_EXECUTION_CONTEXT {
        bigint STEP_EXECUTION_ID PK_FK "Referência ao BATCH_STEP_EXECUTION"
        varchar SHORT_CONTEXT "Resumo serializado do contexto de execução"
        text SERIALIZED_CONTEXT "Contexto serializado completo (checkpoint para restart)"
    }

    BATCH_JOB_EXECUTION_CONTEXT {
        bigint JOB_EXECUTION_ID PK_FK "Referência ao BATCH_JOB_EXECUTION"
        varchar SHORT_CONTEXT "Resumo serializado do contexto global do Job"
        text SERIALIZED_CONTEXT "Dados persistidos compartilhados entre steps"
    }
```

---

## 🔑 Relações e Conceitos Fundamentais

### 1. `BATCH_JOB_INSTANCE` vs `BATCH_JOB_EXECUTION` (1 para N)
* **`BATCH_JOB_INSTANCE` (Instância Lógica):** Representa o conceito do Job combinado com seus parâmetros identificadores (`IDENTIFYING = 'Y'`).
  * Exemplo: Um job `collectorJob` com parâmetro `mesAno=10-2026` é **uma única instância**.
* **`BATCH_JOB_EXECUTION` (Execução Física):** Representa uma tentativa real de executar aquela instância.
  * Se a execução falhar no meio do caminho, uma nova tentativa da mesma `JobInstance` criará uma segunda `JobExecution`. Se concluir com `COMPLETED`, o Spring Batch impede novas execuções da mesma instância (a menos que receba parâmetros diferentes ou seja configurado como restartable).
  * Por isso, o disparador [`BatchJobLauncherService`](../src/main/java/com/example/api_docker/infra/batch/service/BatchJobLauncherService.java) gera o parâmetro `run.id` com o timestamp atual a cada disparo manual.

---

### 2. `BATCH_JOB_EXECUTION_PARAMS` (1 para N)
* Armazena os parâmetros passados para o job (ex: filtros, data de corte, lote, identificadores externos).
* A coluna `IDENTIFYING` (`Y`/`N`) é o que diz ao Spring Batch se aquele parâmetro deve gerar uma nova `JobInstance` ou apenas acompanhar a execução como metadado.

---

### 3. `BATCH_STEP_EXECUTION` (1 para N com `BATCH_JOB_EXECUTION`)
* Um Job é composto por um ou mais Steps.
* Cada passo executado gera um registro detalhado de métricas:
  * Quantos registros foram lidos (`READ_COUNT`).
  * Quantos foram filtrados (`FILTER_COUNT`).
  * Quantos foram gravados (`WRITE_COUNT`).
  * Quantos commits ocorreram (`COMMIT_COUNT`).
  * Quantos erros tolerados foram pulados (`SKIP_COUNT`).

---

### 4. `BATCH_STEP_EXECUTION_CONTEXT` e `BATCH_JOB_EXECUTION_CONTEXT` (1 para 1)
* Armazenam o estado serializado em formato chave-valor (JSON/Texto).
* **Capacidade de Retomada (Restartability):** Se um job que processa 1 milhão de registros falhar no registro 500.000, o cursor e a paginação podem ser salvos no `ExecutionContext`. Ao reiniciar o job, ele recomeça do ponto em que parou sem reprocessar do zero.

---

## 🔢 Sequences Auxiliares
Três sequences foram criadas para geração atômica e segura de chaves primárias (compatíveis com PostgreSQL e H2):
1. **`BATCH_JOB_SEQ`**: Gera IDs para `BATCH_JOB_INSTANCE.JOB_INSTANCE_ID`.
2. **`BATCH_JOB_EXECUTION_SEQ`**: Gera IDs para `BATCH_JOB_EXECUTION.JOB_EXECUTION_ID`.
3. **`BATCH_STEP_EXECUTION_SEQ`**: Gera IDs para `BATCH_STEP_EXECUTION.STEP_EXECUTION_ID`.
