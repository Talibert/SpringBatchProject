package com.example.api_docker.application.exchangerate.usecase;

import com.example.api_docker.application.exchangerate.command.CollectExchangeRatesCommand;
import com.example.api_docker.application.exchangerate.result.CollectExchangeRatesResult;
import com.example.api_docker.domain.exchangerate.ExchangeRate;
import com.example.api_docker.domain.exchangerate.ExchangeRateProvider;
import com.example.api_docker.domain.exchangerate.ExchangeRateRepository;
import com.example.api_docker.domain.shared.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectExchangeRatesUseCase {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional
    public CollectExchangeRatesResult execute(CollectExchangeRatesCommand command) {
        if (command == null || command.currency() == null || command.startDate() == null || command.endDate() == null) {
            throw new DomainException("Parâmetros de coleta de câmbio não podem ser nulos");
        }
        if (command.startDate().isAfter(command.endDate())) {
            throw new DomainException("A data inicial não pode ser posterior à data final");
        }

        List<ExchangeRate> fetchedRates = exchangeRateProvider.fetchExchangeRates(
                command.currency(),
                command.startDate(),
                command.endDate()
        );

        List<ExchangeRate> newRates = fetchedRates.stream()
                .filter(rate -> !exchangeRateRepository.existsByCurrencyAndQuoteTimestamp(
                        rate.getCurrency(),
                        rate.getQuoteTimestamp()
                ))
                .toList();

        if (!newRates.isEmpty()) {
            exchangeRateRepository.saveAll(newRates);
        }

        return new CollectExchangeRatesResult(fetchedRates.size(), newRates.size());
    }
}
