package com.example.api_docker.application.exchangerate.usecase;

import com.example.api_docker.application.exchangerate.query.ListExchangeRatesQuery;
import com.example.api_docker.application.exchangerate.result.ExchangeRateResult;
import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateFilter;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.domain.shared.exception.DomainException;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ListExchangeRatesUseCaseTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ListExchangeRatesUseCase useCase;

    @Test
    @DisplayName("Deve listar cotações paginadas e converter para ExchangeRateResult")
    void shouldListExchangeRatesPaginated() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 2);
        PaginationRequest pagination = PaginationRequest.of(0, 10, "quoteTimestamp", "DESC");

        ExchangeRate rate = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 10, 1, 13, 10, 0),
                new BigDecimal("5.4510"),
                new BigDecimal("5.4516")
        );

        PageResult<ExchangeRate> domainPage = new PageResult<>(
                List.of(rate),
                0,
                10,
                1L,
                1
        );

        ExchangeRateFilter expectedFilter = new ExchangeRateFilter(Currency.USD, startDate, endDate);
        Mockito.when(exchangeRateRepository.findAll(expectedFilter, pagination)).thenReturn(domainPage);

        ListExchangeRatesQuery query = new ListExchangeRatesQuery(Currency.USD, startDate, endDate, pagination);
        PageResult<ExchangeRateResult> result = useCase.execute(query);

        assertEquals(1, result.items().size());
        assertEquals(rate.getId().value(), result.items().get(0).id());
        assertEquals(Currency.USD, result.items().get(0).currency());
        assertEquals(new BigDecimal("5.4510"), result.items().get(0).buyRate());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando startDate for posterior a endDate")
    void shouldThrowDomainExceptionWhenStartDateIsAfterEndDate() {
        ListExchangeRatesQuery query = new ListExchangeRatesQuery(
                Currency.USD,
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 1),
                PaginationRequest.of(0, 10)
        );

        assertThrows(DomainException.class, () -> useCase.execute(query));
        Mockito.verifyNoInteractions(exchangeRateRepository);
    }
}
