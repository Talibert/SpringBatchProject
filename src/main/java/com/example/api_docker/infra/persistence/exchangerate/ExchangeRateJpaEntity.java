package com.example.api_docker.infra.persistence.exchangerate;

import com.example.api_docker.domain.exchangerate.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
public class ExchangeRateJpaEntity {

    @Id
    @Column(name = "exchange_rate_id", columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "quote_timestamp", nullable = false)
    private LocalDateTime quoteTimestamp;

    @Column(name = "buy_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal buyRate;

    @Column(name = "sell_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal sellRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ExchangeRateJpaEntity() {}
}
