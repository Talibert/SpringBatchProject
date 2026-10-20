package com.example.api_docker.infra.batch.job.collector.descriptor;

import com.example.api_docker.domain.batch.JobType;
import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.job.collector.UsdCollectorJobConfig;
import org.springframework.stereotype.Component;

/**
 * Provedor de metadados do Job Coletor de Dólar (USD).
 * <p>
 * Descoberto automaticamente pelo Spring via component scan.
 */
@Component
public class UsdCollectorJobDescriptorProvider implements BatchJobDescriptorProvider {

    @Override
    public JobDescriptor getDescriptor() {
        return new JobDescriptor(UsdCollectorJobConfig.USD_COLLECTOR_JOB_NAME, JobType.COLLECTOR);
    }
}
