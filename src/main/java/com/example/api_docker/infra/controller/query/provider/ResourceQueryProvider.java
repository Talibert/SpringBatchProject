package com.example.api_docker.infra.controller.query.provider;

import com.example.api_docker.infra.controller.query.request.ResourceQueryFilterRequest;
import com.example.api_docker.infra.controller.response.PageResponse;

/**
 * Contrato Strategy para provedores de consulta paginada de recursos coletados.
 * <p>
 * Cada novo recurso coletado (ex: {@code exchange-rates}, {@code selic-rates}) deve
 * implementar esta interface anotada com {@code @Component}. O Spring descobre automaticamente
 * todos os provedores sem necessidade de criar novos endpoints ou alterar o controller central.
 *
 * @param <T> Tipo do DTO de resposta ({@code *Response}) retornado nos itens da página.
 */
public interface ResourceQueryProvider<T> {

    /**
     * Identificador único do recurso na URL (ex: {@code "exchange-rates"}).
     */
    String resourceName();

    /**
     * Executa a consulta filtrada e paginada para o recurso.
     */
    PageResponse<T> query(ResourceQueryFilterRequest filter);
}
