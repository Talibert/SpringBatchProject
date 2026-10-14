package com.example.api_docker.infra.batch.job.collector.step;

import com.example.api_docker.infra.batch.job.collector.client.ExternalDataCollectorClient;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Teste unitário isolado do StepCollector.
 * <p>
 * <b>Por que MetaDataInstanceFactory é superior a mocks do Mockito para o contexto do Batch:</b>
 * <ul>
 *   <li>Se usássemos Mockito para o ChunkContext: teríamos que mockar ChunkContext E StepContext,
 *       fazendo {@code when(chunkContext.getStepContext()).thenReturn(mockStepContext)}, gerando mocks aninhados frágeis.</li>
 *   <li>Com {@link MetaDataInstanceFactory}: passamos instâncias reais de {@link JobParameters}
 *       (com data, identificador, lote, etc.) e ele monta a árvore inteira de execução em memória em 1 linha.</li>
 * </ul>
 */
class StepCollectorTest {

    private final ExternalDataCollectorClient clientMock = mock(ExternalDataCollectorClient.class);
    private final StepCollector stepCollector = new StepCollector(clientMock);

    @Test
    @DisplayName("Deve extrair parâmetros reais via MetaDataInstanceFactory e executar StepCollector com sucesso")
    void shouldExecuteStepCollectorWithRealJobParametersFromMetaDataFactory() throws Exception {
        // Parâmetros reais de negócio esperados pelo Job
        String dataCorte = LocalDate.of(2026, 10, 13).toString();
        String origemExterna = "bacen-taxa-cambio";

        when(clientMock.fetchExternalData(origemExterna)).thenReturn(List.of("USD: 5.60", "EUR: 6.10"));

        // 1. Cria parâmetros reais do Job via JobParametersBuilder
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dataReferencia", dataCorte)
                .addString("origem", origemExterna)
                .addLong("run.id", 1791936680000L)
                .toJobParameters();

        // 2. O MetaDataInstanceFactory cria um StepExecution REAL em memória com os parâmetros reais injetados
        // SEM precisar de nenhum mockito mock para contexto!
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);

        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // 3. Executa a lógica do Step
        RepeatStatus status = stepCollector.execute(contribution, chunkContext);

        // 4. Asserções
        assertNotNull(status);
        assertEquals(RepeatStatus.FINISHED, status);

        // Verifica que o cliente externo foi acionado exatamente com o parâmetro 'origem' vindo do JobParameters
        verify(clientMock, times(1)).fetchExternalData(origemExterna);
    }
}
