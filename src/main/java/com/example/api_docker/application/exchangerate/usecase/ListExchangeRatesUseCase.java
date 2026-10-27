package com.example.api_docker.application.exchangerate.usecase;

import com.example.api_docker.application.exchangerate.query.ListExchangeRatesQuery;
import com.example.api_docker.application.exchangerate.result.ExchangeRateResult;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateFilter;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.domain.shared.exception.DomainException;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.domain.shared.pagination.PaginationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ListExchangeRatesUseCase {

    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional(readOnly = true)
    public PageResult<ExchangeRateResult> execute(ListExchangeRatesQuery query) {
        if (query.startDate() != null && query.endDate() != null && query.startDate().isAfter(query.endDate())) {
            throw new DomainException("Data inicial não pode ser posterior à data final");
        }

        ExchangeRateFilter filter = new ExchangeRateFilter(
                query.currency(),
                query.startDate(),
                query.endDate()
        );

        PaginationRequest pagination = query.pagination() != null
                ? query.pagination()
                : PaginationRequest.of(0, 10, "quoteTimestamp", "DESC");

        PageResult<ExchangeRate> pagedRates = exchangeRateRepository.findAll(filter, pagination);
        return pagedRates.map(ExchangeRateResult::from);
    }
}
