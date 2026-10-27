package com.example.api_docker.domain.exchangerate;

import java.time.LocalDate;

public record ExchangeRateFilter(
        Currency currency,
        LocalDate startDate,
        LocalDate endDate
) {
    public static ExchangeRateFilter empty() {
        return new ExchangeRateFilter(null, null, null);
    }
}
