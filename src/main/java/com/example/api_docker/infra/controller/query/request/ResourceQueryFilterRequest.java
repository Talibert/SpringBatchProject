package com.example.api_docker.infra.controller.query.request;

import com.example.api_docker.domain.shared.pagination.PaginationRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

/**
 * Filtro genérico de consulta recebido pelo endpoint unificado de dados coletados.
 *
 * @param startDate         Data inicial opcional do período de referência.
 * @param endDate           Data final opcional do período de referência.
 * @param search            Código, identificador ou termo de filtro principal opcional (ex: "USD").
 * @param extraParams       Parâmetros de query adicionais específicos de cada recurso.
 * @param pagination        Parâmetros de paginação e ordenação.
 */
public record ResourceQueryFilterRequest(
        LocalDate startDate,
        LocalDate endDate,
        String search,
        Map<String, String> extraParams,
        PaginationRequest pagination
) {
    public String getParam(String key) {
        if (extraParams == null) {
            return null;
        }
        return extraParams.get(key);
    }

    public static ResourceQueryFilterRequest of(
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Map<String, String> extraParams,
            PaginationRequest pagination
    ) {
        Map<String, String> safeParams = extraParams != null
                ? Collections.unmodifiableMap(extraParams)
                : Collections.emptyMap();
        return new ResourceQueryFilterRequest(startDate, endDate, search, safeParams, pagination);
    }
}
