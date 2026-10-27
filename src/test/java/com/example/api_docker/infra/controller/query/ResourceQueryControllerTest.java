package com.example.api_docker.infra.controller.query;

import com.example.api_docker.ControllerAbstractTests;
import com.example.api_docker.application.exchangerate.query.ListExchangeRatesQuery;
import com.example.api_docker.application.exchangerate.result.ExchangeRateResult;
import com.example.api_docker.application.exchangerate.usecase.ListExchangeRatesUseCase;
import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.infra.controller.query.provider.ExchangeRateQueryProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceQueryController.class)
@Import(ExchangeRateQueryProvider.class)
class ResourceQueryControllerTest extends ControllerAbstractTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListExchangeRatesUseCase listExchangeRatesUseCase;

    @Test
    @DisplayName("Deve listar os recursos de consulta registrados")
    void shouldListAvailableResources() throws Exception {
        mockMvc.perform(get("/query/resources")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("exchange-rates"));
    }

    @Test
    @DisplayName("Deve retornar página tipada de cotações de câmbio via endpoint genérico /query/exchange-rates")
    void shouldReturnPaginatedExchangeRates() throws Exception {
        UUID rateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        ExchangeRateResult rateResult = new ExchangeRateResult(
                rateId,
                Currency.USD,
                LocalDate.of(2026, 10, 1),
                LocalDateTime.of(2026, 10, 1, 13, 5, 12),
                new BigDecimal("5.4510"),
                new BigDecimal("5.4516"),
                LocalDateTime.of(2026, 10, 1, 14, 0, 0)
        );

        PageResult<ExchangeRateResult> pageResult = new PageResult<>(
                List.of(rateResult),
                0,
                10,
                1L,
                1
        );

        when(listExchangeRatesUseCase.execute(any(ListExchangeRatesQuery.class))).thenReturn(pageResult);

        mockMvc.perform(get("/query/exchange-rates")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "quoteTimestamp")
                        .param("sortDirection", "DESC")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-02")
                        .param("currency", "USD")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(rateId.toString()))
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.content[0].referenceDate").value("2026-10-01"))
                .andExpect(jsonPath("$.content[0].buyRate").value(5.4510))
                .andExpect(jsonPath("$.content[0].sellRate").value(5.4516))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.isFirst").value(true))
                .andExpect(jsonPath("$.isLast").value(true));

        ArgumentCaptor<ListExchangeRatesQuery> queryCaptor = ArgumentCaptor.forClass(ListExchangeRatesQuery.class);
        verify(listExchangeRatesUseCase).execute(queryCaptor.capture());

        ListExchangeRatesQuery captured = queryCaptor.getValue();
        assertEquals(Currency.USD, captured.currency());
        assertEquals(LocalDate.of(2026, 10, 1), captured.startDate());
        assertEquals(LocalDate.of(2026, 10, 2), captured.endDate());
        assertEquals(0, captured.pagination().page());
        assertEquals(10, captured.pagination().size());
        assertEquals("quoteTimestamp", captured.pagination().sortBy());
        assertEquals("DESC", captured.pagination().sortDirection());
    }

    @Test
    @DisplayName("Deve retornar 404 quando o recurso consultado não existir")
    void shouldReturn404WhenResourceDoesNotExist() throws Exception {
        mockMvc.perform(get("/query/recurso-inexistente")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 422 quando a moeda informada for inválida")
    void shouldReturn422WhenCurrencyIsInvalid() throws Exception {
        mockMvc.perform(get("/query/exchange-rates")
                        .param("currency", "INVALID_COIN")
                        .header("Authorization", "Bearer " + tokenDoUser))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @DisplayName("Deve retornar 401 quando não enviar token JWT")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/query/exchange-rates"))
                .andExpect(status().isUnauthorized());
    }
}
