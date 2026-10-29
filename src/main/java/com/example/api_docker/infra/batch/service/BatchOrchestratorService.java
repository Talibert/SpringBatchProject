package com.example.api_docker.infra.batch.service;

import com.example.api_docker.domain.batch.CalculatorBarrierKey;
import com.example.api_docker.domain.batch.CalculatorBarrierState;
import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.domain.batch.TargetCalculatorContext;
import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ObjectMapper objectMapper;

    /**
     * Mapa thread-safe de barreiras ativas indexado pelo Value Object {@link CalculatorBarrierKey}.
     * <p>
     * Cada entrada representa um Job Calculador que está aguardando a conclusão de seus Jobs Coletores
     * para uma determinada data de referência. Quando o Calculador conclui sua execução, a entrada
     * é removida deste mapa para evitar retenção desnecessária de memória no Singleton.
     */
    private final ConcurrentHashMap<CalculatorBarrierKey, CalculatorBarrierState> activeBarriers = new ConcurrentHashMap<>();

    public BatchOrchestratorService(
            BatchJobLauncherService jobLauncherService,
            JobExplorer jobExplorer,
            List<BatchJobDescriptorProvider> descriptorProviders,
            ObjectMapper objectMapper
    ) {
        this.jobLauncherService = jobLauncherService;
        this.jobExplorer = jobExplorer;
        this.descriptorsByName = descriptorProviders.stream()
                .map(BatchJobDescriptorProvider::getDescriptor)
                .collect(Collectors.toMap(JobDescriptor::jobName, Function.identity(), (d1, d2) -> d1));
        this.objectMapper = objectMapper;
    }

    /**
     * Retorna a lista de todos os descritores de jobs registrados na aplicação.
     */
    public List<JobDescriptor> getAvailableDescriptors() {
        return List.copyOf(descriptorsByName.values());
    }

    /**
     * Recupera o descritor de um job pelo nome.
     */
    public JobDescriptor getDescriptor(String jobName) {
        return descriptorsByName.get(jobName);
    }

    /**
     * Retorna a lista de todas as execuções de jobs que estão atualmente em andamento (STARTING ou STARTED).
     *
     * @return Lista de {@link JobExecution} em execução ordenada pelas mais recentes.
     */
    public List<JobExecution> getRunningJobExecutions() {
        List<JobExecution> runningExecutions = new ArrayList<>();
        for (String jobName : descriptorsByName.keySet()) {
            Set<JobExecution> running = jobExplorer.findRunningJobExecutions(jobName);
            if (running != null) {
                runningExecutions.addAll(running);
            }
        }
        runningExecutions.sort(Comparator.comparing(JobExecution::getId).reversed());
        return runningExecutions;
    }

    /**
     * Ponto de entrada unificado para solicitação de execução de jobs (chamado pelo Controller).
     * <p>
     * Se o job for do tipo {@link JobType#CALCULATOR}, avalia dependências: se faltarem coletas, dispara-as
     * em paralelo e informa na resposta quais dependências foram acionadas. Se já estiverem satisfeitas,
     * inicia o cálculo de imediato.
     * Se for do tipo {@link JobType#COLLECTOR}, dispara diretamente.
     *
     * @param jobName        Nome do job solicitado.
     * @param dataReferencia Data de corte e referência.
     * @param customParams   Parâmetros adicionais customizados.
     * @return {@link OrchestratedJobTriggerResult} com detalhes sobre início imediato ou disparo de dependências.
     * @throws IllegalArgumentException se o job não for encontrado.
     * @throws Exception em caso de erro no Spring Batch.
     */
    public OrchestratedJobTriggerResult triggerJob(
            String jobName,
            String dataReferencia,
            Map<String, Object> customParams
    ) throws Exception {
        JobDescriptor descriptor = descriptorsByName.get(jobName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Job não encontrado no catálogo registrado: " + jobName);
        }

        Map<String, Object> params = buildParams(dataReferencia, customParams);

        if (descriptor.type() == JobType.CALCULATOR) {
            List<String> coletasPendentes = findPendingCollectorDependencies(jobName, dataReferencia);
            if (coletasPendentes.isEmpty()) {
                log.info("Todas as dependências do calculador [{}] estão satisfeitas para a data [{}]. Disparando cálculo imediatamente...",
                        jobName, dataReferencia);
                JobExecution execution = jobLauncherService.runJob(jobName, params);
                return new OrchestratedJobTriggerResult(
                        jobName,
                        dataReferencia,
                        execution,
                        true,
                        List.of(),
                        String.format("Todas as dependências do calculador [%s] estão satisfeitas para a data [%s]. O cálculo foi iniciado em segundo plano.", jobName, dataReferencia)
                );
            } else {
                log.warn("Calculador [{}] possui {} coletas pendentes para data [{}]: {}. Disparando coletores em paralelo...",
                        jobName, coletasPendentes.size(), dataReferencia, coletasPendentes);

                clearBarrierState(jobName, dataReferencia);

                TargetCalculatorContext targetContext = TargetCalculatorContext.of(jobName, customParams);
                String targetContextJson = serializeTargetContext(targetContext);

                // Dispara todos os coletores pendentes em paralelo (executados pelo ThreadPool do Spring Batch)
                for (String collectorJobName : coletasPendentes) {
                    Map<String, Object> collectorParams = Map.of(
                            "dataReferencia", dataReferencia,
                            "targetCalculatorContext", targetContextJson
                    );
                    jobLauncherService.runJob(collectorJobName, collectorParams);
                }

                return new OrchestratedJobTriggerResult(
                        jobName,
                        dataReferencia,
                        null,
                        false,
                        coletasPendentes,
                        String.format("Solicitação aceita. O calculador [%s] possui %d coleta(s) pendente(s) para a data [%s]: %s. As coletas foram disparadas em segundo plano e o cálculo iniciará automaticamente assim que concluírem.",
                                jobName, coletasPendentes.size(), dataReferencia, coletasPendentes)
                );
            }
        } else {
            JobExecution execution = jobLauncherService.runJob(jobName, params);
            return new OrchestratedJobTriggerResult(
                    jobName,
                    dataReferencia,
                    execution,
                    true,
                    List.of(),
                    String.format("Execução do job [%s] para a data [%s] solicitada com sucesso. O processamento ocorrerá em segundo plano.", jobName, dataReferencia)
            );
        }
    }

    private Map<String, Object> buildParams(String dataReferencia, Map<String, Object> customParams) {
        Map<String, Object> params = new HashMap<>();
        if (customParams != null) {
            params.putAll(customParams);
        }
        params.put("dataReferencia", dataReferencia);
        return params;
    }

    /**
     * Ponto de entrada unificado para processar eventos de conclusão de qualquer job vindos de mensageria.
     * <p>
     * Centraliza a decisão de negócio: apenas eventos de jobs coletores concluídos com sucesso
     * que possuam um {@link TargetCalculatorContext} acionam a verificação de barreira do calculador alvo.
     *
     * @param event Evento de conclusão recebido.
     */
    public void handleJobCompletedEvent(BatchJobCompletedEvent event) {
        if (event == null) {
            return;
        }
        if (!BatchStatus.COMPLETED.name().equalsIgnoreCase(event.status())) {
            log.debug("Job [{}] finalizou com status [{}]. Orquestração ignorada.", event.jobName(), event.status());
            return;
        }
        if (event.isCalculator()) {
            clearBarrierState(event.jobName(), event.dataReferencia());
            log.debug("Calculador [{}] finalizado para data [{}]. Estado em memória da barreira limpo.",
                    event.jobName(), event.dataReferencia());
            return;
        }
        if (event.isCollector() && event.hasTargetContext()) {
            onCollectorCompleted(event.jobName(), event.dataReferencia(), event.targetContext());
        } else {
            log.debug("Job [{}] finalizado sem target context. Nenhuma checagem de orquestração necessária.", event.jobName());
        }
    }

    /**
     * Ponto de entrada acionado quando um Job Coletor acionado por um Calculador alvo finaliza com sucesso.
     * <p>
     * Avalia única e exclusivamente as dependências do calculador especificado em {@code targetContext}.
     * Protegido contra condições de corrida quando múltiplos coletores terminam simultaneamente:
     * registra a conclusão em memória por barreira e sincroniza a avaliação/disparo.
     *
     * @param collectorJobName Nome do coletor concluído.
     * @param dataReferencia   Data de referência concluída.
     * @param targetContext    Value Object contendo o calculador alvo e seus parâmetros originais (obrigatório).
     */
    public void onCollectorCompleted(String collectorJobName, String dataReferencia, TargetCalculatorContext targetContext) {
        if (targetContext == null || targetContext.targetJobName() == null || targetContext.targetJobName().isBlank()) {
            log.warn("onCollectorCompleted acionado sem targetContext para coletor [{}]. Nenhuma checagem será executada.",
                    collectorJobName);
            return;
        }

        String targetCalculatorJob = targetContext.targetJobName();
        log.info("Notificação de conclusão recebida para coletor [{}] na data [{}], avaliando calculador alvo [{}]",
                collectorJobName, dataReferencia, targetCalculatorJob);

        JobDescriptor targetDescriptor = descriptorsByName.get(targetCalculatorJob);
        if (targetDescriptor == null || targetDescriptor.type() != JobType.CALCULATOR
                || !targetDescriptor.dependsOnJobNames().contains(collectorJobName)) {
            log.warn("Job target [{}] não encontrado, não é CALCULATOR ou não depende do coletor [{}]",
                    targetCalculatorJob, collectorJobName);
            return;
        }

        CalculatorBarrierKey barrierKey = CalculatorBarrierKey.of(targetDescriptor.jobName(), dataReferencia);
        CalculatorBarrierState barrierState = activeBarriers.computeIfAbsent(
                barrierKey,
                key -> new CalculatorBarrierState()
        );

        // Sincroniza no próprio objeto de estado da barreira para garantir atomicidade entre coletores concorrentes
        synchronized (barrierState) {
            if (barrierState.isTriggered()) {
                log.debug("Calculador alvo [{}] na data [{}] já foi disparado por outro evento concorrente. Ignorando.",
                        targetDescriptor.jobName(), dataReferencia);
                return;
            }

            barrierState.registerCompletedCollector(collectorJobName);

            List<String> pendentes = findPendingCollectorDependencies(
                    targetDescriptor.jobName(),
                    dataReferencia,
                    barrierState.getCompletedCollectors()
            );

            if (pendentes.isEmpty()) {
                barrierState.markTriggered();
                log.info("🎉 Todas as dependências do calculador alvo [{}] foram concluídas para data [{}]. Disparando cálculo com parâmetros originais preservados!",
                        targetDescriptor.jobName(), dataReferencia);
                try {
                    Map<String, Object> calculatorParams = new HashMap<>(targetContext.customParams());
                    calculatorParams.put("dataReferencia", dataReferencia);
                    jobLauncherService.runJob(targetDescriptor.jobName(), calculatorParams);
                } catch (Exception e) {
                    barrierState.revertTrigger();
                    log.error("Erro ao disparar automaticamente calculador alvo [{}] após término das coletas",
                            targetDescriptor.jobName(), e);
                }
            } else {
                log.info("Calculador alvo [{}] na data [{}] ainda aguarda as seguintes coletas: {}",
                        targetDescriptor.jobName(), dataReferencia, pendentes);
            }
        }
    }

    /**
     * Remove integralmente o estado transitório em memória associado a uma barreira ({@link CalculatorBarrierKey}).
     */
    public void clearBarrierState(String calculatorJobName, String dataReferencia) {
        if (calculatorJobName == null || calculatorJobName.isBlank()
                || dataReferencia == null || dataReferencia.isBlank()) {
            return;
        }
        activeBarriers.remove(CalculatorBarrierKey.of(calculatorJobName, dataReferencia));
    }

    boolean hasActiveBarrierState(String calculatorJobName, String dataReferencia) {
        return activeBarriers.containsKey(CalculatorBarrierKey.of(calculatorJobName, dataReferencia));
    }

    private String serializeTargetContext(TargetCalculatorContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            log.error("Erro ao serializar TargetCalculatorContext para o job [{}]", context.targetJobName(), e);
            throw new RuntimeException("Falha ao serializar contexto da orquestração", e);
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
