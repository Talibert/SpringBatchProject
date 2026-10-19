package com.example.api_docker.infra.batch.job.collector.client.bcb;

import com.example.api_docker.infra.batch.job.collector.client.bcb.dto.BcbCotacaoDolarPayload;
import com.example.api_docker.infra.http.HttpDownloadClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BcbCotacaoDolarClientTest {

    private MockRestServiceServer mockServer;
    private BcbCotacaoDolarClient bcbClient;

    private static final String BASE_URL = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        HttpDownloadClient downloadClient = new HttpDownloadClient(builder);
        ObjectMapper objectMapper = new ObjectMapper();
        bcbClient = new BcbCotacaoDolarClient(downloadClient, objectMapper, BASE_URL);
    }

    @Test
    @DisplayName("Deve construir a URI OData do BCB corretamente com datas formatadas e parâmetros")
    void deveConstruirUriODataCorretamente() {
        LocalDate dataInicial = LocalDate.of(2026, 10, 1);
        LocalDate dataFinal = LocalDate.of(2026, 10, 18);

        URI uri = bcbClient.buildCotacaoUri(dataInicial, dataFinal, 100);

        String uriString = uri.toString();
        assertThat(uriString).contains("CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)");
        assertThat(uriString).contains("@dataInicial='10-01-2026'");
        assertThat(uriString).contains("@dataFinalCotacao='10-18-2026'");
        assertThat(uriString).contains("$top=100");
        assertThat(uriString).contains("$format=json");
    }

    @Test
    @DisplayName("Deve baixar JSON bruto do BCB com sucesso")
    void deveBaixarCotacaoBrutaComSucesso() {
        LocalDate dataInicial = LocalDate.of(2026, 10, 1);
        LocalDate dataFinal = LocalDate.of(2026, 10, 2);

        String mockResponseBody = "{\"value\":[]}";
        URI uriEsperada = bcbClient.buildCotacaoUri(dataInicial, dataFinal, 100);

        mockServer.expect(requestTo(uriEsperada))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        String jsonBruto = bcbClient.baixarCotacaoBruta(dataInicial, dataFinal, 100);

        assertThat(jsonBruto).isEqualTo(mockResponseBody);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve buscar e extrair diretamente a lista de BcbCotacaoDolarPayload com sucesso")
    void deveBuscarCotacaoRetornandoListaComSucesso() {
        LocalDate dataInicial = LocalDate.of(2026, 10, 1);
        LocalDate dataFinal = LocalDate.of(2026, 10, 2);

        String mockResponseBody = """
                {
                  "@odata.context": "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/$metadata#_CotacaoDolarPeriodo",
                  "value": [
                    {
                      "cotacaoCompra": 5.4510,
                      "cotacaoVenda": 5.4516,
                      "dataHoraCotacao": "2026-10-01 13:05:00.0"
                    },
                    {
                      "cotacaoCompra": 5.4620,
                      "cotacaoVenda": 5.4628,
                      "dataHoraCotacao": "2026-10-02 13:05:00.0"
                    }
                  ]
                }
                """;

        URI uriEsperada = bcbClient.buildCotacaoUri(dataInicial, dataFinal, 100);

        mockServer.expect(requestTo(uriEsperada))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        List<BcbCotacaoDolarPayload> cotacoes = bcbClient.buscarCotacaoPeriodo(dataInicial, dataFinal, 100);

        assertThat(cotacoes).isNotNull();
        assertThat(cotacoes).hasSize(2);
        assertThat(cotacoes.get(0).cotacaoCompra()).isEqualByComparingTo(new BigDecimal("5.4510"));
        assertThat(cotacoes.get(0).cotacaoVenda()).isEqualByComparingTo(new BigDecimal("5.4516"));
        assertThat(cotacoes.get(0).dataHoraCotacao()).isEqualTo("2026-10-01 13:05:00.0");

        mockServer.verify();
    }
}
