package com.example.api_docker.infra.persistence.exchangerate;

import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateFilter;
import com.example.api_docker.domain.exchangerate.ExchangeRateId;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "referenceDate",
            "quoteTimestamp",
            "buyRate",
            "sellRate",
            "currency",
            "createdAt"
    );

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

    @Override
    public PageResult<ExchangeRate> findAll(ExchangeRateFilter filter, PaginationRequest pagination) {
        ExchangeRateFilter safeFilter = filter != null ? filter : ExchangeRateFilter.empty();
        Specification<ExchangeRateJpaEntity> specification = buildSpecification(safeFilter);
        Pageable pageable = buildPageable(pagination);

        Page<ExchangeRateJpaEntity> entityPage = exchangeRateJpaRepository.findAll(specification, pageable);

        List<ExchangeRate> items = entityPage.getContent().stream()
                .map(this::toDomain)
                .toList();

        return new PageResult<>(
                items,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    private Specification<ExchangeRateJpaEntity> buildSpecification(ExchangeRateFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.currency() != null) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), filter.currency()));
            }
            if (filter.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("referenceDate"), filter.startDate()));
            }
            if (filter.endDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("referenceDate"), filter.endDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(PaginationRequest pagination) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(pagination.sortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String sortField = pagination.sortBy();
        if (sortField == null || sortField.isBlank() || !ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "quoteTimestamp";
        }

        Sort sort = Sort.by(direction, sortField);
        return PageRequest.of(pagination.page(), pagination.size(), sort);
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
