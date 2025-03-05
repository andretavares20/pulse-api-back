package br.com.pulseapi.model;

import lombok.Data;

@Data
public class ApiStatusResponse {
    private final int statusCode;
    private final long latencyMs;
    private final String responseBody; // null se não houver corpo (ex.: sucesso ou falha de conexão)
}
