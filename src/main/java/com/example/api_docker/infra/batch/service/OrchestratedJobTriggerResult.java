package com.example.api_docker.infra.batch.service;

import org.springframework.batch.core.JobExecution;

import java.util.Collections;
import java.util.List;

/**
 * Resultado da solicitação de disparo de um job pelo {@link BatchOrchestratorService}.
 * <p>
 * Carrega metadados ricos sobre se o job solicitado iniciou de imediato ou se dependências
 * tiveram que ser disparadas em segundo plano antes do cálculo.
 *
 * @param jobName               Nome do job solicitado.
 * @param dataReferencia        Data de referência do processamento.
 * @param jobExecution          Instância de execução iniciada (se iniciou de imediato), ou null se aguardando dependências.
 * @param startedImmediately   Indica se o job principal já foi iniciado de imediato.
 * @param triggeredDependencies Lista de nomes dos jobs coletores disparados em paralelo como dependência.
 * @param message               Mensagem descritiva contextualizada para retorno da API.
 */
public record OrchestratedJobTriggerResult(
        String jobName,
        String dataReferencia,
        JobExecution jobExecution,
        boolean startedImmediately,
        List<String> triggeredDependencies,
        String message
) {
    public OrchestratedJobTriggerResult {
        triggeredDependencies = triggeredDependencies != null
                ? Collections.unmodifiableList(triggeredDependencies)
                : Collections.emptyList();
    }
}
