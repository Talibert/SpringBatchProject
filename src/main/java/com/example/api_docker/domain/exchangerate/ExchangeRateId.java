package com.example.api_docker.domain.exchangerate;

import java.util.Objects;
import java.util.UUID;

public record ExchangeRateId(UUID value) {
    public ExchangeRateId {
        Objects.requireNonNull(value, "ExchangeRateId não pode ser nulo");
    }

    public static ExchangeRateId generate() {
        return new ExchangeRateId(UUID.randomUUID());
    }

    public static ExchangeRateId of(String raw) {
        return new ExchangeRateId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
