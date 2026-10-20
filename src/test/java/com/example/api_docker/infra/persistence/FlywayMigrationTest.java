package com.example.api_docker.infra.persistence;

import com.example.api_docker.RepositoryAbstractTests;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest extends RepositoryAbstractTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Deve validar que todas as migrations do Flyway foram aplicadas com sucesso")
    void shouldValidateThatAllMigrationsAppliedSuccessfully() {
        MigrationInfo[] migrations = flyway.info().all();

        assertTrue(migrations.length > 0, "Deve haver ao menos uma migration registrada");

        for (MigrationInfo migration : migrations) {
            assertEquals(
                    MigrationState.SUCCESS,
                    migration.getState(),
                    "A migration " + migration.getScript() + " deveria estar em estado SUCCESS"
            );
            assertNotNull(migration.getInstalledOn(), "Data de instalação não pode ser nula");
            assertNotNull(migration.getChecksum(), "Checksum da migration não pode ser nulo");
        }

        MigrationInfo current = flyway.info().current();
        assertNotNull(current);
        assertEquals("3", current.getVersion().getVersion());
        assertEquals("create exchange rates table", current.getDescription());
    }

    @Test
    @DisplayName("Deve validar que as tabelas e colunas foram criadas no banco pela migration")
    void shouldValidateSchemaCreatedByFlyway() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet tables = metaData.getTables(null, null, "USERS", null)) {
                assertTrue(tables.next(), "Tabela USERS deve existir no banco de dados");
            }

            try (ResultSet tables = metaData.getTables(null, null, "EXCHANGE_RATES", null)) {
                assertTrue(tables.next(), "Tabela EXCHANGE_RATES deve existir no banco de dados");
            }

            try (ResultSet tables = metaData.getTables(null, null, "BATCH_JOB_INSTANCE", null)) {
                assertTrue(tables.next(), "Tabela BATCH_JOB_INSTANCE deve existir no banco de dados");
            }

            try (ResultSet tables = metaData.getTables(null, null, "BATCH_JOB_EXECUTION", null)) {
                assertTrue(tables.next(), "Tabela BATCH_JOB_EXECUTION deve existir no banco de dados");
            }

            List<String> columns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "USERS", null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            assertTrue(columns.contains("user_id"), "Coluna user_id deve existir");
            assertTrue(columns.contains("first_name"), "Coluna first_name deve existir");
            assertTrue(columns.contains("last_name"), "Coluna last_name deve existir");
            assertTrue(columns.contains("email"), "Coluna email deve existir");
            assertTrue(columns.contains("password_hash"), "Coluna password_hash deve existir");
            assertTrue(columns.contains("created_at"), "Coluna created_at deve existir");
        }
    }
}
