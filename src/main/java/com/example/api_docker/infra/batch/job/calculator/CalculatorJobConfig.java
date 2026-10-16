package com.example.api_docker.infra.batch.job.calculator;

import com.example.api_docker.infra.batch.job.calculator.step.StepCalculator;
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
 * Configuração declarativa do Job Calculador base da aplicação.
 * <p>
 * <b>Papel na Arquitetura:</b>
 * Esta classe é puramente orquestradora: monta o pipeline do Job conectando os metadados,
 * listeners de auditoria e o step de cálculo ({@link StepCalculator}). Toda a lógica
 * de fórmulas e consolidação analítica reside na classe dedicada do Step.
 */
@Configuration
public class CalculatorJobConfig {

    /**
     * Identificador único do job calculador no contexto do Spring Batch.
     */
    public static final String CALCULATOR_JOB_NAME = "calculatorJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobCompletionNotificationListener jobExecutionListener;

    public CalculatorJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JobCompletionNotificationListener jobExecutionListener
    ) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobExecutionListener = jobExecutionListener;
    }

    /**
     * Declara o Job Calculador vinculando o listener e o step de cálculo.
     *
     * @param calculatorStep Step contendo a lógica de cálculo encapsulada em {@link StepCalculator}.
     * @return Instância configurada do {@link Job}.
     */
    @Bean(name = CALCULATOR_JOB_NAME)
    public Job calculatorJob(Step calculatorStep) {
        return new JobBuilder(CALCULATOR_JOB_NAME, jobRepository)
                .listener(jobExecutionListener)
                .start(calculatorStep)
                .build();
    }

    /**
     * Declara o step de execução delegando a lógica para a classe {@link StepCalculator}.
     *
     * @param stepCalculator Componente contendo a implementação do {@link org.springframework.batch.core.step.tasklet.Tasklet}.
     * @return Instância configurada do {@link Step}.
     */
    @Bean
    public Step calculatorStep(StepCalculator stepCalculator) {
        return new StepBuilder("calculatorStep", jobRepository)
                .tasklet(stepCalculator, transactionManager)
                .build();
    }
}
