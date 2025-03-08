package br.com.pulseapi.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import br.com.pulseapi.model.ApiStatusResponse;
import lombok.RequiredArgsConstructor;

/**
 * Cliente HTTP para verificar o status de APIs externas.
 */
@Component
@RequiredArgsConstructor
public class HttpApiClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiClient.class);

    private final RestTemplate restTemplate;

    /**
     * Obtém o status de uma API externa, incluindo latência e corpo da resposta.
     */
    public ApiStatusResponse fetchApiStatus(String apiUrl) {
        long startTime = System.currentTimeMillis();
        ApiStatusResponse initialResponse = performGetRequest(apiUrl);
        long latencyInMs = calculateLatency(startTime);
        logRequestResult(apiUrl, initialResponse.getHttpStatusCode(), latencyInMs);
        return createFinalResponse(initialResponse, latencyInMs);
    }

    private ApiStatusResponse performGetRequest(String apiUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            return new ApiStatusResponse(response.getStatusCode().value(), 0, null);
        } catch (HttpClientErrorException e) {
            return handleClientError(e);
        } catch (HttpServerErrorException e) {
            return handleServerError(e);
        } catch (ResourceAccessException e) {
            return handleConnectionFailure(apiUrl, e);
        } catch (Exception e) {
            return handleUnexpectedError(apiUrl, e);
        }
    }

    private long calculateLatency(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private void logRequestResult(String apiUrl, int statusCode, long latencyInMs) {
        logger.debug("Requisição para {} retornou status {} em {}ms", apiUrl, statusCode, latencyInMs);
    }

    private ApiStatusResponse createFinalResponse(ApiStatusResponse initialResponse, long latencyInMs) {
        return new ApiStatusResponse(
                initialResponse.getHttpStatusCode(),
                latencyInMs,
                initialResponse.getResponseBody()
        );
    }

    private ApiStatusResponse handleClientError(HttpClientErrorException exception) {
        return new ApiStatusResponse(
                exception.getStatusCode().value(),
                0,
                exception.getResponseBodyAsString()
        );
    }

    private ApiStatusResponse handleServerError(HttpServerErrorException exception) {
        return new ApiStatusResponse(
                exception.getStatusCode().value(),
                0,
                exception.getResponseBodyAsString()
        );
    }

    private ApiStatusResponse handleConnectionFailure(String apiUrl, ResourceAccessException exception) {
        logger.error("Falha de conexão ao acessar {}: {}", apiUrl, exception.getMessage());
        return new ApiStatusResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), 0, "Connection failed");
    }

    private ApiStatusResponse handleUnexpectedError(String apiUrl, Exception exception) {
        logger.error("Erro inesperado ao acessar {}: {}", apiUrl, exception.getMessage());
        return new ApiStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 0, "Unexpected error");
    }
}
