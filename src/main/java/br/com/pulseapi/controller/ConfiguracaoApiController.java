package br.com.pulseapi.controller;

import java.security.Key;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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
import br.com.pulseapi.model.dtos.EndpointDTO;
import br.com.pulseapi.service.ConfiguracaoApiService;
import br.com.pulseapi.service.UserService;
import io.jsonwebtoken.security.Keys;

@RestController
@RequestMapping("/api")
public class ConfiguracaoApiController {

    private final ConfiguracaoApiService configuracaoApiService;
    private final UserService userService;
    private final Key secretKey;

    public ConfiguracaoApiController(
            ConfiguracaoApiService configuracaoApiService,
            UserService userService,
            @Value("${jwt.secret}") String secretKeyBase64) {
        this.configuracaoApiService = configuracaoApiService;
        this.userService = userService;
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyBase64));
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
    public ResponseEntity<Map<String, Object>> createEndpoint(@RequestBody Map<String, String> endpointRequest) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity user = userService.findByEmail(email);
        if (user == null) {
            return createErrorResponse("Usuário não encontrado.");
        }

        String name = endpointRequest.get("name");
        String url = endpointRequest.get("url");

        if (name == null || url == null) {
            return createErrorResponse("Campos obrigatórios: name, url.");
        }

        if (!url.matches("^https?://.+")) {
            return createErrorResponse("URL inválida. Use um formato válido (ex.: http:// ou https://).");
        }

        ConfiguracaoApiEntity config = new ConfiguracaoApiEntity();
        config.setApiName(name);
        config.setApiUrl(url);
        config.setNotificationChannel("default"); // Valor padrão, pode ser ajustado
        config.setUser(user);
        EndpointDTO savedEndpoint = configuracaoApiService.save(config);

        return createSuccessResponse("Endpoint registrado com sucesso.", savedEndpoint);
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
