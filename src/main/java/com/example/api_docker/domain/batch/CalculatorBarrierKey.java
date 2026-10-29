package com.example.api_docker.domain.batch;

import com.example.api_docker.domain.shared.exception.DomainException;

/**
 * Value Object que identifica de forma única uma barreira de sincronização (Join)
 * para o disparo de um Job Calculador em uma determinada data de referência.
 * <p>
 * Substitui o uso de chaves primitivas concatenadas em texto (ex: {@code "calculador|2026-10-15"}),
 * garantindo imutabilidade, validação de invariantes e implementação nativa de {@code equals}
 * e {@code hashCode} para uso seguro como chave em mapas concorrentes.
 *
 * @param calculatorJobName Nome lógico do Job Calculador que aguarda suas dependências.
 * @param dataReferencia    Data de corte/referência dos dados para o cálculo.
 */
public record CalculatorBarrierKey(
        String calculatorJobName,
        String dataReferencia
) {
    public CalculatorBarrierKey {
        if (calculatorJobName == null || calculatorJobName.isBlank()) {
            throw new DomainException("Nome do job calculador da barreira não pode ser nulo ou vazio");
        }
        if (dataReferencia == null || dataReferencia.isBlank()) {
            throw new DomainException("Data de referência da barreira não pode ser nula ou vazia");
        }
    }

    public static CalculatorBarrierKey of(String calculatorJobName, String dataReferencia) {
        return new CalculatorBarrierKey(calculatorJobName, dataReferencia);
    }
}
