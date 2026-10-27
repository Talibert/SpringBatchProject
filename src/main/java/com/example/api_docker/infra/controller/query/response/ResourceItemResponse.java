package com.example.api_docker.infra.controller.query.response;

/**
 * Interface marcadora para todos os DTOs de itens retornados pelo endpoint unificado de consulta
 * ({@code GET /query/{resource}}).
 * <p>
 * Garante em tempo de compilação que apenas objetos de resposta válidos da camada de apresentação
 * possam ser expostos por implementações de {@code ResourceQueryProvider}.
 */
public interface ResourceItemResponse {
}
