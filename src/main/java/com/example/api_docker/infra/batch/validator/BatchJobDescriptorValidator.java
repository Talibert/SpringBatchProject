package com.example.api_docker.infra.batch.validator;

import com.example.api_docker.infra.batch.descriptor.BatchJobDescriptorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validador de integridade executado no ciclo de inicialização do Spring.
 * <p>
 * Garante que todo {@link Job} registrado no contexto da aplicação possua obrigatoriamente
 * uma implementação correspondente de {@link BatchJobDescriptorProvider}.
 * Em caso de ausência, interrompe a inicialização da aplicação (fail-fast) com uma mensagem descritiva.
 */
@Component
public class BatchJobDescriptorValidator implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(BatchJobDescriptorValidator.class);

    private final List<Job> registeredJobs;
    private final List<BatchJobDescriptorProvider> descriptorProviders;

    public BatchJobDescriptorValidator(
            List<Job> registeredJobs,
            List<BatchJobDescriptorProvider> descriptorProviders
    ) {
        this.registeredJobs = registeredJobs != null ? registeredJobs : List.of();
        this.descriptorProviders = descriptorProviders != null ? descriptorProviders : List.of();
    }

    @Override
    public void afterSingletonsInstantiated() {
        Set<String> documentedJobNames = descriptorProviders.stream()
                .map(p -> p.getDescriptor().jobName())
                .collect(Collectors.toSet());

        List<String> missingDescriptors = registeredJobs.stream()
                .map(Job::getName)
                .filter(jobName -> !documentedJobNames.contains(jobName))
                .toList();

        if (!missingDescriptors.isEmpty()) {
            String errorMessage = String.format(
                    "❌ VIOLAÇÃO DE CONTRATO BATCH: Os seguintes Jobs foram registrados no Spring Batch sem um BatchJobDescriptorProvider correspondente: %s. " +
                    "Para garantir a governança e orquestração corretas, implemente um @Component com BatchJobDescriptorProvider para cada Job registrado.",
                    missingDescriptors
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("✅ Integridade do Spring Batch validada com sucesso: todos os {} jobs possuem descritores associados.", registeredJobs.size());
    }
}
