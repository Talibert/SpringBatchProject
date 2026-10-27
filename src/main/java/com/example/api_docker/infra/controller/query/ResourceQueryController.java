package com.example.api_docker.infra.controller.query;

import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import com.example.api_docker.infra.controller.query.provider.ResourceQueryProvider;
import com.example.api_docker.infra.controller.query.request.ResourceQueryFilterRequest;
import com.example.api_docker.infra.controller.query.response.ResourceItemResponse;
import com.example.api_docker.infra.controller.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "Consultas de Coletas", description = "Endpoint unificado e paginado para consulta de recursos coletados pelos jobs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/query")
public class ResourceQueryController {

    private static final Set<String> RESERVED_QUERY_PARAMS = Set.of(
            "page",
            "size",
            "sortBy",
            "sortDirection",
            "startDate",
            "endDate",
            "search"
    );

    private final Map<String, ResourceQueryProvider<? extends ResourceItemResponse>> providersByResource;

    public ResourceQueryController(List<ResourceQueryProvider<? extends ResourceItemResponse>> providers) {
        this.providersByResource = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        ResourceQueryProvider::resourceName,
                        Function.identity()
                ));
    }

    @Operation(
            summary = "Listar recursos de consulta disponíveis",
            description = "Retorna os identificadores de todos os recursos coletados que podem ser consultados via GET /query/{resource}."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de recursos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/resources")
    public ResponseEntity<List<String>> listAvailableResources() {
        List<String> resources = providersByResource.keySet().stream()
                .sorted()
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(
            summary = "Consultar dados coletados de um recurso com paginação e filtros",
            description = "Endpoint genérico que delega a consulta ao provedor do recurso informado (ex: exchange-rates) " +
                    "e retorna um PageResponse<T> tipado para consumo no frontend."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de resultados retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de filtro inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Recurso de consulta não encontrado")
    })
    @GetMapping("/{resource}")
    public ResponseEntity<PageResponse<? extends ResourceItemResponse>> queryResource(
            @Parameter(description = "Nome do recurso coletado", example = "exchange-rates")
            @PathVariable String resource,
            @Parameter(description = "Número da página (iniciando em 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade de registros por página", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenação", example = "quoteTimestamp")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Direção da ordenação (ASC ou DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(description = "Data inicial de referência (ISO yyyy-MM-dd)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data final de referência (ISO yyyy-MM-dd)", example = "2026-10-26")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Código ou termo de busca rápida (ex: USD)", example = "USD")
            @RequestParam(required = false) String search,
            @Parameter(hidden = true)
            @RequestParam Map<String, String> allQueryParams
    ) {
        ResourceQueryProvider<? extends ResourceItemResponse> provider = providersByResource.get(resource);
        if (provider == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, String> extraParams = extractExtraParams(allQueryParams);
        PaginationRequest pagination = PaginationRequest.of(page, size, sortBy, sortDirection);
        ResourceQueryFilterRequest filter = ResourceQueryFilterRequest.of(
                startDate,
                endDate,
                search,
                extraParams,
                pagination
        );

        PageResponse<? extends ResourceItemResponse> response = provider.query(filter);
        return ResponseEntity.ok(response);
    }

    private Map<String, String> extractExtraParams(Map<String, String> allQueryParams) {
        if (allQueryParams == null || allQueryParams.isEmpty()) {
            return Map.of();
        }
        Map<String, String> extra = new HashMap<>();
        for (Map.Entry<String, String> entry : allQueryParams.entrySet()) {
            if (!RESERVED_QUERY_PARAMS.contains(entry.getKey())) {
                extra.put(entry.getKey(), entry.getValue());
            }
        }
        return extra;
    }
}
