package br.com.pulseapi.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.com.pulseapi.client.TelegramClient;
import br.com.pulseapi.domain.ApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final TelegramClient telegramClient;

    public void sendAlert(ApiConfig config, int status, String endpoint, long latencyMs, String responseBody) {
        String message = buildAlertMessage(config, status, endpoint, latencyMs, responseBody);
        sendTelegramMessage(config.getNotificationChannel(), message);
    }

    private String buildAlertMessage(ApiConfig config, int status, String endpoint, long latencyMs, String responseBody) {
        String statusText = getStatusText(status);
        return String.format(
            "API %s (%s) falhou com status %d (%s) às %s\nLatência: %dms\nDetalhes: %s",
            config.getName(),
            endpoint,
            status,
            statusText,
            LocalDateTime.now(),
            latencyMs,
            responseBody != null && !responseBody.isEmpty() ? responseBody : "Nenhum detalhe disponível"
        );
    }

    private void sendTelegramMessage(String chatId, String message) {
        try {
            telegramClient.sendMessage(chatId, message);
            log.info("Notificação enviada ao Telegram: {}", message);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação ao Telegram: {}", e.getMessage());
        }
    }

    private String getStatusText(int status) {
        try {
            return HttpStatus.valueOf(status).getReasonPhrase();
        } catch (IllegalArgumentException e) {
            return "Unknown Error";
        }
    }
}
