package com.example.api_docker.domain.exchangerate;

import com.example.api_docker.UnitAbstractTests;
import com.example.api_docker.domain.shared.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateTest extends UnitAbstractTests {

    @Test
    @DisplayName("Deve criar ExchangeRate com sucesso extraindo referenceDate do quoteTimestamp")
    void shouldCreateExchangeRateSuccessfully() {
        LocalDateTime quoteTimestamp = LocalDateTime.of(2026, 10, 15, 13, 5, 30);
        BigDecimal buyRate = new BigDecimal("5.4510");
        BigDecimal sellRate = new BigDecimal("5.4516");

        ExchangeRate exchangeRate = ExchangeRate.create(
                Currency.USD,
                quoteTimestamp,
                buyRate,
                sellRate
        );

        assertNotNull(exchangeRate.getId());
        assertEquals(Currency.USD, exchangeRate.getCurrency());
        assertEquals(LocalDate.of(2026, 10, 15), exchangeRate.getReferenceDate());
        assertEquals(quoteTimestamp, exchangeRate.getQuoteTimestamp());
        assertEquals(0, buyRate.compareTo(exchangeRate.getBuyRate()));
        assertEquals(0, sellRate.compareTo(exchangeRate.getSellRate()));
        assertNotNull(exchangeRate.getCreatedAt());
    }

    @Test
    @DisplayName("Deve restaurar ExchangeRate com sucesso")
    void shouldRestoreExchangeRateSuccessfully() {
        ExchangeRateId id = ExchangeRateId.generate();
        LocalDate referenceDate = LocalDate.of(2026, 10, 15);
        LocalDateTime quoteTimestamp = LocalDateTime.of(2026, 10, 15, 13, 5, 30);
        BigDecimal buyRate = new BigDecimal("5.4510");
        BigDecimal sellRate = new BigDecimal("5.4516");
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);

        ExchangeRate exchangeRate = ExchangeRate.restore(
                id,
                Currency.USD,
                referenceDate,
                quoteTimestamp,
                buyRate,
                sellRate,
                createdAt
        );

        assertEquals(id, exchangeRate.getId());
        assertEquals(Currency.USD, exchangeRate.getCurrency());
        assertEquals(referenceDate, exchangeRate.getReferenceDate());
        assertEquals(quoteTimestamp, exchangeRate.getQuoteTimestamp());
        assertEquals(buyRate, exchangeRate.getBuyRate());
        assertEquals(sellRate, exchangeRate.getSellRate());
        assertEquals(createdAt, exchangeRate.getCreatedAt());
    }

    @Test
    @DisplayName("Deve lançar DomainException quando parâmetros forem inválidos")
    void shouldThrowDomainExceptionWhenParametersAreInvalid() {
        LocalDateTime validTimestamp = LocalDateTime.of(2026, 10, 15, 13, 5, 30);
        BigDecimal validRate = new BigDecimal("5.4510");

        assertThrows(DomainException.class, () ->
                ExchangeRate.create(null, validTimestamp, validRate, validRate)
        );

        assertThrows(DomainException.class, () ->
                ExchangeRate.create(Currency.USD, null, validRate, validRate)
        );

        assertThrows(DomainException.class, () ->
                ExchangeRate.create(Currency.USD, validTimestamp, null, validRate)
        );

        assertThrows(DomainException.class, () ->
                ExchangeRate.create(Currency.USD, validTimestamp, BigDecimal.ZERO, validRate)
        );

        assertThrows(DomainException.class, () ->
                ExchangeRate.create(Currency.USD, validTimestamp, validRate, new BigDecimal("-1.0"))
        );
    }
}
