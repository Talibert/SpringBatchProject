package com.example.api_docker.domain.batch;

/**
 * Classificação funcional de um Job no ecossistema de processamento em lote.
 * <p>
 * Pertence à camada de Domínio, representando o conceito ubíquo de tipos de processamento:
 * coleta de dados externos (COLLECTOR) ou cálculo e consolidação (CALCULATOR).
 */
public enum JobType {

    /**
     * Job responsável por ingestão e extração de dados brutos de fontes externas.
     */
    COLLECTOR,

    /**
     * Job analítico que atua sobre dados ingeridos para cálculo e consolidação.
     */
    CALCULATOR
}
