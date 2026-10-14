package com.example.api_docker.infra.batch.job.collector.client;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente para coleta de dados brutos provenientes de fontes externas (APIs de parceiros, SFTP, etc.).
 * <p>
 * Serve como ponto de extensão para ingestão de dados e como referência arquitetural
 * de dependência injetada no {@link com.example.api_docker.infra.batch.job.collector.step.StepCollector},
 * demonstrando como simular serviços externos nos testes com {@code @MockitoBean}.
 */
@Component
public class ExternalDataCollectorClient {

    /**
     * Simula a busca de registros em uma API ou serviço externo.
     *
     * @param origin Identificador da fonte de dados de origem.
     * @return Lista de payloads de dados coletados.
     */
    public List<String> fetchExternalData(String origin) {
        // Ponto de integração real com WebClient, RestClient, SDK externo, etc.
        return List.of("payload-exemplo-1", "payload-exemplo-2");
    }
}
