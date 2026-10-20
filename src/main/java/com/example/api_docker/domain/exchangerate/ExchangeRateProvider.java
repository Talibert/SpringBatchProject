package com.example.api_docker.domain.exchangerate;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateProvider {
    List<ExchangeRate> fetchExchangeRates(Currency currency, LocalDate startDate, LocalDate endDate);
}
