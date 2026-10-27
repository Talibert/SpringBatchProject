package com.example.api_docker.domain.exchangerate;

import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;

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
    PageResult<ExchangeRate> findAll(ExchangeRateFilter filter, PaginationRequest pagination);
}
