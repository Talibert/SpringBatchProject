package com.example.api_docker.infra.batch.job.collector;

import com.example.api_docker.BatchIntegrationAbstractTests;
import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateProvider;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.infra.batch.service.BatchJobLauncherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testes de integração para validação do ciclo de vida e persistência do Job Coletor de Dólar.
 */
class UsdCollectorJobIntegrationTest extends BatchIntegrationAbstractTests {

    @Autowired
    private BatchJobLauncherService jobLauncherService;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @MockitoBean
    private ExchangeRateProvider exchangeRateProviderMock;

    @Test
    @DisplayName("Deve disparar e executar o usdCollectorJob salvando as cotações de dólar coletadas no banco")
    void shouldExecuteUsdCollectorJobSuccessfully() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 10, 10);
        LocalDateTime quoteTimestamp = LocalDateTime.of(2026, 10, 10, 13, 5, 0);

        ExchangeRate mockedRate = ExchangeRate.create(
                Currency.USD,
                quoteTimestamp,
                new BigDecimal("5.4510"),
                new BigDecimal("5.4516")
        );

        Mockito.when(exchangeRateProviderMock.fetchExchangeRates(Currency.USD, targetDate, targetDate))
                .thenReturn(List.of(mockedRate));

        JobExecution execution = jobLauncherService.runJob(
                UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME,
                Map.of("dataReferencia", targetDate.toString())
        );

        assertNotNull(execution);
        assertNotNull(execution.getId());
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), execution.getExitStatus().getExitCode());

        assertFalse(execution.getStepExecutions().isEmpty());
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertEquals(UsdCollectorJobConfig.USD_COLLECTOR_STEP_NAME, stepExecution.getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        Mockito.verify(exchangeRateProviderMock, Mockito.times(1))
                .fetchExchangeRates(Currency.USD, targetDate, targetDate);

        List<ExchangeRate> savedRates = exchangeRateRepository.findByCurrencyAndReferenceDateBetween(
                Currency.USD,
                targetDate,
                targetDate
        );
        assertEquals(1, savedRates.size());
        assertEquals(0, new BigDecimal("5.4510").compareTo(savedRates.get(0).getBuyRate()));

        JobExecution persistedExecution = jobExplorer.getJobExecution(execution.getId());
        assertNotNull(persistedExecution);
        assertEquals(execution.getId(), persistedExecution.getId());
        assertEquals(UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME, persistedExecution.getJobInstance().getJobName());
    }
}
