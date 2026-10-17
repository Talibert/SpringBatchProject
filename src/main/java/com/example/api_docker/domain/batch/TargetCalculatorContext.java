package com.example.api_docker.domain.batch;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Value Object do Domínio que encapsula o contexto e parâmetros de execução
 * do Calculador alvo que solicitou o disparo prévio de Jobs Coletores.
 *
 * @param targetJobName Nome do Job Calculador alvo (ex: calculatorJob).
 * @param customParams  Parâmetros adicionais informados na solicitação original de cálculo.
 */
public record TargetCalculatorContext(
        String targetJobName,
        Map<String, Object> customParams
) {

    public TargetCalculatorContext {
        Objects.requireNonNull(targetJobName, "targetJobName não pode ser nulo");
        customParams = customParams != null ? Map.copyOf(customParams) : Collections.emptyMap();
    }

    public static TargetCalculatorContext of(String targetJobName, Map<String, Object> customParams) {
        return new TargetCalculatorContext(targetJobName, customParams);
    }

    public static TargetCalculatorContext of(String targetJobName) {
        return of(targetJobName, Collections.emptyMap());
    }

    public boolean hasCustomParams() {
        return !customParams.isEmpty();
    }
}
