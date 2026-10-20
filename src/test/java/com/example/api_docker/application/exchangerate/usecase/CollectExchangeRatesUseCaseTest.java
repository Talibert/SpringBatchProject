package com.example.api_docker.application.exchangerate.usecase;

import com.example.api_docker.UnitAbstractTests;
import com.example.api_docker.application.exchangerate.command.CollectExchangeRatesCommand;
import com.example.api_docker.application.exchangerate.result.CollectExchangeRatesResult;
import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateProvider;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.domain.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectExchangeRatesUseCaseTest extends UnitAbstractTests {

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private CollectExchangeRatesUseCase collectExchangeRatesUseCase;

    @Test
    @DisplayName("Deve coletar cotações do provedor e salvar apenas as que ainda não existem no banco")
    void shouldCollectAndSaveOnlyNewExchangeRates() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 2);

        ExchangeRate existingRate = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 10, 1, 13, 5, 0),
                new BigDecimal("5.4510"),
                new BigDecimal("5.4516")
        );
        ExchangeRate newRate = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 10, 2, 13, 5, 0),
                new BigDecimal("5.4620"),
                new BigDecimal("5.4628")
        );

        when(exchangeRateProvider.fetchExchangeRates(Currency.USD, startDate, endDate))
                .thenReturn(List.of(existingRate, newRate));

        when(exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(Currency.USD, existingRate.getQuoteTimestamp()))
                .thenReturn(true);
        when(exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(Currency.USD, newRate.getQuoteTimestamp()))
                .thenReturn(false);

        CollectExchangeRatesCommand command = new CollectExchangeRatesCommand(Currency.USD, startDate, endDate);
        CollectExchangeRatesResult result = collectExchangeRatesUseCase.execute(command);

        assertEquals(2, result.fetchedCount());
        assertEquals(1, result.savedCount());

        verify(exchangeRateRepository, times(1)).saveAll(List.of(newRate));
    }

    @Test
    @DisplayName("Não deve chamar saveAll quando todas as cotações já existirem no banco")
    void shouldNotCallSaveAllWhenAllRatesAlreadyExist() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 1);

        ExchangeRate existingRate = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 10, 1, 13, 5, 0),
                new BigDecimal("5.4510"),
                new BigDecimal("5.4516")
        );

        when(exchangeRateProvider.fetchExchangeRates(Currency.USD, startDate, endDate))
                .thenReturn(List.of(existingRate));
        when(exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(Currency.USD, existingRate.getQuoteTimestamp()))
                .thenReturn(true);

        CollectExchangeRatesCommand command = new CollectExchangeRatesCommand(Currency.USD, startDate, endDate);
        CollectExchangeRatesResult result = collectExchangeRatesUseCase.execute(command);

        assertEquals(1, result.fetchedCount());
        assertEquals(0, result.savedCount());

        verify(exchangeRateRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando data inicial for maior que data final")
    void shouldThrowDomainExceptionWhenStartDateIsAfterEndDate() {
        CollectExchangeRatesCommand command = new CollectExchangeRatesCommand(
                Currency.USD,
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 1)
        );

        assertThrows(DomainException.class, () -> collectExchangeRatesUseCase.execute(command));
    }
}
