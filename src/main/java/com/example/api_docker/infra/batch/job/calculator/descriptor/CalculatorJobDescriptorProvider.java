package com.example.api_docker.infra.batch.job.calculator.descriptor;

import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.job.calculator.CalculatorJobConfig;
import com.example.api_docker.infra.batch.job.collector.UsdCollectorJobConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provedor de metadados do Job Calculador base da aplicação.
 * <p>
 * Declara as dependências de coleta necessárias para que este cálculo execute com integridade.
 * Descoberto automaticamente pelo Spring via component scan.
 */
@Component
public class CalculatorJobDescriptorProvider implements BatchJobDescriptorProvider {

    @Override
    public JobDescriptor getDescriptor() {
        return new JobDescriptor(
                CalculatorJobConfig.CALCULATOR_JOB_NAME,
                JobType.CALCULATOR,
                List.of(UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME)
        );
    }
}
