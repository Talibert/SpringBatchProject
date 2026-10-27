package com.example.api_docker.application.exchangerate.query;

import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;

import java.time.LocalDate;

public record ListExchangeRatesQuery(
        Currency currency,
        LocalDate startDate,
        LocalDate endDate,
        PaginationRequest pagination
) {
}
