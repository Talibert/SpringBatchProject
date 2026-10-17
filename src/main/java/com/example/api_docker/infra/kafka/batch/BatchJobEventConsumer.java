package com.example.api_docker.infra.kafka.batch;

import com.example.api_docker.domain.batch.event.BatchJobCompletedEvent;
import com.example.api_docker.infra.batch.service.BatchOrchestratorService;
import com.example.api_docker.infra.kafka.KafkaConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka responsável por escutar notificações de término de jobs de lote (coletores e calculadores).
 * <p>
 * O evento é capturado para fins de auditoria e resposta ao frontend.
 * O plus de checar o calculador alvo só é acionado se o job finalizado for um Coletor
 * decorrente da dependência de um Calculador (com {@code targetCalculatorJob} preenchido).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class BatchJobEventConsumer extends KafkaConsumer {

    private final BatchOrchestratorService orchestratorService;

    /**
     * Escuta a finalização de qualquer job de lote.
     *
     * @param event Evento contendo os metadados do job finalizado.
     */
    @KafkaListener(topics = "batch.job.completed", groupId = "${spring.kafka.consumer.group-id:my-group}")
    public void onJobCompleted(BatchJobCompletedEvent event) {
        String targetJob = event.hasTargetContext() ? event.targetContext().targetJobName() : null;
        log.info("📢 [KAFKA BATCH CONSUMER] Job finalizado! Job=[{}], Tipo=[{}], DataRef=[{}], Status=[{}], Target=[{}]",
                event.jobName(), event.jobType(), event.dataReferencia(), event.status(), targetJob);

        // Se disparamos APENAS o coletor (sem targetContext), ele NÃO checa calculadores.
        // O plus de checar o target SÓ ocorre se o coletor vier da dependência de um calculador.
        if (event.isCollector() && event.hasTargetContext()) {
            orchestratorService.onCollectorCompleted(event.jobName(), event.dataReferencia(), event.targetContext());
        } else {
            log.debug("Job [{}] finalizado sem target context. Nenhuma checagem de orquestração necessária.", event.jobName());
        }
    }
}
