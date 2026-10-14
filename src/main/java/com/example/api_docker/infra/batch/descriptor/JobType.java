package com.example.api_docker.infra.batch.descriptor;

/**
 * Classificação funcional de um Job no ecossistema de processamento em lote.
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
