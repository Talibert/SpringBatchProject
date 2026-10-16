package com.example.api_docker.infra.kafka.batch;

import com.example.api_docker.domain.batch.event.CollectorCompletedEvent;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import com.example.api_docker.infra.kafka.KafkaConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka responsável por escutar notificações de jobs de lote concluídos
 * e acionar a sincronização / destravamento de dependências no orquestrador.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class BatchJobEventConsumer extends KafkaConsumer {

    private final BatchOrchestratorService orchestratorService;

    /**
     * Escuta o término de jobs coletores para avaliar se os calculadores dependentes podem ser disparados.
     *
     * @param event Evento contendo os metadados do coletor finalizado e a data de referência.
     */
    @KafkaListener(topics = "batch.collector.completed", groupId = "${spring.kafka.consumer.group-id:my-group}")
    public void onCollectorCompleted(CollectorCompletedEvent event) {
        log.info("📢 [KAFKA BATCH CONSUMER] Evento recebido: Coletor finalizado! Job=[{}], DataRef=[{}], Status=[{}]",
                event.jobName(), event.dataReferencia(), event.status());

        orchestratorService.onCollectorCompleted(event.jobName(), event.dataReferencia());
    }
}
