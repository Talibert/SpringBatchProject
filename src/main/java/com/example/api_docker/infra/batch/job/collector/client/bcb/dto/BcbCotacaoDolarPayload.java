package com.example.api_docker.infra.batch.job.collector.client.bcb.dto;

import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Payload que representa um item individual de cotação do dólar retornado pela API do Banco Central.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BcbCotacaoDolarPayload(
        @JsonProperty("cotacaoCompra")
        BigDecimal cotacaoCompra,

        @JsonProperty("cotacaoVenda")
        BigDecimal cotacaoVenda,

        @JsonProperty("dataHoraCotacao")
        String dataHoraCotacao
) {
    private static final DateTimeFormatter BCB_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
            .optionalEnd()
            .toFormatter();

    /**
     * Converte o payload técnico da API do Banco Central para a entidade de domínio {@link ExchangeRate}.
     */
    public ExchangeRate toDomain() {
        LocalDateTime quoteTimestamp = LocalDateTime.parse(dataHoraCotacao.trim(), BCB_TIMESTAMP_FORMATTER);
        return ExchangeRate.create(
                Currency.USD,
                quoteTimestamp,
                cotacaoCompra,
                cotacaoVenda
        );
    }
}
