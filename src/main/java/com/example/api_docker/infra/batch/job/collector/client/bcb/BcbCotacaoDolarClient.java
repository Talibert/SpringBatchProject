package com.example.api_docker.infra.batch.job.collector.client.bcb;

import com.example.api_docker.infra.batch.job.collector.client.bcb.dto.BcbCotacaoDolarPayload;
import com.example.api_docker.infra.http.HttpDownloadClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cliente especializado para consulta e download das cotações do dólar (PTAX)
 * junto à API OData do Banco Central do Brasil (Olinda).
 * <p>
 * Utiliza o {@link HttpDownloadClient} para obter o payload bruto e extrai diretamente
 * a lista de {@link BcbCotacaoDolarPayload} do nó {@code value} da resposta OData.
 */
@Component
public class BcbCotacaoDolarClient {

    private static final Logger log = LoggerFactory.getLogger(BcbCotacaoDolarClient.class);

    // OData Olinda BCB espera formato MM-dd-yyyy com aspas simples, ex: '10-01-2026'
    private static final DateTimeFormatter BCB_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final HttpDownloadClient httpDownloadClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public BcbCotacaoDolarClient(
            HttpDownloadClient httpDownloadClient,
            ObjectMapper objectMapper,
            @Value("${bcb.api.ptax.base-url:https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata}") String baseUrl
    ) {
        this.httpDownloadClient = httpDownloadClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    /**
     * Baixa o JSON bruto da cotação do dólar diretamente da API do BCB.
     *
     * @param dataInicial Data inicial do período.
     * @param dataFinal   Data final do período.
     * @param top         Limite máximo de registros a retornar (ex: 100).
     * @return String contendo o JSON bruto retornado pela API.
     */
    public String baixarCotacaoBruta(LocalDate dataInicial, LocalDate dataFinal, int top) {
        Objects.requireNonNull(dataInicial, "dataInicial não pode ser nula");
        Objects.requireNonNull(dataFinal, "dataFinal não pode ser nula");

        URI uri = buildCotacaoUri(dataInicial, dataFinal, top);
        log.info("Baixando JSON bruto do BCB para período de {} até {} [URI: {}]", dataInicial, dataFinal, uri);

        return httpDownloadClient.downloadAsString(uri);
    }

    /**
     * Baixa os dados e extrai diretamente a lista de cotações contidas no nó {@code value}.
     *
     * @param dataInicial Data inicial do período.
     * @param dataFinal   Data final do período.
     * @param top         Limite máximo de registros a retornar (ex: 100).
     * @return Lista de {@link BcbCotacaoDolarPayload}.
     */
    public List<BcbCotacaoDolarPayload> buscarCotacaoPeriodo(LocalDate dataInicial, LocalDate dataFinal, int top) {
        String jsonBruto = baixarCotacaoBruta(dataInicial, dataFinal, top);

        try {
            JsonNode rootNode = objectMapper.readTree(jsonBruto);
            JsonNode valueNode = rootNode.path("value");

            if (valueNode.isMissingNode() || !valueNode.isArray()) {
                return Collections.emptyList();
            }

            return objectMapper.readerForListOf(BcbCotacaoDolarPayload.class)
                    .readValue(valueNode);
        } catch (java.io.IOException e) {
            log.error("Erro ao converter JSON de cotação do BCB: {}", e.getMessage());
            throw new IllegalStateException("Falha no parse do retorno da API do Banco Central", e);
        }
    }

    /**
     * Constrói a URI OData mantendo a formatação esperada pelo serviço Olinda:
     * {@code /CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='MM-dd-yyyy'&@dataFinalCotacao='MM-dd-yyyy'&$top=100&$format=json}
     */
    public URI buildCotacaoUri(LocalDate dataInicial, LocalDate dataFinal, int top) {
        String dataInicialFormatada = "'" + dataInicial.format(BCB_DATE_FORMATTER) + "'";
        String dataFinalFormatada = "'" + dataFinal.format(BCB_DATE_FORMATTER) + "'";

        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)")
                .queryParam("@dataInicial", dataInicialFormatada)
                .queryParam("@dataFinalCotacao", dataFinalFormatada)
                .queryParam("$top", top > 0 ? top : 100)
                .queryParam("$format", "json")
                .build(true)
                .toUri();
    }
}
