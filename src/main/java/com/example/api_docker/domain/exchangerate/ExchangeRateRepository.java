package com.example.api_docker.domain.exchangerate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository {
    void save(ExchangeRate exchangeRate);
    void saveAll(List<ExchangeRate> exchangeRates);
    Optional<ExchangeRate> findById(ExchangeRateId id);
    boolean existsByCurrencyAndQuoteTimestamp(Currency currency, LocalDateTime quoteTimestamp);
    List<ExchangeRate> findByCurrencyAndReferenceDateBetween(Currency currency, LocalDate startDate, LocalDate endDate);
}
