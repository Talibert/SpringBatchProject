package com.example.api_docker.infra.controller.query.provider;

import com.example.api_docker.application.exchangerate.query.ListExchangeRatesQuery;
import com.example.api_docker.application.exchangerate.result.ExchangeRateResult;
import com.example.api_docker.application.exchangerate.usecase.ListExchangeRatesUseCase;
import com.example.api_docker.domain.exchangerate.Currency;
import com.example.api_docker.domain.shared.exception.DomainException;
import com.example.api_docker.domain.shared.pagination.PageResult;
import com.example.api_docker.infra.controller.query.request.ResourceQueryFilterRequest;
import com.example.api_docker.infra.controller.query.response.ExchangeRateResponse;
import com.example.api_docker.infra.controller.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeRateQueryProvider implements ResourceQueryProvider<ExchangeRateResponse> {

    public static final String RESOURCE_NAME = "exchange-rates";

    private final ListExchangeRatesUseCase listExchangeRatesUseCase;

    @Override
    public String resourceName() {
        return RESOURCE_NAME;
    }

    @Override
    public PageResponse<ExchangeRateResponse> query(ResourceQueryFilterRequest filter) {
        Currency currency = resolveCurrency(filter);

        ListExchangeRatesQuery query = new ListExchangeRatesQuery(
                currency,
                filter.startDate(),
                filter.endDate(),
                filter.pagination()
        );

        PageResult<ExchangeRateResult> result = listExchangeRatesUseCase.execute(query);
        return PageResponse.from(result, ExchangeRateResponse::from);
    }

    private Currency resolveCurrency(ResourceQueryFilterRequest filter) {
        String rawCurrency = filter.getParam("currency");
        if (rawCurrency == null || rawCurrency.isBlank()) {
            rawCurrency = filter.search();
        }
        if (rawCurrency == null || rawCurrency.isBlank()) {
            return null;
        }
        try {
            return Currency.valueOf(rawCurrency.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Moeda inválida: " + rawCurrency);
        }
    }
}
