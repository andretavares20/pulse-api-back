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

@Component
@RequiredArgsConstructor
public class HttpApiClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpApiClient.class);
    private final RestTemplate restTemplate;

    public ApiStatusResponse fetchApiStatus(String url) {
        long startTime = System.currentTimeMillis();
        ApiStatusResponse response = executeGetRequest(url);
        long latency = System.currentTimeMillis() - startTime;
        logger.debug("Requisição para {} retornou status {} em {}ms", url, response.getHttpStatusCode(), latency);
        return new ApiStatusResponse(response.getHttpStatusCode(), latency, response.getResponseBody());
    }

    private ApiStatusResponse executeGetRequest(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return new ApiStatusResponse(response.getStatusCode().value(), 0, null);
        } catch (HttpClientErrorException e) {
            return new ApiStatusResponse(e.getStatusCode().value(), 0, e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            return new ApiStatusResponse(e.getStatusCode().value(), 0, e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            logger.error("Falha de conexão: {}", e.getMessage());
            return new ApiStatusResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), 0, "Connection failed");
        } catch (Exception e) {
            logger.error("Erro inesperado: {}", e.getMessage());
            return new ApiStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 0, "Unexpected error");
        }
    }
}
