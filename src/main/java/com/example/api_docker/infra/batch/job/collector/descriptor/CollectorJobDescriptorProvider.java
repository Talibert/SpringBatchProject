package com.example.api_docker.infra.batch.job.collector.descriptor;

import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import com.example.api_docker.infra.batch.descriptor.JobDescriptor;
import com.example.api_docker.infra.batch.descriptor.JobType;
import com.example.api_docker.infra.batch.job.collector.CollectorJobConfig;
import org.springframework.stereotype.Component;

/**
 * Provedor de metadados do Job Coletor base da aplicação.
 * <p>
 * Descoberto automaticamente pelo Spring via component scan.
 */
@Component
public class CollectorJobDescriptorProvider implements BatchJobDescriptorProvider {

    @Override
    public JobDescriptor getDescriptor() {
        return new JobDescriptor(CollectorJobConfig.COLLECTOR_JOB_NAME, JobType.COLLECTOR);
    }
}
