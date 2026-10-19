package com.example.api_docker.infra.batch.job.collector.client.bcb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

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
) {}
