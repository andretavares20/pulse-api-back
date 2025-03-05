package br.com.pulseapi.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.pulseapi.client.HttpApiClient;
import br.com.pulseapi.domain.ApiConfig;
import br.com.pulseapi.exceptions.DuplicateUrlException;
import br.com.pulseapi.model.ApiStatusResponse;
import br.com.pulseapi.repository.ApiConfigRepository;
import br.com.pulseapilib.client.model.StatusReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiMonitorService {

    private static final List<Long> VALID_SCHEDULE_INTERVALS = Arrays.asList(60_000L, 300_000L, 600_000L); // 1min, 5min, 10min

    private final HttpApiClient httpClient;
    private final NotificationService notificationService;
    private final ApiConfigRepository repository;

    public ResponseEntity<?> registerApi(ApiConfig config) {
        try {
            validateNewApi(config);
            validateUrlAccessibility(config.getUrl());
            validateScheduleInterval(config.getScheduleInterval());
            config.setAccessToken(generateAccessToken());
            ApiConfig savedConfig = repository.save(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } catch (DuplicateUrlException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return handleInternalError("Erro ao registrar API: " + e.getMessage());
        }
    }

    private void validateScheduleInterval(Long scheduleInterval) {
        if (scheduleInterval == null || !VALID_SCHEDULE_INTERVALS.contains(scheduleInterval)) {
            throw new IllegalArgumentException("O intervalo de agendamento deve ser 60000 (1min), 300000 (5min) ou 600000 (10min)");
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

    public void checkApiStatus(ApiConfig config) {
        ApiStatusResponse statusResponse = fetchApiStatus(config.getUrl());
        processScheduledStatus(config, statusResponse, config.getUrl());
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

    private void validateNewApi(ApiConfig config) {
        if (repository.existsByUrl(config.getUrl())) {
            throw new DuplicateUrlException("Já existe um registro com a URL '" + config.getUrl() + "'");
        }
    }

    private void validateUrlAccessibility(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("A URL deve começar com 'http://' ou 'https://'");
        }
        try {
            ApiStatusResponse status = httpClient.fetchApiStatus(url);
            log.info("URL {} é acessível, status retornado: {}", url, status.getStatusCode());
        } catch (Exception e) {
            log.error("URL {} não é acessível: {}", url, e.getMessage());
            throw new IllegalArgumentException("A URL '" + url + "' não é acessível: " + e.getMessage());
        }
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString();
    }

    private ApiConfig findApiConfig(String accessToken) {
        return repository.findByAccessToken(accessToken)
                .orElseThrow(() -> new IllegalArgumentException("Access token não registrado: " + accessToken));
    }

    private void processStatusReport(ApiConfig config, StatusReport report) {
        if (shouldNotify(config, report.getStatus())) {
            notificationService.sendAlert(config, report.getStatus(), report.getEndpoint(), 0, null);
        }
        updateApiStatus(config, report.getStatus());
    }

    private void processScheduledStatus(ApiConfig config, ApiStatusResponse statusResponse, String endpoint) {
        if (shouldNotify(config, statusResponse.getStatusCode())) {
            notificationService.sendAlert(
                config,
                statusResponse.getStatusCode(),
                endpoint,
                statusResponse.getLatencyMs(),
                statusResponse.getResponseBody()
            );
        }
        updateApiStatus(config, statusResponse.getStatusCode());
    }

    private ApiStatusResponse fetchApiStatus(String url) {
        try {
            return httpClient.fetchApiStatus(url);
        } catch (Exception e) {
            log.error("Erro ao verificar status da URL {}: {}", url, e.getMessage());
            return new ApiStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 0, "Unexpected error");
        }
    }

    private boolean shouldNotify(ApiConfig config, int status) {
        return status != 200 && status != 201;
    }

    private void updateApiStatus(ApiConfig config, int status) {
        config.setLastStatus(status);
        repository.save(config);
    }

    private ResponseEntity<String> handleInternalError(String message) {
        log.error(message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno.");
    }
}
