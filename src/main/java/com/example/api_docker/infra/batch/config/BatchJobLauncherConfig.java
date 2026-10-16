package com.example.api_docker.infra.batch.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração declarativa dos launchers do Spring Batch.
 * <p>
 * Disponibiliza duas estratégias distintas e mutuamente exclusivas via {@link ConditionalOnProperty}:
 * <ul>
 *   <li><b>asyncJobLauncher (Produção / Padrão):</b> Ativo quando {@code spring.batch.job.async.enabled=true}.
 *       Cria um {@link ThreadPoolTaskExecutor} para permitir que múltiplos jobs executem em paralelo.</li>
 *   <li><b>syncJobLauncher (Testes Determinísticos):</b> Ativo quando {@code spring.batch.job.async.enabled=false}.
 *       Utiliza um {@link SyncTaskExecutor} sequencial na thread chamadora, sem alocar pool de threads à toa.</li>
 * </ul>
 */
@Configuration
public class BatchJobLauncherConfig {

    /**
     * Pool de threads dedicado para execuções assíncronas do Spring Batch.
     * Só é instanciado na memória quando o modo assíncrono estiver ativado.
     */
    @Bean(name = "batchTaskExecutor")
    @ConditionalOnProperty(name = "spring.batch.job.async.enabled", havingValue = "true", matchIfMissing = true)
    public TaskExecutor batchTaskExecutor(
            @Value("${spring.batch.thread-pool.core-size:4}") int corePoolSize,
            @Value("${spring.batch.thread-pool.max-size:8}") int maxPoolSize,
            @Value("${spring.batch.thread-pool.queue-capacity:50}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-exec-");
        executor.initialize();
        return executor;
    }

    /**
     * Bean 1: Launcher Assíncrono com ThreadPool para execução paralela não-bloqueante.
     * Ativo em produção e testes de orquestração distribuída.
     */
    @Bean(name = "asyncJobLauncher")
    @Primary
    @ConditionalOnProperty(name = "spring.batch.job.async.enabled", havingValue = "true", matchIfMissing = true)
    public JobLauncher asyncJobLauncher(
            JobRepository jobRepository,
            @Qualifier("batchTaskExecutor") TaskExecutor taskExecutor
    ) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * Bean 2: Launcher Síncrono para testes determinísticos sem corrida de threads.
     * Ativo quando spring.batch.job.async.enabled=false.
     */
    @Bean(name = "syncJobLauncher")
    @Primary
    @ConditionalOnProperty(name = "spring.batch.job.async.enabled", havingValue = "false")
    public JobLauncher syncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
