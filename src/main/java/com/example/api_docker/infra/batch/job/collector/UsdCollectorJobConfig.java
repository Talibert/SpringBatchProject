package com.example.api_docker.infra.batch.job.collector;

import com.example.api_docker.infra.batch.job.collector.step.UsdCollectorStep;
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
 * Configuração declarativa do Job Coletor de cotações de Dólar (USD).
 * <p>
 * <b>Papel na Arquitetura:</b>
 * Esta classe é puramente orquestradora: monta o pipeline do Job conectando os metadados,
 * listeners de auditoria e o step de execução ({@link UsdCollectorStep}).
 */
@Configuration
public class UsdCollectorJobConfig {

    /**
     * Identificador único do job coletor de dólar no contexto do Spring Batch.
     */
    public static final String USD_COLLECTOR_JOB_NAME = "usdCollectorJob";

    /**
     * Identificador do step de coleta de dólar.
     */
    public static final String USD_COLLECTOR_STEP_NAME = "usdCollectorStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobCompletionNotificationListener jobExecutionListener;

    public UsdCollectorJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JobCompletionNotificationListener jobExecutionListener
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobExecutionListener = jobExecutionListener;
    }

    /**
     * Declara o Job Coletor de Dólar vinculando o listener e o step de coleta.
     *
     * @param usdCollectorStep Componente contendo a implementação do {@link org.springframework.batch.core.step.tasklet.Tasklet}.
     * @return Instância configurada do {@link Job}.
     */
    @Bean(name = USD_COLLECTOR_JOB_NAME)
    public Job usdCollectorJob(UsdCollectorStep usdCollectorStep) {
        return new JobBuilder(USD_COLLECTOR_JOB_NAME, jobRepository)
                .listener(jobExecutionListener)
                .start(buildStep(usdCollectorStep))
                .build();
    }

    private Step buildStep(UsdCollectorStep usdCollectorStep) {
        return new StepBuilder(USD_COLLECTOR_STEP_NAME, jobRepository)
                .tasklet(usdCollectorStep, transactionManager)
                .build();
    }
}
