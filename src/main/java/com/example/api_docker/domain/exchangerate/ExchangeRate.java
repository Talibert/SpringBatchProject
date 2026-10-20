package com.example.api_docker.domain.exchangerate;

import com.example.api_docker.domain.shared.exception.DomainException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ExchangeRate {

    private final ExchangeRateId id;
    private final Currency currency;
    private final LocalDate referenceDate;
    private final LocalDateTime quoteTimestamp;
    private final BigDecimal buyRate;
    private final BigDecimal sellRate;
    private final LocalDateTime createdAt;

    private ExchangeRate(
            ExchangeRateId id,
            Currency currency,
            LocalDate referenceDate,
            LocalDateTime quoteTimestamp,
            BigDecimal buyRate,
            BigDecimal sellRate,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.currency = currency;
        this.referenceDate = referenceDate;
        this.quoteTimestamp = quoteTimestamp;
        this.buyRate = buyRate;
        this.sellRate = sellRate;
        this.createdAt = createdAt;
    }

    public static ExchangeRate create(
            Currency currency,
            LocalDateTime quoteTimestamp,
            BigDecimal buyRate,
            BigDecimal sellRate
    ) {
        validate(currency, quoteTimestamp, buyRate, sellRate);
        return new ExchangeRate(
                ExchangeRateId.generate(),
                currency,
                quoteTimestamp.toLocalDate(),
                quoteTimestamp,
                buyRate,
                sellRate,
                LocalDateTime.now()
        );
    }

    public static ExchangeRate restore(
            ExchangeRateId id,
            Currency currency,
            LocalDate referenceDate,
            LocalDateTime quoteTimestamp,
            BigDecimal buyRate,
            BigDecimal sellRate,
            LocalDateTime createdAt
    ) {
        return new ExchangeRate(id, currency, referenceDate, quoteTimestamp, buyRate, sellRate, createdAt);
    }

    private static void validate(
            Currency currency,
            LocalDateTime quoteTimestamp,
            BigDecimal buyRate,
            BigDecimal sellRate
    ) {
        if (currency == null) {
            throw new DomainException("Moeda não pode ser nula");
        }
        if (quoteTimestamp == null) {
            throw new DomainException("Data e hora da cotação não pode ser nula");
        }
        if (buyRate == null || buyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Cotação de compra deve ser maior que zero");
        }
        if (sellRate == null || sellRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Cotação de venda deve ser maior que zero");
        }
    }
}
