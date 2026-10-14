package com.example.api_docker.infra.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Serviço de infraestrutura para disparo dinâmico e rastreamento de jobs do Spring Batch.
 * <p>
 * <b>Funcionalidades:</b>
 * <ul>
 *   <li>Descoberta automática de todos os beans de {@link Job} registrados na aplicação através do mapa injetado.</li>
 *   <li>Adição de parâmetros automáticos de rastreabilidade (como {@code run.id} e {@code dataHoraDisparo}),
 *       garantindo que cada disparo crie uma nova {@code JobExecution} única.</li>
 *   <li>Métodos utilitários convenientes para acionar jobs coletores e calculadores.</li>
 *   <li>Consulta direta do histórico de execuções via {@link JobExplorer}.</li>
 * </ul>
 */
@Service
public class BatchJobLauncherService {

    private static final Logger log = LoggerFactory.getLogger(BatchJobLauncherService.class);

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Map<String, Job> jobsByName;

    public BatchJobLauncherService(
            JobLauncher jobLauncher,
            JobExplorer jobExplorer,
            Map<String, Job> jobsByName
    ) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobsByName = jobsByName;
    }

    /**
     * Dispara um job específico pelo seu identificador/nome de bean com parâmetros customizados.
     *
     * @param nomeJob           Nome do job registrado no contexto Spring.
     * @param parametrosCustom  Mapa de parâmetros adicionais para a execução (valores String, Long, Double, etc.).
     * @return {@link JobExecution} contendo os metadados e o status inicial da execução.
     * @throws IllegalArgumentException caso o job informado não seja encontrado no contexto.
     * @throws Exception                em caso de falha de lançamento pelo Spring Batch.
     */
    public JobExecution runJob(String nomeJob, Map<String, Object> parametrosCustom) throws Exception {
        Job job = jobsByName.get(nomeJob);
        if (job == null) {
            throw new IllegalArgumentException("Job não encontrado com o nome: " + nomeJob);
        }

        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addLong("run.id", System.currentTimeMillis());
        paramsBuilder.addString("dataHoraDisparo", Instant.now().toString());

        if (parametrosCustom != null) {
            parametrosCustom.forEach((chave, valor) -> {
                if (valor instanceof String s) {
                    paramsBuilder.addString(chave, s);
                } else if (valor instanceof Long l) {
                    paramsBuilder.addLong(chave, l);
                } else if (valor instanceof Double d) {
                    paramsBuilder.addDouble(chave, d);
                } else if (valor != null) {
                    paramsBuilder.addString(chave, valor.toString());
                }
            });
        }

        JobParameters parameters = paramsBuilder.toJobParameters();
        log.info("Disparando job [{}] com parâmetros: {}", nomeJob, parameters);
        return jobLauncher.run(job, parameters);
    }



    /**
     * Recupera os dados e status de uma execução de job pelo seu ID.
     *
     * @param jobExecutionId Identificador da execução.
     * @return {@link JobExecution} com detalhes de status, duração e steps, ou null se não encontrada.
     */
    public JobExecution getJobExecution(Long jobExecutionId) {
        return jobExplorer.getJobExecution(jobExecutionId);
    }
}
