package com.example.api_docker.infra.batch.job.calculator.step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Step responsável por encapsular a lógica de processamento e cálculo de métricas.
 * <p>
 * Centraliza os algoritmos de agregação, cálculos matemáticos e consolidações analíticas
 * sobre os dados brutos ingeridos, desacoplando o código de negócio da classe de
 * configuração do Job.
 */
@Component
public class StepCalculator implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(StepCalculator.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Executando StepCalculator: iniciando consolidação analítica e cálculo de métricas...");

        // Ponto de extensão para regras de negócio de cálculo (chamada a UseCases, agregação de dados, etc.)

        log.info("StepCalculator finalizado com sucesso.");
        return RepeatStatus.FINISHED;
    }
}
