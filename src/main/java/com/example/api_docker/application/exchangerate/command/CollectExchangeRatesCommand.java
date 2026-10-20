package com.example.api_docker.application.exchangerate.command;

import com.example.api_docker.domain.exchangerate.Currency;

import java.time.LocalDate;

public record CollectExchangeRatesCommand(
        Currency currency,
        LocalDate startDate,
        LocalDate endDate
) {}
