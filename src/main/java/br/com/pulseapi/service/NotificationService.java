package br.com.pulseapi.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.pulseapi.client.TelegramClient;
import br.com.pulseapi.domain.ApiConfig;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final TelegramClient telegramClient;

    public void sendAlert(ApiConfig config, int status, String endpoint) {
        String message = buildAlertMessage(config, status, endpoint);
        sendMessage(config.getNotificationChannel(), message);
    }

    private String buildAlertMessage(ApiConfig config, int status, String endpoint) {
        String statusMessage = getStatusMessage(status);
        return String.format("API %s (%s) falhou com status %d (%s) às %s",
                config.getName(), endpoint, status, statusMessage, LocalDateTime.now());
    }

    private String getStatusMessage(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "Unknown Error";
        };
    }

    private void sendMessage(String chatId, String message) {
        try {
            telegramClient.sendMessage(chatId, message);
        } catch (Exception e) {
            logError(e);
        }
    }

    private void logError(Exception e) {
        System.err.println("Falha ao enviar alerta: " + e.getMessage());
    }
}
