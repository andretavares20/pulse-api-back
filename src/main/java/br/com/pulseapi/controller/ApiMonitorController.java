package br.com.pulseapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.pulseapi.entities.ConfiguracaoApiEntity;
import br.com.pulseapi.service.ApiMonitorService;
import br.com.pulseapilib.client.model.StatusReport;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para gerenciamento de configurações e monitoramento de APIs.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiMonitorController {
    private static final Logger logger = LoggerFactory.getLogger(ApiMonitorController.class);

    private final ApiMonitorService apiMonitorService;

    /**
     * Registra uma nova configuração de API.
     *
     * @param config Configuração da API a ser registrada
     * @return ResponseEntity com o resultado do registro
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerApi(@RequestBody ConfiguracaoApiEntity config) {
        logger.debug("Recebida requisição para registrar API: {}", config.getApiUrl());
        return apiMonitorService.registerApi(config);
    }

    /**
     * Reporta o status de uma API monitorada.
     *
     * @param report Relatório de status da API
     * @return ResponseEntity com o resultado do relatório
     */
    @PostMapping("/report")
    public ResponseEntity<?> reportStatus(@RequestBody StatusReport report) {
        logger.debug("Recebida requisição para reportar status da API: {}", report.getApiUrl());
        return apiMonitorService.reportStatus(report);
    }

    /**
     * Atualiza uma configuração de API existente.
     *
     * @param config Configuração atualizada da API
     * @return ResponseEntity com a configuração atualizada ou erro
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateApiConfig(@RequestBody ConfiguracaoApiEntity config) {
        logger.debug("Recebida requisição para atualizar API: {}", config.getApiUrl());
        try {
            ConfiguracaoApiEntity updatedConfig = apiMonitorService.updateApiConfig(config);
            return ResponseEntity.ok(updatedConfig);
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação ao atualizar API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            logger.warn("Permissão negada ao atualizar API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erro interno ao atualizar API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao atualizar o registro.");
        }
    }

    /**
     * Valida um token de acesso.
     *
     * @param token Token a ser validado
     * @return ResponseEntity com o resultado da validação
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        logger.debug("Recebida requisição para validar token: {}", token);
        return apiMonitorService.validateToken(token);
    }
}
