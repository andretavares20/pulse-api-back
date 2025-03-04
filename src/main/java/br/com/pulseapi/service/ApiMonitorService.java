package br.com.pulseapi.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.pulseapi.client.HttpApiClient;
import br.com.pulseapi.domain.ApiConfig;
import br.com.pulseapi.exceptions.DuplicateUrlException;
import br.com.pulseapi.repository.ApiConfigRepository;
import br.com.pulseapilib.client.model.StatusReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiMonitorService {
    private final HttpApiClient httpClient;
    private final NotificationService notificationService;
    private final ApiConfigRepository repository;

    public ResponseEntity<?> registerApi(ApiConfig config) {
        try {
            validateNewApi(config);
            validateUrlAccessibility(config.getUrl());
            String accessToken = generateAccessToken();
            config.setAccessToken(accessToken);
            ApiConfig savedConfig = repository.save(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } catch (DuplicateUrlException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return handleInternalError("Erro ao registrar API: " + e.getMessage());
        }
    }

    private void validateUrlAccessibility(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("A URL deve começar com 'http://' ou 'https://'");
        }
        try {
            int status = httpClient.getStatus(url);
            log.info("URL {} é acessível, status retornado: {}", url, status);
        } catch (Exception e) {
            log.error("URL {} não é acessível: {}", url, e.getMessage());
            throw new IllegalArgumentException("A URL '" + url + "' não é acessível: " + e.getMessage());
        }
    }

    public ResponseEntity<?> reportStatus(StatusReport report) {
        try {
            ApiConfig config = findApiConfig(report.getAccessToken());
            processStatusReport(config, report);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return handleInternalError("Erro ao processar relatório: " + e.getMessage());
        }
    }

    private void validateNewApi(ApiConfig config) {
        if (repository.existsByUrl(config.getUrl())) {
            throw new DuplicateUrlException("Já existe um registro com a URL '" + config.getUrl() + "'");
        }
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString(); // Token único simples
    }

    private ApiConfig findApiConfig(String accessToken) {
        return repository.findByAccessToken(accessToken)
                .orElseThrow(() -> new IllegalArgumentException("Access token não registrado: " + accessToken));
    }

    private void processStatusReport(ApiConfig config, StatusReport report) {
        if (shouldNotify(report.getStatus())) {
            notificationService.sendAlert(config, report.getStatus(), report.getEndpoint());
        }
        updateApiStatus(config, report.getStatus());
    }

    private boolean shouldNotify(int status) {
        return status != 200 && status != 201;
    }

    private void updateApiStatus(ApiConfig config, int status) {
        config.setLastStatus(status);
        repository.save(config);
    }

    private ResponseEntity<String> handleInternalError(String message) {
        System.err.println(message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno.");
    }

    public void checkApiStatus(ApiConfig config) {
        int status = fetchApiStatus(config.getUrl());
        processApiStatus(config, status, config.getUrl()); // Usa URL como endpoint por padrão
    }

    private int fetchApiStatus(String url) {
        return httpClient.getStatus(url);
    }

    private void processApiStatus(ApiConfig config, int status, String endpoint) {
        if (shouldNotify(status)) {
            notificationService.sendAlert(config, status, endpoint);
        }
        updateApiStatus(config, status);
    }

    public ResponseEntity<?> validateToken(String token) {
        log.info("Validando token: {}", token);
        try {
            boolean isValid = repository.existsByAccessToken(token);
            if (isValid) {
                log.info("Token válido!");
                return ResponseEntity.ok().build();
            }
            log.warn("Token inválido: {}", token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            return handleInternalError("Erro ao validar token: " + e.getMessage());
        }
    }
}
