package com.example.api_docker.infra.batch.descriptor;

import com.example.api_docker.domain.batch.JobType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metadados declarativos de um Job da aplicação.
 * <p>
 * Permite ao orquestrador inspecionar dinamicamente o catálogo de jobs registrados
 * sem acoplamento a métodos específicos na camada de lançamento.
 *
 * @param jobName              Nome lógico e identificador único do Job no Spring Batch.
 * @param type                 Classificação funcional ({@link JobType#COLLECTOR} ou {@link JobType#CALCULATOR}).
 * @param dependsOnJobNames    Lista de nomes de jobs coletores que devem ter sido executados previamente.
 */
public record JobDescriptor(
        String jobName,
        JobType type,
        List<String> dependsOnJobNames
) {
    public JobDescriptor {
        Objects.requireNonNull(jobName, "jobName não pode ser nulo");
        Objects.requireNonNull(type, "type não pode ser nulo");
        dependsOnJobNames = dependsOnJobNames != null ? List.copyOf(dependsOnJobNames) : Collections.emptyList();
    }

    /**
     * Construtor de conveniência para jobs sem dependências (ex: jobs coletores).
     */
    public JobDescriptor(String jobName, JobType type) {
        this(jobName, type, Collections.emptyList());
    }

    /**
     * Verifica se o job possui dependências de outros jobs para ser executado.
     */
    public boolean hasDependencies() {
        return !dependsOnJobNames.isEmpty();
    }
}
