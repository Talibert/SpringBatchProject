package com.example.api_docker.infra.batch.job.collector.step;

import com.example.api_docker.application.exchangerate.command.CollectExchangeRatesCommand;
import com.example.api_docker.application.exchangerate.result.CollectExchangeRatesResult;
import com.example.api_docker.application.exchangerate.usecase.CollectExchangeRatesUseCase;
import com.example.api_docker.domain.exchangerate.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Step responsável por acionar a coleta de cotações de dólar (USD).
 * <p>
 * Suporta a leitura de um intervalo de datas ({@code startDate}/{@code dataInicial} e {@code endDate}/{@code dataFinal})
 * ou de uma única data ({@code dataReferencia} ou apenas a data inicial), realizando a coleta de um único dia
 * quando apenas uma data é informada.
 */
@Component
public class UsdCollectorStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(UsdCollectorStep.class);

    private final CollectExchangeRatesUseCase collectExchangeRatesUseCase;

    public UsdCollectorStep(CollectExchangeRatesUseCase collectExchangeRatesUseCase) {
        this.collectExchangeRatesUseCase = collectExchangeRatesUseCase;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executando UsdCollectorStep: iniciando processo de coleta de cotações USD...");

        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
        LocalDate startDate = resolveStartDate(jobParameters);
        LocalDate endDate = resolveEndDate(jobParameters, startDate);

        log.info("Processando coleta de USD para o período de [{}] até [{}]", startDate, endDate);

        CollectExchangeRatesCommand command = new CollectExchangeRatesCommand(Currency.USD, startDate, endDate);
        CollectExchangeRatesResult result = collectExchangeRatesUseCase.execute(command);

        log.info(
                "UsdCollectorStep finalizado com sucesso: {} cotações obtidas, {} novas cotações salvas.",
                result.fetchedCount(),
                result.savedCount()
        );
        return RepeatStatus.FINISHED;
    }

    private LocalDate resolveStartDate(Map<String, Object> jobParameters) {
        String rawStart = getStringParam(jobParameters, "startDate", "dataInicial", "dataReferencia");
        if (rawStart != null) {
            return LocalDate.parse(rawStart);
        }
        return LocalDate.now();
    }

    private LocalDate resolveEndDate(Map<String, Object> jobParameters, LocalDate startDate) {
        String rawEnd = getStringParam(jobParameters, "endDate", "dataFinal");
        if (rawEnd != null) {
            return LocalDate.parse(rawEnd);
        }
        return startDate;
    }

    private String getStringParam(Map<String, Object> jobParameters, String... keys) {
        for (String key : keys) {
            Object value = jobParameters.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str.trim();
            }
        }
        return null;
    }
}
