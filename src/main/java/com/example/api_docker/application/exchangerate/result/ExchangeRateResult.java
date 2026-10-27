package com.example.api_docker.application.exchangerate.result;

import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeRateResult(
        UUID id,
        Currency currency,
        LocalDate referenceDate,
        LocalDateTime quoteTimestamp,
        BigDecimal buyRate,
        BigDecimal sellRate,
        LocalDateTime createdAt
) {
    public static ExchangeRateResult from(ExchangeRate exchangeRate) {
        return new ExchangeRateResult(
                exchangeRate.getId().value(),
                exchangeRate.getCurrency(),
                exchangeRate.getReferenceDate(),
                exchangeRate.getQuoteTimestamp(),
                exchangeRate.getBuyRate(),
                exchangeRate.getSellRate(),
                exchangeRate.getCreatedAt()
        );
    }
}
