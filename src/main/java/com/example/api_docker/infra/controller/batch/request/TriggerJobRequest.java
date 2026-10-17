package com.example.api_docker.infra.controller.batch.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Collections;
import java.util.Map;

@Schema(description = "Dados para solicitação de execução de um Job do Spring Batch")
public record TriggerJobRequest(
        @Schema(description = "Data de referência e corte dos dados a serem processados (formato ISO YYYY-MM-DD)", example = "2026-10-17")
        @NotNull(message = "A dataReferencia é obrigatória")
        @NotBlank(message = "A dataReferencia não pode ser vazia")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "A dataReferencia deve estar no formato YYYY-MM-DD")
        String dataReferencia,

        @Schema(description = "Parâmetros adicionais customizados opcionais para a execução", example = "{\"origem\": \"bacen-cambio\"}")
        Map<String, Object> parametros
) {
    public TriggerJobRequest {
        parametros = (parametros != null) ? Collections.unmodifiableMap(parametros) : Collections.emptyMap();
    }
}
