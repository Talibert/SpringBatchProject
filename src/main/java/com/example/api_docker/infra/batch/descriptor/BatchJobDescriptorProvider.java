package com.example.api_docker.infra.batch.descriptor;

/**
 * Contrato para fornecimento declarativo e descentralizado de metadados de Jobs.
 * <p>
 * Cada novo Job criado no projeto deve implementar esta interface anotada com {@code @Component}
 * dentro do seu próprio pacote funcional. O Spring descobre todos os providers automaticamente
 * via component scan, tornando a adição de novos jobs totalmente desacoplada de catálogos centrais.
 */
public interface BatchJobDescriptorProvider {

    /**
     * Retorna a definição imutável dos metadados e dependências do Job.
     *
     * @return {@link JobDescriptor} preenchido.
     */
    JobDescriptor getDescriptor();
}
