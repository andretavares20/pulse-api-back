package br.com.pulseapi.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpApiClient {
    private final RestTemplate restTemplate;

    public HttpApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public int getStatus(String url) {
        try {
            return restTemplate.getForEntity(url, String.class).getStatusCodeValue();
        } catch (HttpClientErrorException e) {
            // Erros 4xx (ex.: 404 Not Found, 403 Forbidden)
            return e.getStatusCode().value();
        } catch (HttpServerErrorException e) {
            // Erros 5xx (ex.: 500 Internal Server Error, 503 Service Unavailable)
            return e.getStatusCode().value();
        } catch (ResourceAccessException e) {
            // Falhas de conexão (ex.: timeout, URL inválida)
            return HttpStatus.SERVICE_UNAVAILABLE.value(); // 503
        } catch (Exception e) {
            // Qualquer outro erro genérico
            return HttpStatus.INTERNAL_SERVER_ERROR.value(); // 500
        }
    }
}
