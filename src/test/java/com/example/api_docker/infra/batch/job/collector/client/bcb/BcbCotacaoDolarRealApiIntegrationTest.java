package com.example.api_docker.infra.batch.job.collector.client.bcb;

import com.example.api_docker.infra.batch.job.collector.client.bcb.dto.BcbCotacaoDolarPayload;
import com.example.api_docker.infra.http.HttpDownloadClient;
import com.example.api_docker.infra.http.config.HttpClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração real contra a API do Banco Central (Olinda PTAX).
 * <p>
 * Marcado com a tag {@code external} para que o Maven Surefire o ignore na execução
 * padrão do CI/CD.
 * <p>
 * Para executá-lo individualmente:
 * {@code ./mvnw test -Dtest=BcbCotacaoDolarRealApiIntegrationTest -Dtest.excluded.groups=none}
 */
@Tag("external")
class BcbCotacaoDolarRealApiIntegrationTest {

    private BcbCotacaoDolarClient bcbClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        HttpClientConfig config = new HttpClientConfig();
        ClientHttpRequestFactory requestFactory = config.clientHttpRequestFactory();
        RestClient.Builder restClientBuilder = config.restClientBuilder(requestFactory);
        HttpDownloadClient downloadClient = new HttpDownloadClient(restClientBuilder);
        objectMapper = new ObjectMapper();

        bcbClient = new BcbCotacaoDolarClient(
                downloadClient,
                objectMapper,
                "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata"
        );
    }

    @Test
    @DisplayName("Deve bater na API real do BCB e validar a estrutura bruta do JSON retornado")
    void deveValidarJsonRealBrutoDaApiDoBancoCentral() throws Exception {
        LocalDate dataInicial = LocalDate.of(2023, 10, 1);
        LocalDate dataFinal = LocalDate.of(2023, 10, 5);

        String jsonBruto = bcbClient.baixarCotacaoBruta(dataInicial, dataFinal, 10);

        assertThat(jsonBruto).isNotBlank();

        JsonNode root = objectMapper.readTree(jsonBruto);
        assertThat(root.has("@odata.context")).isTrue();
        assertThat(root.has("value")).isTrue();

        JsonNode valueNode = root.get("value");
        assertThat(valueNode.isArray()).isTrue();
        assertThat(valueNode.size()).isGreaterThan(0);

        JsonNode primeiroItem = valueNode.get(0);
        assertThat(primeiroItem.has("cotacaoCompra")).isTrue();
        assertThat(primeiroItem.has("cotacaoVenda")).isTrue();
        assertThat(primeiroItem.has("dataHoraCotacao")).isTrue();
        assertThat(primeiroItem.get("cotacaoCompra").isNumber()).isTrue();
        assertThat(primeiroItem.get("cotacaoVenda").isNumber()).isTrue();
    }

    @Test
    @DisplayName("Deve bater na API real do BCB e retornar diretamente a lista de BcbCotacaoDolarPayload")
    void deveConsultarApiRealERetornarListaDePayloadComSucesso() {
        LocalDate dataInicial = LocalDate.of(2023, 10, 1);
        LocalDate dataFinal = LocalDate.of(2023, 10, 5);

        List<BcbCotacaoDolarPayload> cotacoes = bcbClient.buscarCotacaoPeriodo(dataInicial, dataFinal, 10);

        assertThat(cotacoes).isNotEmpty();

        BcbCotacaoDolarPayload primeiraCotacao = cotacoes.get(0);
        assertThat(primeiraCotacao.cotacaoCompra()).isNotNull().isPositive();
        assertThat(primeiraCotacao.cotacaoVenda()).isNotNull().isPositive();
        assertThat(primeiraCotacao.dataHoraCotacao()).isNotBlank();
    }
}
