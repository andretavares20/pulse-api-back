package br.com.pulseapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.pulseapi.domain.ApiConfig;
import br.com.pulseapi.repository.ApiConfigRepository;
import br.com.pulseapi.service.ApiMonitorService;
import br.com.pulseapilib.client.model.StatusReport;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiMonitorController {
    private final ApiMonitorService apiMonitorService;
    private final ApiConfigRepository repository;

    @PostMapping("/register")
    public ResponseEntity<?> registerApi(@RequestBody ApiConfig config) {
        return apiMonitorService.registerApi(config);
    }

    @PostMapping("/report")
    public ResponseEntity<?> reportStatus(@RequestBody StatusReport report) {
        return apiMonitorService.reportStatus(report);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateApi(@RequestBody ApiConfig config) {
        try {
            ApiConfig existing = repository.findByUrl(config.getUrl())
                    .orElseThrow(() -> new IllegalArgumentException("URL não encontrada: " + config.getUrl()));

            // Verifica se o usuário é o responsável (exemplo simplificado)
            if (!existing.getUserId().equals(config.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Você não tem permissão para atualizar este registro.");
            }

            // Atualiza os campos
            existing.setName(config.getName());
            existing.setNotificationChannel(config.getNotificationChannel());
            existing.setLastStatus(config.getLastStatus());
            apiMonitorService.checkApiStatus(existing);

            ApiConfig updatedConfig = repository.save(existing);
            return ResponseEntity.ok(updatedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao atualizar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao atualizar o registro.");
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        return apiMonitorService.validateToken(token);
    }
}
