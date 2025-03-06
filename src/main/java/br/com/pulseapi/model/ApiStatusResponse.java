package br.com.pulseapi.model;

import lombok.Getter;
import lombok.ToString;

/**
 * Representa a resposta de uma requisição HTTP feita a uma API monitorada.
 * Contém o código de status, a latência da requisição e o corpo da resposta (se disponível).
 */
@Getter
@ToString
public class ApiStatusResponse {

    /**
     * Código de status HTTP retornado pela API (ex.: 200, 404).
     */
    private final int httpStatusCode;

    /**
     * Latência da requisição em milissegundos.
     */
    private final long requestLatencyMs;

    /**
     * Corpo da resposta retornado pela API, ou null se não houver (ex.: em caso de sucesso ou falha de conexão).
     */
    private final String responseBody;

    /**
     * Constrói uma nova resposta de status da API.
     *
     * @param httpStatusCode Código de status HTTP da requisição
     * @param requestLatencyMs Latência da requisição em milissegundos
     * @param responseBody Corpo da resposta, pode ser null
     */
    public ApiStatusResponse(int httpStatusCode, long requestLatencyMs, String responseBody) {
        this.httpStatusCode = httpStatusCode;
        this.requestLatencyMs = requestLatencyMs;
        this.responseBody = responseBody;
    }
}
