package br.com.pulseapi.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.pulseapi.client.HttpApiClient;
import br.com.pulseapi.domain.ApiConfig;
import br.com.pulseapi.exceptions.DuplicateApiUrlException;
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
            validateUrlAccessibility(config.getApiUrl());
            validateScheduleInterval(config.getScheduleInterval());
            config.setAccessToken(generateAccessToken());
            ApiConfig savedConfig = repository.save(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } catch (DuplicateApiUrlException e) {
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
        ApiStatusResponse statusResponse = fetchApiStatus(config.getApiUrl());
        processScheduledStatus(config, statusResponse, config.getApiUrl());
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
        if (repository.existsByApiUrl(config.getApiUrl())) {
            throw new DuplicateApiUrlException("Já existe um registro com a URL '" + config.getApiUrl() + "'");
        }
    }

    private void validateUrlAccessibility(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("A URL deve começar com 'http://' ou 'https://'");
        }
        try {
            ApiStatusResponse status = httpClient.fetchApiStatus(url);
            log.info("URL {} é acessível, status retornado: {}", url, status.getHttpStatusCode());
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
        if (shouldNotify(config, statusResponse.getHttpStatusCode())) {
            notificationService.sendAlert(
                config,
                statusResponse.getHttpStatusCode(),
                endpoint,
                statusResponse.getRequestLatencyMs(),
                statusResponse.getResponseBody()
            );
        }
        updateApiStatus(config, statusResponse.getHttpStatusCode());
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
        config.setLastHttpStatus(status);
        repository.save(config);
    }

    private ResponseEntity<String> handleInternalError(String message) {
        log.error(message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno.");
    }

    /**
     * Atualiza uma configuração de API existente e verifica seu status.
     *
     * @param config Configuração atualizada da API
     * @return Configuração atualizada salva no banco
     * @throws IllegalArgumentException se a URL não existir
     * @throws SecurityException se o usuário não tiver permissão
     */
    public ApiConfig updateApiConfig(ApiConfig config) {
        log.debug("Atualizando configuração da API: {}", config.getApiUrl());
        ApiConfig existing = findExistingConfigByUrl(config.getApiUrl());
        validateUserPermission(existing, config.getOwnerUserId());
        copyUpdatedFields(existing, config);
        checkApiStatus(existing);
        ApiConfig updatedConfig = repository.save(existing);
        log.info("Configuração da API {} atualizada com sucesso", updatedConfig.getApiUrl());
        return updatedConfig;
    }

    private void copyUpdatedFields(ApiConfig target, ApiConfig source) {
        BeanUtils.copyProperties(source, target, "id", "accessToken", "scheduleInterval"); // Ignora campos que não devem ser atualizados
    }

    private void validateUserPermission(ApiConfig existing, String userId) {
        if (!existing.getOwnerUserId().equals(userId)) {
            throw new SecurityException("Você não tem permissão para atualizar este registro.");
        }
    }

    private ApiConfig findExistingConfigByUrl(String url) {
        return repository.findByApiUrl(url)
                .orElseThrow(() -> new IllegalArgumentException("URL não encontrada: " + url));
    }
}
