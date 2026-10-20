package com.example.api_docker.infra.persistence.exchangerate;

import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateId;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {

    private final ExchangeRateJpaRepository exchangeRateJpaRepository;

    @Override
    public void save(ExchangeRate exchangeRate) {
        exchangeRateJpaRepository.save(toJpaEntity(exchangeRate));
    }

    @Override
    public void saveAll(List<ExchangeRate> exchangeRates) {
        List<ExchangeRateJpaEntity> entities = exchangeRates.stream()
                .map(this::toJpaEntity)
                .toList();
        exchangeRateJpaRepository.saveAll(entities);
    }

    @Override
    public Optional<ExchangeRate> findById(ExchangeRateId id) {
        return exchangeRateJpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByCurrencyAndQuoteTimestamp(Currency currency, LocalDateTime quoteTimestamp) {
        return exchangeRateJpaRepository.existsByCurrencyAndQuoteTimestamp(currency, quoteTimestamp);
    }

    @Override
    public List<ExchangeRate> findByCurrencyAndReferenceDateBetween(
            Currency currency,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return exchangeRateJpaRepository
                .findByCurrencyAndReferenceDateBetweenOrderByQuoteTimestampAsc(currency, startDate, endDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ExchangeRateJpaEntity toJpaEntity(ExchangeRate exchangeRate) {
        ExchangeRateJpaEntity entity = new ExchangeRateJpaEntity();
        entity.setId(exchangeRate.getId().value());
        entity.setCurrency(exchangeRate.getCurrency());
        entity.setReferenceDate(exchangeRate.getReferenceDate());
        entity.setQuoteTimestamp(exchangeRate.getQuoteTimestamp());
        entity.setBuyRate(exchangeRate.getBuyRate());
        entity.setSellRate(exchangeRate.getSellRate());
        entity.setCreatedAt(exchangeRate.getCreatedAt());
        return entity;
    }

    private ExchangeRate toDomain(ExchangeRateJpaEntity entity) {
        return ExchangeRate.restore(
                new ExchangeRateId(entity.getId()),
                entity.getCurrency(),
                entity.getReferenceDate(),
                entity.getQuoteTimestamp(),
                entity.getBuyRate(),
                entity.getSellRate(),
                entity.getCreatedAt()
        );
    }
}
