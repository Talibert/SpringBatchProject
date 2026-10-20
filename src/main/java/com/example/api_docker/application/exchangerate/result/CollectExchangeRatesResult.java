package com.example.api_docker.application.exchangerate.result;

public record CollectExchangeRatesResult(
        int fetchedCount,
        int savedCount
) {}
