package com.example.api_docker.infra.batch.job.collector.step;

import com.example.api_docker.application.exchangerate.command.CollectExchangeRatesCommand;
import com.example.api_docker.application.exchangerate.result.CollectExchangeRatesResult;
import com.example.api_docker.application.exchangerate.usecase.CollectExchangeRatesUseCase;
import com.example.api_docker.domain.exchangerate.Currency;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsdCollectorStepTest {

    private final CollectExchangeRatesUseCase useCaseMock = mock(CollectExchangeRatesUseCase.class);
    private final UsdCollectorStep usdCollectorStep = new UsdCollectorStep(useCaseMock);

    @Test
    @DisplayName("Deve coletar apenas um único dia quando apenas uma data (dataReferencia) for informada")
    void shouldCollectSingleDayWhenOnlyOneDateIsProvided() {
        LocalDate singleDate = LocalDate.of(2026, 10, 13);
        when(useCaseMock.execute(any())).thenReturn(new CollectExchangeRatesResult(1, 1));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dataReferencia", singleDate.toString())
                .addLong("run.id", 1791936680000L)
                .toJobParameters();

        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        RepeatStatus status = usdCollectorStep.execute(contribution, chunkContext);

        assertNotNull(status);
        assertEquals(RepeatStatus.FINISHED, status);

        CollectExchangeRatesCommand expectedCommand = new CollectExchangeRatesCommand(
                Currency.USD,
                singleDate,
                singleDate
        );
        verify(useCaseMock, times(1)).execute(expectedCommand);
    }

    @Test
    @DisplayName("Deve coletar um intervalo de datas quando startDate e endDate forem informadas")
    void shouldCollectDateRangeWhenStartAndEndDatesAreProvided() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 18);
        when(useCaseMock.execute(any())).thenReturn(new CollectExchangeRatesResult(12, 12));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())
                .addLong("run.id", 1791936680001L)
                .toJobParameters();

        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        RepeatStatus status = usdCollectorStep.execute(contribution, chunkContext);

        assertNotNull(status);
        assertEquals(RepeatStatus.FINISHED, status);

        CollectExchangeRatesCommand expectedCommand = new CollectExchangeRatesCommand(
                Currency.USD,
                startDate,
                endDate
        );
        verify(useCaseMock, times(1)).execute(expectedCommand);
    }
}
