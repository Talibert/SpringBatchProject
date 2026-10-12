package com.example.api_docker.infra.batch.job.collector.step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Step responsável por encapsular a lógica de execução da coleta de dados.
 * <p>
 * Centraliza a inteligência de ingestão e extração de dados brutos de fontes externas
 * (APIs parceiras, arquivos, mensageria, etc.), desacoplando o código de negócio
 * da classe de configuração do Job.
 */
@Component
public class StepCollector implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(StepCollector.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executando StepCollector: iniciando processo de coleta e ingestão de dados brutos...");

        // Ponto de extensão para regras de negócio de coleta (chamada a UseCases, WebClients, etc.)

        log.info("StepCollector finalizado com sucesso.");
        return RepeatStatus.FINISHED;
    }
}
