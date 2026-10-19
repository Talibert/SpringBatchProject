package com.example.api_docker.infra.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Cliente HTTP utilitário responsável exclusivamente por realizar downloads e requisições HTTP,
 * retornando a resposta bruta (String, byte[] ou Stream).
 * <p>
 * Não realiza parse de dados nem conhece DTOs de negócio. A responsabilidade de interpretação
 * e transformação do payload é delegada a quem consome a resposta.
 */
@Component
public class HttpDownloadClient {

    private static final Logger log = LoggerFactory.getLogger(HttpDownloadClient.class);

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpDownloadClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }


    /**
     * Construtor para testes unitários com RestClient mockado.
     */
    public HttpDownloadClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient não pode ser nulo");
    }

    /**
     * Realiza download do conteúdo em formato de texto bruto (JSON, XML, CSV, texto puro).
     *
     * @param uri             URI de destino.
     * @param headersConsumer Customizador opcional de headers HTTP.
     * @return Conteúdo bruto em formato {@link String}.
     */
    public String downloadAsString(URI uri, Consumer<HttpHeaders> headersConsumer) {
        log.debug("Executando download texto HTTP GET: {}", uri);
        return restClient.get()
                .uri(uri)
                .headers(headersConsumer != null ? headersConsumer : headers -> {})
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    int statusCode = resp.getStatusCode().value();
                    String statusText = resp.getStatusText();
                    log.error("Erro HTTP [{} - {}] no download de: {}", statusCode, statusText, uri);
                    throw new HttpDownloadException("Falha no download HTTP: " + statusCode + " - " + statusText, statusCode);
                })
                .body(String.class);
    }

    /**
     * Sobrecarga conveniente para download de texto via URI.
     */
    public String downloadAsString(URI uri) {
        return downloadAsString(uri, null);
    }

    /**
     * Sobrecarga conveniente para download de texto via URL em String.
     */
    public String downloadAsString(String url) {
        return downloadAsString(URI.create(url), null);
    }

    /**
     * Realiza download do conteúdo em formato binário bruto (arquivos, zips, PDFs, planilhas).
     *
     * @param uri             URI de destino.
     * @param headersConsumer Customizador opcional de headers HTTP.
     * @return Conteúdo em array de bytes.
     */
    public byte[] downloadAsBytes(URI uri, Consumer<HttpHeaders> headersConsumer) {
        log.debug("Executando download binário HTTP GET: {}", uri);
        return restClient.get()
                .uri(uri)
                .headers(headersConsumer != null ? headersConsumer : headers -> {})
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    int statusCode = resp.getStatusCode().value();
                    String statusText = resp.getStatusText();
                    log.error("Erro HTTP [{} - {}] no download binário de: {}", statusCode, statusText, uri);
                    throw new HttpDownloadException("Falha no download HTTP: " + statusCode + " - " + statusText, statusCode);
                })
                .body(byte[].class);
    }

    /**
     * Sobrecarga conveniente para download de bytes via URI.
     */
    public byte[] downloadAsBytes(URI uri) {
        return downloadAsBytes(uri, null);
    }

    /**
     * Sobrecarga conveniente para download de bytes via URL em String.
     */
    public byte[] downloadAsBytes(String url) {
        return downloadAsBytes(URI.create(url), null);
    }
}
