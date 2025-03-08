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

/**
 * Serviço responsável por gerenciar o monitoramento e configuração de APIs externas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiMonitorService {

    private static final List<Long> VALID_SCHEDULE_INTERVALS = Arrays.asList(60_000L, 300_000L, 600_000L); // 1min, 5min, 10min

    private final HttpApiClient httpApiClient;
    private final NotificationService notificationService;
    private final ApiConfigRepository apiConfigRepository;

    // Métodos públicos de negócio

    /**
     * Registra uma nova configuração de API, validando e gerando um token de acesso.
     */
    public ResponseEntity<?> registerApi(ApiConfig apiConfig) {
        try {
            validateNewApiRegistration(apiConfig);
            apiConfig.setAccessToken(generateAccessToken());
            ApiConfig savedConfig = apiConfigRepository.save(apiConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } catch (DuplicateApiUrlException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return handleInternalError("Erro ao registrar API: " + e.getMessage());
        }
    }

    /**
     * Processa um relatório de status enviado por uma API externa.
     */
    public ResponseEntity<?> reportStatus(StatusReport statusReport) {
        try {
            ApiConfig apiConfig = findApiConfigByToken(statusReport.getAccessToken());
            processStatusReport(apiConfig, statusReport);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return handleInternalError("Erro ao processar relatório: " + e.getMessage());
        }
    }

    /**
     * Verifica o status de uma API agendada e notifica se necessário.
     */
    public void checkApiStatus(ApiConfig apiConfig) {
        ApiStatusResponse statusResponse = fetchApiStatus(apiConfig.getApiUrl());
        processScheduledStatus(apiConfig, statusResponse);
    }

    /**
     * Valida um token de acesso recebido.
     */
    public ResponseEntity<?> validateToken(String accessToken) {
        log.info("Validando token: {}", accessToken);
        try {
            boolean isValid = apiConfigRepository.existsByAccessToken(accessToken);
            if (isValid) {
                log.info("Token válido!");
                return ResponseEntity.ok().build();
            }
            log.warn("Token inválido: {}", accessToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            return handleInternalError("Erro ao validar token: " + e.getMessage());
        }
    }

    /**
     * Atualiza uma configuração existente de API, verificando permissões e status.
     */
    public ApiConfig updateApiConfig(ApiConfig updatedConfig) {
        log.debug("Atualizando configuração da API: {}", updatedConfig.getApiUrl());
        ApiConfig existingConfig = findConfigByUrl(updatedConfig.getApiUrl());
        validateUserPermission(existingConfig, updatedConfig.getOwnerUserId());
        copyUpdatedFields(existingConfig, updatedConfig);
        checkApiStatus(existingConfig);
        ApiConfig savedConfig = apiConfigRepository.save(existingConfig);
        log.info("Configuração da API {} atualizada com sucesso", savedConfig.getApiUrl());
        return savedConfig;
    }

    // Métodos privados de validação

    private void validateNewApiRegistration(ApiConfig apiConfig) {
        checkForDuplicateUrl(apiConfig.getApiUrl());
        validateUrlAccessibility(apiConfig.getApiUrl());
        ensureValidScheduleInterval(apiConfig.getScheduleInterval());
    }

    private void checkForDuplicateUrl(String url) {
        if (apiConfigRepository.existsByApiUrl(url)) {
            throw new DuplicateApiUrlException("Já existe um registro com a URL '" + url + "'");
        }
    }

    private void validateUrlAccessibility(String url) {
        if (!isValidUrlPrefix(url)) {
            throw new IllegalArgumentException("A URL deve começar com 'http://' ou 'https://'");
        }
        try {
            ApiStatusResponse status = httpApiClient.fetchApiStatus(url);
            log.info("URL {} é acessível, status retornado: {}", url, status.getHttpStatusCode());
        } catch (Exception e) {
            log.error("URL {} não é acessível: {}", url, e.getMessage());
            throw new IllegalArgumentException("A URL '" + url + "' não é acessível: " + e.getMessage());
        }
    }

    private void ensureValidScheduleInterval(Long scheduleInterval) {
        if (scheduleInterval == null || !VALID_SCHEDULE_INTERVALS.contains(scheduleInterval)) {
            throw new IllegalArgumentException("O intervalo de agendamento deve ser 60000 (1min), 300000 (5min) ou 600000 (10min)");
        }
    }

    private void validateUserPermission(ApiConfig existingConfig, String userId) {
        if (!existingConfig.getOwnerUserId().equals(userId)) {
            throw new SecurityException("Você não tem permissão para atualizar este registro.");
        }
    }

    // Métodos privados de processamento

    private void processStatusReport(ApiConfig apiConfig, StatusReport statusReport) {
        if (shouldSendNotification(apiConfig, statusReport.getStatus())) {
            notificationService.sendAlert(apiConfig, statusReport.getStatus(), statusReport.getEndpoint(), 0, null);
        }
        updateApiStatus(apiConfig, statusReport.getStatus());
    }

    private void processScheduledStatus(ApiConfig apiConfig, ApiStatusResponse statusResponse) {
        if (shouldSendNotification(apiConfig, statusResponse.getHttpStatusCode())) {
            notificationService.sendAlert(
                    apiConfig,
                    statusResponse.getHttpStatusCode(),
                    apiConfig.getApiUrl(),
                    statusResponse.getRequestLatencyMs(),
                    statusResponse.getResponseBody()
            );
        }
        updateApiStatus(apiConfig, statusResponse.getHttpStatusCode());
    }

    // Métodos privados utilitários

    private ApiStatusResponse fetchApiStatus(String url) {
        try {
            return httpApiClient.fetchApiStatus(url);
        } catch (Exception e) {
            log.error("Erro ao verificar status da URL {}: {}", url, e.getMessage());
            return new ApiStatusResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 0, "Unexpected error");
        }
    }

    private ApiConfig findApiConfigByToken(String accessToken) {
        return apiConfigRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new IllegalArgumentException("Access token não registrado: " + accessToken));
    }

    private ApiConfig findConfigByUrl(String url) {
        return apiConfigRepository.findByApiUrl(url)
                .orElseThrow(() -> new IllegalArgumentException("URL não encontrada: " + url));
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString();
    }

    private boolean shouldSendNotification(ApiConfig apiConfig, int statusCode) {
        return statusCode != HttpStatus.OK.value() && statusCode != HttpStatus.CREATED.value();
    }

    private void updateApiStatus(ApiConfig apiConfig, int statusCode) {
        apiConfig.setLastHttpStatus(statusCode);
        apiConfigRepository.save(apiConfig);
    }

    private void copyUpdatedFields(ApiConfig targetConfig, ApiConfig sourceConfig) {
        BeanUtils.copyProperties(sourceConfig, targetConfig, "id", "accessToken", "scheduleInterval");
    }

    private ResponseEntity<String> handleInternalError(String errorMessage) {
        log.error(errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno.");
    }

    private boolean isValidUrlPrefix(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
