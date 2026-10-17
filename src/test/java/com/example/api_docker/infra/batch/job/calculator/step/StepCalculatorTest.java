package com.example.api_docker.infra.batch.job.calculator.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Testes Unitários do StepCalculator")
class StepCalculatorTest {

    private final StepCalculator stepCalculator = new StepCalculator();

    @Test
    @DisplayName("Deve executar o StepCalculator com sucesso utilizando MetaDataInstanceFactory")
    void shouldExecuteStepCalculatorSuccessfully() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dataReferencia", "2026-10-17")
                .addLong("run.id", 999999L)
                .toJobParameters();

        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        RepeatStatus status = stepCalculator.execute(contribution, chunkContext);

        assertNotNull(status);
        assertEquals(RepeatStatus.FINISHED, status);
    }
}
