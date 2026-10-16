package com.example.api_docker.infra.batch.service;

import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.descriptor.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço de orquestração reativa para jobs do Spring Batch.
 * <p>
 * Responsável por:
 * <ul>
 *   <li>Inspecionar dependências declarativas de jobs calculadores através dos {@link JobDescriptor}.</li>
 *   <li>Consultar o histórico do banco de dados (via {@link JobExplorer}) para validar se as coletas
 *       necessárias já foram concluídas com sucesso para a data de referência.</li>
 *   <li>Disparar coletas faltantes em paralelo caso o cálculo seja solicitado sem os dados brutos.</li>
 *   <li>Reagir a eventos de término de coleta vindos do Apache Kafka e acionar automaticamente os calculadores
 *       assim que a última dependência for satisfeita (Padrão Barreira / Join).</li>
 * </ul>
 */
@Service
public class BatchOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(BatchOrchestratorService.class);

    private final BatchJobLauncherService jobLauncherService;
    private final JobExplorer jobExplorer;
    private final Map<String, JobDescriptor> descriptorsByName;

    public BatchOrchestratorService(
            BatchJobLauncherService jobLauncherService,
            JobExplorer jobExplorer,
            List<BatchJobDescriptorProvider> descriptorProviders
    ) {
        this.jobLauncherService = jobLauncherService;
        this.jobExplorer = jobExplorer;
        this.descriptorsByName = descriptorProviders.stream()
                .map(BatchJobDescriptorProvider::getDescriptor)
                .collect(Collectors.toMap(JobDescriptor::jobName, Function.identity(), (d1, d2) -> d1));
    }

    /**
     * Solicita a execução de um job calculador com checagem prévia de dependências.
     * <p>
     * Se todas as coletas já tiverem sido concluídas para a data, o cálculo é iniciado imediatamente.
     * Caso contrário, dispara todos os jobs coletores pendentes em paralelo e aguarda a finalização
     * via eventos no Kafka para destravar o cálculo.
     *
     * @param calculatorJobName Nome do job calculador.
     * @param dataReferencia    Data de corte dos dados analíticos.
     * @param customParams      Parâmetros adicionais de execução.
     * @return {@link JobExecution} do cálculo se executado de imediato, ou {@code null} se aguardando coletas.
     * @throws Exception em caso de falha de lançamento.
     */
    public JobExecution requestCalculation(
            String calculatorJobName,
            String dataReferencia,
            Map<String, Object> customParams
    ) throws Exception {
        JobDescriptor descriptor = descriptorsByName.get(calculatorJobName);
        if (descriptor == null) {
            throw new IllegalArgumentException("JobDescriptor não encontrado para o job: " + calculatorJobName);
        }

        List<String> coletasPendentes = findPendingCollectorDependencies(calculatorJobName, dataReferencia);

        Map<String, Object> params = new HashMap<>();
        if (customParams != null) {
            params.putAll(customParams);
        }
        params.put("dataReferencia", dataReferencia);

        if (coletasPendentes.isEmpty()) {
            log.info("Todas as dependências do calculador [{}] estão satisfeitas para a data [{}]. Disparando cálculo imediatamente...",
                    calculatorJobName, dataReferencia);
            return jobLauncherService.runJob(calculatorJobName, params);
        }

        log.warn("Calculador [{}] possui {} coletas pendentes para data [{}]: {}. Disparando coletores em paralelo...",
                calculatorJobName, coletasPendentes.size(), dataReferencia, coletasPendentes);

        // Dispara todos os coletores pendentes em paralelo (executados pelo ThreadPool do Spring Batch)
        for (String collectorJobName : coletasPendentes) {
            Map<String, Object> collectorParams = new HashMap<>();
            collectorParams.put("dataReferencia", dataReferencia);
            jobLauncherService.runJob(collectorJobName, collectorParams);
        }

        return null;
    }

    /**
     * Ponto de entrada acionado pelo Consumidor Kafka quando qualquer Job Coletor finaliza com sucesso.
     * <p>
     * Avalia todos os calculadores que dependiam desse coletor. Se todas as dependências daquela data
     * estiverem prontas, aciona o calculador.
     *
     * @param collectorJobName Nome do coletor concluído.
     * @param dataReferencia   Data de referência concluída.
     */
    public void onCollectorCompleted(String collectorJobName, String dataReferencia) {
        log.info("Notificação de conclusão recebida para coletor [{}] na data [{}]", collectorJobName, dataReferencia);

        // Encontra todos os calculadores que declaram dependência desse coletor
        List<JobDescriptor> calculadoresInteressados = descriptorsByName.values().stream()
                .filter(d -> d.type() == JobType.CALCULATOR)
                .filter(d -> d.dependsOnJobNames().contains(collectorJobName))
                .toList();

        for (JobDescriptor calculador : calculadoresInteressados) {
            // O próprio coletor que emitiu o evento é considerado concluído (elimina race condition de commit no banco)
            List<String> pendentes = findPendingCollectorDependencies(
                    calculador.jobName(),
                    dataReferencia,
                    Set.of(collectorJobName)
            );

            if (pendentes.isEmpty()) {
                log.info("🎉 Todas as dependências do calculador [{}] foram concluídas para data [{}]. Disparando cálculo automaticamente!",
                        calculador.jobName(), dataReferencia);
                try {
                    jobLauncherService.runJob(calculador.jobName(), Map.of("dataReferencia", dataReferencia));
                } catch (Exception e) {
                    log.error("Erro ao disparar automaticamente calculador [{}] após término das coletas", calculador.jobName(), e);
                }
            } else {
                log.info("Calculador [{}] na data [{}] ainda aguarda as seguintes coletas: {}",
                        calculador.jobName(), dataReferencia, pendentes);
            }
        }
    }

    /**
     * Descobre quais jobs coletores obrigatórios ainda não concluíram com sucesso para uma data de referência.
     *
     * @param calculatorJobName Nome do calculador.
     * @param dataReferencia    Data de corte.
     * @return Lista com os nomes dos coletores pendentes.
     */
    public List<String> findPendingCollectorDependencies(String calculatorJobName, String dataReferencia) {
        return findPendingCollectorDependencies(calculatorJobName, dataReferencia, Collections.emptySet());
    }

    /**
     * Descobre quais jobs coletores obrigatórios ainda não concluíram com sucesso para uma data de referência,
     * permitindo especificar coletores que já devem ser considerados concluídos em memória (ex: o coletor do evento atual).
     *
     * @param calculatorJobName           Nome do calculador.
     * @param dataReferencia              Data de corte.
     * @param alreadyCompletedCollectors   Conjunto de nomes de coletores a ignorar na checagem de banco.
     * @return Lista com os nomes dos coletores pendentes.
     */
    public List<String> findPendingCollectorDependencies(
            String calculatorJobName,
            String dataReferencia,
            Set<String> alreadyCompletedCollectors
    ) {
        JobDescriptor descriptor = descriptorsByName.get(calculatorJobName);
        if (descriptor == null || !descriptor.hasDependencies()) {
            return Collections.emptyList();
        }

        List<String> pendentes = new ArrayList<>();
        for (String requiredCollector : descriptor.dependsOnJobNames()) {
            if (alreadyCompletedCollectors != null && alreadyCompletedCollectors.contains(requiredCollector)) {
                continue;
            }
            if (!isJobCompletedForDate(requiredCollector, dataReferencia)) {
                pendentes.add(requiredCollector);
            }
        }
        return pendentes;
    }

    /**
     * Consulta os metadados do Spring Batch para averiguar se um job possui alguma execução
     * bem-sucedida ({@link BatchStatus#COMPLETED}) para a data informada.
     *
     * @param jobName        Nome do job.
     * @param dataReferencia Data de corte.
     * @return true se já concluiu com sucesso, false caso contrário.
     */
    public boolean isJobCompletedForDate(String jobName, String dataReferencia) {
        if (jobName == null || dataReferencia == null) {
            return false;
        }

        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 100);
        for (JobInstance instance : instances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
            for (JobExecution execution : executions) {
                if (execution.getStatus() == BatchStatus.COMPLETED) {
                    String dataParam = execution.getJobParameters().getString("dataReferencia");
                    if (dataReferencia.equals(dataParam)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
