package com.example.api_docker.domain.batch;

import com.example.api_docker.domain.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorBarrierTest {

    @Test
    @DisplayName("Deve criar CalculatorBarrierKey válido e garantir igualdade por valor")
    void shouldCreateValidBarrierKeyAndCheckEquality() {
        CalculatorBarrierKey key1 = CalculatorBarrierKey.of("calculadorGeral", "2026-10-20");
        CalculatorBarrierKey key2 = CalculatorBarrierKey.of("calculadorGeral", "2026-10-20");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    @DisplayName("Deve lançar DomainException ao criar CalculatorBarrierKey com valores nulos ou vazios")
    void shouldThrowDomainExceptionWhenBarrierKeyIsInvalid() {
        assertThrows(DomainException.class, () -> CalculatorBarrierKey.of(null, "2026-10-20"));
        assertThrows(DomainException.class, () -> CalculatorBarrierKey.of("  ", "2026-10-20"));
        assertThrows(DomainException.class, () -> CalculatorBarrierKey.of("calculadorGeral", null));
        assertThrows(DomainException.class, () -> CalculatorBarrierKey.of("calculadorGeral", ""));
    }

    @Test
    @DisplayName("Deve gerenciar ciclo de vida do CalculatorBarrierState: registro, disparo e reversão")
    void shouldManageCalculatorBarrierStateLifecycle() {
        CalculatorBarrierState state = new CalculatorBarrierState();

        assertFalse(state.isTriggered());
        assertTrue(state.getCompletedCollectors().isEmpty());

        state.registerCompletedCollector("coletorA");
        state.registerCompletedCollector("coletorB");
        assertEquals(Set.of("coletorA", "coletorB"), state.getCompletedCollectors());

        // Ao marcar como disparado, ativa a flag e limpa a coleção transitória de coletores
        state.markTriggered();
        assertTrue(state.isTriggered());
        assertTrue(state.getCompletedCollectors().isEmpty());

        // Em caso de erro no disparo, permite reverter
        state.revertTrigger();
        assertFalse(state.isTriggered());
    }
}
