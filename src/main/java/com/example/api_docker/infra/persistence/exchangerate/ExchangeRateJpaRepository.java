package com.example.api_docker.infra.persistence.exchangerate;

import com.example.api_docker.domain.exchangerate.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateJpaEntity, UUID> {
    boolean existsByCurrencyAndQuoteTimestamp(Currency currency, LocalDateTime quoteTimestamp);
    List<ExchangeRateJpaEntity> findByCurrencyAndReferenceDateBetweenOrderByQuoteTimestampAsc(
            Currency currency,
            LocalDate startDate,
            LocalDate endDate
    );
}
