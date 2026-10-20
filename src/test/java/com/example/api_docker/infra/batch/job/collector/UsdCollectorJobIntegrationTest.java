package com.example.api_docker.infra.batch.job.collector;

import com.example.api_docker.BatchIntegrationAbstractTests;
import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.infra.batch.service.BatchJobLauncherService;
import com.example.api_docker.infra.http.HttpDownloadClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração ponta a ponta do Job Coletor de Dólar,
 * mockando apenas o transporte HTTP bruto ({@link HttpDownloadClient}) com o JSON do BCB
 * e validando o parse, conversão para domínio, persistência e idempotência no banco de dados.
 */
class UsdCollectorJobIntegrationTest extends BatchIntegrationAbstractTests {

    @Autowired
    private BatchJobLauncherService jobLauncherService;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @MockitoBean
    private HttpDownloadClient httpDownloadClientMock;

    @Test
    @DisplayName("Deve executar usdCollectorJob fazendo parse do JSON mockado do BCB, persistindo no banco sem duplicar em reexecução")
    void shouldExecuteUsdCollectorJobParsingBcbJsonAndPersistingWithoutDuplicates() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 2);

        String bcbJsonResponse = """
                {
                  "@odata.context": "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/$metadata#_CotacaoDolarPeriodo",
                  "value": [
                    {
                      "cotacaoCompra": 5.4510,
                      "cotacaoVenda": 5.4516,
                      "dataHoraCotacao": "2026-10-01 13:05:12.345"
                    },
                    {
                      "cotacaoCompra": 5.4620,
                      "cotacaoVenda": 5.4628,
                      "dataHoraCotacao": "2026-10-02 13:10:00.0"
                    }
                  ]
                }
                """;

        Mockito.when(httpDownloadClientMock.downloadAsString(Mockito.any(URI.class)))
                .thenReturn(bcbJsonResponse);

        // 1ª Execução: Coleta intervalo de 2 dias
        JobExecution firstExecution = jobLauncherService.runJob(
                UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME,
                Map.of(
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                )
        );

        assertNotNull(firstExecution);
        assertEquals(BatchStatus.COMPLETED, firstExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), firstExecution.getExitStatus().getExitCode());

        assertFalse(firstExecution.getStepExecutions().isEmpty());
        StepExecution stepExecution = firstExecution.getStepExecutions().iterator().next();
        assertEquals(UsdCollectorJobConfig.USD_COLLECTOR_STEP_NAME, stepExecution.getStepName());
        assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

        // Valida a URI construída pelo BcbCotacaoDolarClient
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        Mockito.verify(httpDownloadClientMock, Mockito.times(1)).downloadAsString(uriCaptor.capture());
        String calledUri = uriCaptor.getValue().toString();
        assertTrue(calledUri.contains("@dataInicial='10-01-2026'"));
        assertTrue(calledUri.contains("@dataFinalCotacao='10-02-2026'"));

        // Valida os registros salvos no banco de dados
        List<ExchangeRate> savedRates = exchangeRateRepository.findByCurrencyAndReferenceDateBetween(
                Currency.USD,
                startDate,
                endDate
        );
        assertEquals(2, savedRates.size());

        ExchangeRate firstRate = savedRates.get(0);
        assertEquals(Currency.USD, firstRate.getCurrency());
        assertEquals(LocalDate.of(2026, 10, 1), firstRate.getReferenceDate());
        assertEquals(LocalDateTime.of(2026, 10, 1, 13, 5, 12, 345_000_000), firstRate.getQuoteTimestamp());
        assertEquals(0, new BigDecimal("5.4510").compareTo(firstRate.getBuyRate()));
        assertEquals(0, new BigDecimal("5.4516").compareTo(firstRate.getSellRate()));

        ExchangeRate secondRate = savedRates.get(1);
        assertEquals(Currency.USD, secondRate.getCurrency());
        assertEquals(LocalDate.of(2026, 10, 2), secondRate.getReferenceDate());
        assertEquals(LocalDateTime.of(2026, 10, 2, 13, 10, 0), secondRate.getQuoteTimestamp());
        assertEquals(0, new BigDecimal("5.4620").compareTo(secondRate.getBuyRate()));
        assertEquals(0, new BigDecimal("5.4628").compareTo(secondRate.getSellRate()));

        // 2ª Execução para o mesmo período: valida idempotência (não deve duplicar registros no banco)
        JobExecution secondExecution = jobLauncherService.runJob(
                UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME,
                Map.of(
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString()
                )
        );
        assertEquals(BatchStatus.COMPLETED, secondExecution.getStatus());

        List<ExchangeRate> ratesAfterReExecution = exchangeRateRepository.findByCurrencyAndReferenceDateBetween(
                Currency.USD,
                startDate,
                endDate
        );
        assertEquals(2, ratesAfterReExecution.size());

        // Valida metadados do Job no JobExplorer
        JobExecution persistedExecution = jobExplorer.getJobExecution(firstExecution.getId());
        assertNotNull(persistedExecution);
        assertEquals(UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME, persistedExecution.getJobInstance().getJobName());
    }
}
