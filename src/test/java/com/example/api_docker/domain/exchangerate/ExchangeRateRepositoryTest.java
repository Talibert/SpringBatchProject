package com.example.api_docker.domain.exchangerate;

import com.example.api_docker.RepositoryAbstractTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateRepositoryTest extends RepositoryAbstractTests {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Test
    @DisplayName("Deve salvar e buscar cotação pelo ID")
    void shouldSaveAndFindExchangeRateById() {
        LocalDateTime quoteTimestamp = LocalDateTime.of(2026, 10, 1, 13, 10, 0);
        ExchangeRate exchangeRate = ExchangeRate.create(
                Currency.USD,
                quoteTimestamp,
                new BigDecimal("5.4510"),
                new BigDecimal("5.4516")
        );

        exchangeRateRepository.save(exchangeRate);

        Optional<ExchangeRate> found = exchangeRateRepository.findById(exchangeRate.getId());

        assertTrue(found.isPresent());
        assertEquals(exchangeRate.getId(), found.get().getId());
        assertEquals(Currency.USD, found.get().getCurrency());
        assertEquals(LocalDate.of(2026, 10, 1), found.get().getReferenceDate());
        assertEquals(quoteTimestamp, found.get().getQuoteTimestamp());
        assertEquals(0, new BigDecimal("5.4510").compareTo(found.get().getBuyRate()));
        assertEquals(0, new BigDecimal("5.4516").compareTo(found.get().getSellRate()));
    }

    @Test
    @DisplayName("Deve verificar existência por moeda e quoteTimestamp")
    void shouldCheckExistenceByCurrencyAndQuoteTimestamp() {
        LocalDateTime quoteTimestamp = LocalDateTime.of(2026, 10, 2, 13, 10, 0);
        ExchangeRate exchangeRate = ExchangeRate.create(
                Currency.USD,
                quoteTimestamp,
                new BigDecimal("5.4610"),
                new BigDecimal("5.4616")
        );

        exchangeRateRepository.save(exchangeRate);

        assertTrue(exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(Currency.USD, quoteTimestamp));
        assertFalse(exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(Currency.EUR, quoteTimestamp));
        assertFalse(exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(Currency.USD, quoteTimestamp.plusMinutes(1)));
    }

    @Test
    @DisplayName("Deve salvar lista e buscar cotações por período de data de referência")
    void shouldSaveAllAndFindByCurrencyAndReferenceDateBetween() {
        ExchangeRate rate1 = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 9, 10, 13, 0, 0),
                new BigDecimal("5.1000"),
                new BigDecimal("5.1010")
        );
        ExchangeRate rate2 = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 9, 11, 13, 0, 0),
                new BigDecimal("5.1200"),
                new BigDecimal("5.1210")
        );
        ExchangeRate rateOut = ExchangeRate.create(
                Currency.USD,
                LocalDateTime.of(2026, 9, 20, 13, 0, 0),
                new BigDecimal("5.2000"),
                new BigDecimal("5.2010")
        );

        exchangeRateRepository.saveAll(List.of(rate1, rate2, rateOut));

        List<ExchangeRate> results = exchangeRateRepository.findByCurrencyAndReferenceDateBetween(
                Currency.USD,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15)
        );

        assertEquals(2, results.size());
        assertEquals(LocalDate.of(2026, 9, 10), results.get(0).getReferenceDate());
        assertEquals(LocalDate.of(2026, 9, 11), results.get(1).getReferenceDate());
    }
}
