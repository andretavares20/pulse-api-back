package br.com.pulseapi.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.com.pulseapi.client.TelegramClient;
import br.com.pulseapi.domain.ApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável por enviar notificações de alertas sobre falhas em APIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TelegramClient telegramClient;

    /**
     * Envia um alerta sobre uma falha na API para o canal de notificação configurado.
     */
    public void sendAlert(ApiConfig apiConfig, int statusCode, String endpoint, long latencyInMs, String responseBody) {
        String alertMessage = buildAlertMessage(apiConfig, statusCode, endpoint, latencyInMs, responseBody);
        sendMessageToTelegram(apiConfig.getNotificationChannel(), alertMessage);
    }

    private String buildAlertMessage(ApiConfig apiConfig, int statusCode, String endpoint, long latencyInMs, String responseBody) {
        String statusDescription = getHttpStatusDescription(statusCode);
        return formatAlertMessage(
                apiConfig.getApiName(),
                endpoint,
                statusCode,
                statusDescription,
                latencyInMs,
                responseBody
        );
    }

    private String formatAlertMessage(String apiName, String endpoint, int statusCode, String statusDescription,
                                      long latencyInMs, String responseBody) {
        String details = getResponseDetails(responseBody);
        return String.format(
                "API %s (%s) falhou com status %d (%s) às %s\nLatência: %dms\nDetalhes: %s",
                apiName,
                endpoint,
                statusCode,
                statusDescription,
                LocalDateTime.now(),
                latencyInMs,
                details
        );
    }

    private void sendMessageToTelegram(String chatId, String message) {
        try {
            telegramClient.sendMessage(chatId, message);
            log.info("Notificação enviada ao Telegram: {}", message);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação ao Telegram: {}", e.getMessage());
        }
    }

    private String getHttpStatusDescription(int statusCode) {
        try {
            return HttpStatus.valueOf(statusCode).getReasonPhrase();
        } catch (IllegalArgumentException e) {
            return "Unknown Error"; // Retorna texto padrão para códigos de status inválidos
        }
    }

    private String getResponseDetails(String responseBody) {
        return (responseBody != null && !responseBody.isEmpty()) ? responseBody : "Nenhum detalhe disponível";
    }
}
