package com.example.api_docker.infra.controller.query.response;

import com.example.api_docker.application.exchangerate.result.ExchangeRateResult;
import com.example.api_docker.domain.exchangerate.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados de uma cotação de câmbio coletada")
public record ExchangeRateResponse(
        @Schema(description = "Identificador único da cotação", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Moeda da cotação", example = "USD")
        Currency currency,

        @Schema(description = "Data de referência da cotação", example = "2026-10-01")
        LocalDate referenceDate,

        @Schema(description = "Data e hora exata do boletim da cotação", example = "2026-10-01T13:05:12.345")
        LocalDateTime quoteTimestamp,

        @Schema(description = "Taxa de compra", example = "5.4510")
        BigDecimal buyRate,

        @Schema(description = "Taxa de venda", example = "5.4516")
        BigDecimal sellRate,

        @Schema(description = "Data e hora em que o registro foi salvo", example = "2026-10-01T14:00:00")
        LocalDateTime createdAt
) implements ResourceItemResponse {
    public static ExchangeRateResponse from(ExchangeRateResult result) {
        return new ExchangeRateResponse(
                result.id(),
                result.currency(),
                result.referenceDate(),
                result.quoteTimestamp(),
                result.buyRate(),
                result.sellRate(),
                result.createdAt()
        );
    }
}
