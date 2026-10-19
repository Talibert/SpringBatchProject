package com.example.api_docker.infra.http;

/**
 * Exceção lançada quando ocorre um erro na execução de um download ou requisição HTTP.
 */
public class HttpDownloadException extends RuntimeException {

    private final int statusCode;

    public HttpDownloadException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public HttpDownloadException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpDownloadException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
