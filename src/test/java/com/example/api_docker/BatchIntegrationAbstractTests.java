package com.example.api_docker;

import com.example.api_docker.infra.persistence.exchangerate.ExchangeRateJpaRepository;
import com.example.api_docker.infra.persistence.user.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Classe base abstrata para testes de integração do Spring Batch.
 * <p>
 * Carrega o contexto Spring configurado para banco de dados H2 em memória com as
 * migrations do Flyway já aplicadas (tabelas de negócio e tabelas de metadados do Batch).
 * <p>
 * <b>Atenção:</b> Esta classe deliberadamente NÃO inclui a anotação {@code @Transactional}
 * no nível do teste para não interferir no controle transacional nativo do Spring Batch,
 * permitindo que os chunks, commits e metadados de execução sejam persistidos e
 * consultados exatamente como ocorrem em ambiente de produção.
 * Por não haver rollback automático, o método {@link #cleanUpDatabase()} limpa as tabelas
 * de negócio após cada teste para garantir isolamento.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test-repository.properties")
public abstract class BatchIntegrationAbstractTests {

    @Autowired
    private ExchangeRateJpaRepository exchangeRateJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @AfterEach
    void cleanUpDatabase() {
        exchangeRateJpaRepository.deleteAllInBatch();
        userJpaRepository.deleteAllInBatch();
    }
}
