package com.example.api_docker.infra.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpDownloadClientTest {

    private MockRestServiceServer mockServer;
    private HttpDownloadClient httpDownloadClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        httpDownloadClient = new HttpDownloadClient(builder);
    }

    @Test
    @DisplayName("Deve realizar download retornando String bruta com sucesso")
    void deveFazerDownloadParaStringComSucesso() {
        String url = "https://api.exemplo.com/dados-texto";
        String payload = "{\"status\":\"ok\",\"total\":10}";

        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        String response = httpDownloadClient.downloadAsString(url);

        assertThat(response).isEqualTo(payload);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve realizar download de bytes brutos com sucesso")
    void deveFazerDownloadDeBytesComSucesso() {
        String url = "https://api.exemplo.com/arquivo.bin";
        byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};

        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedBytes, MediaType.APPLICATION_OCTET_STREAM));

        byte[] response = httpDownloadClient.downloadAsBytes(url);

        assertThat(response).isEqualTo(expectedBytes);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve lançar HttpDownloadException quando a requisição retornar código de erro HTTP")
    void deveLancarExcecaoEmErroHttp() {
        String url = "https://api.exemplo.com/not-found";

        mockServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> httpDownloadClient.downloadAsString(url))
                .isInstanceOf(HttpDownloadException.class)
                .hasMessageContaining("404");

        mockServer.verify();
    }
}
