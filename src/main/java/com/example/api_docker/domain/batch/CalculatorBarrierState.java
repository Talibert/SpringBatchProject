package com.example.api_docker.domain.batch;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsula o estado transitório em memória e o controle de concorrência de uma barreira
 * de sincronização ({@link CalculatorBarrierKey}) enquanto os Jobs Coletores dependentes
 * estão em execução.
 * <p>
 * <b>Por que esta classe existe?</b>
 * No Spring Batch, o listener {@code afterJob} (que publica o evento de conclusão no Kafka)
 * é executado <i>antes</i> do commit final do status {@code COMPLETED} no {@code JobRepository}.
 * Quando dois ou mais coletores terminam simultaneamente em threads paralelas:
 * <ol>
 *   <li>Sem este estado em memória, um coletor consultaria o banco e ainda veria o outro como {@code STARTED},
 *       fazendo com que nenhum dos dois disparasse o Calculador.</li>
 *   <li>Se ambos já tivessem comitado no banco ao mesmo tempo, ambos poderiam disparar o Calculador em duplicidade.</li>
 * </ol>
 * <p>
 * A própria instância de {@code CalculatorBarrierState} atua como monitor de sincronização ({@code synchronized}),
 * garantindo que a chegada de cada coletor, a checagem de pendências e a marcação de disparo ({@code triggered})
 * ocorram de forma atômica para aquela barreira.
 */
public class CalculatorBarrierState {

    /**
     * Conjunto de nomes dos Jobs Coletores que já emitiram evento de conclusão para esta barreira.
     */
    private final Set<String> completedCollectors = new HashSet<>();

    /**
     * Flag que garante que o Job Calculador seja disparado no máximo uma única vez por ciclo de barreira,
     * mesmo que eventos concorrentes cheguem simultaneamente.
     */
    private boolean triggered = false;

    /**
     * Registra que um Job Coletor dependente concluiu sua execução com sucesso.
     *
     * @param collectorJobName Nome do coletor concluído.
     */
    public synchronized void registerCompletedCollector(String collectorJobName) {
        if (collectorJobName != null && !collectorJobName.isBlank()) {
            completedCollectors.add(collectorJobName);
        }
    }

    /**
     * Retorna uma cópia imutável dos coletores que já concluíram nesta barreira.
     */
    public synchronized Set<String> getCompletedCollectors() {
        return Collections.unmodifiableSet(new HashSet<>(completedCollectors));
    }

    /**
     * Indica se o Job Calculador alvo já foi disparado por esta barreira.
     */
    public synchronized boolean isTriggered() {
        return triggered;
    }

    /**
     * Marca a barreira como disparada e limpa o conjunto transitório de coletores acumulados.
     */
    public synchronized void markTriggered() {
        this.triggered = true;
        this.completedCollectors.clear();
    }

    /**
     * Desfaz a marcação de disparo (utilizado em caso de falha ao acionar o JobLauncher,
     * permitindo nova tentativa).
     */
    public synchronized void revertTrigger() {
        this.triggered = false;
    }
}
