package com.example.api_docker.infra.batch.job.collector;

import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.descriptor.JobType;
import com.example.api_docker.infra.batch.job.collector.step.StepCollector;
import com.example.api_docker.infra.batch.listener.JobCompletionNotificationListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuração declarativa do Job Coletor base da aplicação.
 * <p>
 * <b>Papel na Arquitetura:</b>
 * Esta classe é puramente orquestradora: monta o pipeline do Job conectando os metadados,
 * listeners de auditoria e o step de execução ([@link StepCollector]). Toda a lógica operacional
 * de coleta reside na classe dedicada do Step.
 */
@Configuration
public class CollectorJobConfig {

    /**
     * Identificador único do job coletor no contexto do Spring Batch.
     */
    public static final String COLLECTOR_JOB_NAME = "collectorJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobCompletionNotificationListener jobExecutionListener;

    public CollectorJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JobCompletionNotificationListener jobExecutionListener
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobExecutionListener = jobExecutionListener;
    }

    /**
     * Declara o Job Coletor vinculando o listener e o step de coleta.
     *
     * @param collectorStep Step contendo a lógica de coleta encapsulada em {@link StepCollector}.
     * @return Instância configurada do {@link Job}.
     */
    @Bean(name = COLLECTOR_JOB_NAME)
    public Job collectorJob(Step collectorStep) {
        return new JobBuilder(COLLECTOR_JOB_NAME, jobRepository)
                .listener(jobExecutionListener)
                .start(collectorStep)
                .build();
    }

    /**
     * Declara o step de execução delegando a lógica para a classe {@link StepCollector}.
     *
     * @param stepCollector Componente contendo a implementação do {@link org.springframework.batch.core.step.tasklet.Tasklet}.
     * @return Instância configurada do {@link Step}.
     */
    @Bean
    public Step collectorStep(StepCollector stepCollector) {
        return new StepBuilder("collectorStep", jobRepository)
                .tasklet(stepCollector, transactionManager)
                .build();
    }

    /**
     * Declara os metadados do job coletor para descoberta e orquestração dinâmica.
     */
    @Bean
    public JobDescriptor collectorJobDescriptor() {
        return new JobDescriptor(COLLECTOR_JOB_NAME, JobType.COLLECTOR);
    }
}
