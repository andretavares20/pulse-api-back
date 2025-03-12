package br.com.pulseapi.controller;

import java.security.Key;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.pulseapi.entities.ConfiguracaoApiEntity;
import br.com.pulseapi.entities.UserEntity;
import br.com.pulseapi.exceptions.DuplicateApiUrlException;
import br.com.pulseapi.model.dtos.EndpointDTO;
import br.com.pulseapi.service.ApiMonitorService;
import br.com.pulseapi.service.ConfiguracaoApiService;
import br.com.pulseapi.service.UserService;
import br.com.pulseapi.utils.ScheduleIntervalConverter;
import io.jsonwebtoken.security.Keys;

@RestController
@RequestMapping("/api")
public class ConfiguracaoApiController {

    private final ConfiguracaoApiService configuracaoApiService;
    private final UserService userService;
    private final Key secretKey;
    private final ApiMonitorService apiMonitorService;

    public ConfiguracaoApiController(
            ConfiguracaoApiService configuracaoApiService,
            UserService userService,
            @Value("${jwt.secret}") String secretKeyBase64,ApiMonitorService apiMonitorService) {
        this.configuracaoApiService = configuracaoApiService;
        this.userService = userService;
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyBase64));
        this.apiMonitorService=apiMonitorService;
    }

    @GetMapping("/endpoints")
    public ResponseEntity<List<EndpointDTO>> getEndpoints() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(403).build();
        }
        List<EndpointDTO> endpoints = configuracaoApiService.findByUser(user);
        return ResponseEntity.ok(endpoints);
    }

    @PostMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> createEndpoint(@RequestBody EndpointDTO endpointDTO) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity user = userService.findByEmail(email);
        if (user == null) {
            return createErrorResponse("Usuário não encontrado.");
        }

        // Validação dos campos obrigatórios
        if (endpointDTO.getName() == null || endpointDTO.getUrl() == null) {
            return createErrorResponse("Campos obrigatórios: name, url.");
        }

        if (!endpointDTO.getUrl().matches("^https?://.+")) {
            return createErrorResponse("URL inválida. Use um formato válido (ex.: http:// ou https://).");
        }

        // Definir valores padrão para scheduleInterval e notificationChannel, se não fornecidos
        String scheduleIntervalStr = endpointDTO.getScheduleInterval();
        if (scheduleIntervalStr == null) {
            scheduleIntervalStr = "5m";
            endpointDTO.setScheduleInterval(scheduleIntervalStr);
        }

        String notificationChannel = endpointDTO.getNotificationChannel();
        if (notificationChannel == null) {
            notificationChannel = "default";
            endpointDTO.setNotificationChannel(notificationChannel);
        }

        // Converter scheduleInterval de string para milissegundos
        Long scheduleIntervalMs;
        try {
            scheduleIntervalMs = ScheduleIntervalConverter.convertToMilliseconds(scheduleIntervalStr);
        } catch (IllegalArgumentException e) {
            return createErrorResponse("Intervalo de agendamento inválido: " + e.getMessage());
        }

        // Criar a entidade ConfiguracaoApiEntity
        ConfiguracaoApiEntity apiConfig = new ConfiguracaoApiEntity();
        apiConfig.setApiName(endpointDTO.getName());
        apiConfig.setApiUrl(endpointDTO.getUrl());
        apiConfig.setScheduleInterval(scheduleIntervalMs);
        apiConfig.setNotificationChannel(endpointDTO.getNotificationChannel());
        apiConfig.setLastHttpStatus(null);
        apiConfig.setUser(user);

        // Chamar o serviço para registrar a API
        ConfiguracaoApiEntity savedConfig;
        try {
            savedConfig = apiMonitorService.registerApi(apiConfig);
            EndpointDTO savedEndpoint = configuracaoApiService.mapToEndpointDTO(savedConfig);
            return createSuccessResponse("Endpoint registrado com sucesso.", savedEndpoint);
        } catch (DuplicateApiUrlException e) {
            return createErrorResponse(e.getMessage());
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Erro interno ao registrar endpoint: " + e.getMessage());
        }
    }

    @DeleteMapping("/endpoints/{id}")
    public ResponseEntity<Map<String, Object>> deleteEndpoint(@PathVariable Long id) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity user = userService.findByEmail(email);
        if (user == null) {
            return createErrorResponse("Usuário não encontrado.");
        }

        // Verificar se o endpoint pertence ao usuário (opcional, para segurança)
        List<EndpointDTO> userEndpoints = configuracaoApiService.findByUser(user);
        boolean exists = userEndpoints.stream().anyMatch(endpoint -> endpoint.getId().equals(id));
        if (!exists) {
            return createErrorResponse("Endpoint não encontrado ou não pertence ao usuário.");
        }

        configuracaoApiService.deleteById(id);
        return createSuccessResponse("Endpoint removido com sucesso.", null);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<Map<String, Object>> createSuccessResponse(String message, EndpointDTO endpoint) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        if (endpoint != null) {
            response.put("endpoint", endpoint);
        }
        return ResponseEntity.ok(response);
    }
}
