package br.com.pulseapi.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Cliente para envio de mensagens ao Telegram via Bot API.
 */
@Component
public class TelegramClient extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramClient.class);

    @Value("${telegram.bot.token}")
    private final String botToken;

    @Value("${telegram.bot.username}")
    private final String botUsername;

    /**
     * Construtor que inicializa o bot com o token.
     */
    public TelegramClient(@Value("${telegram.bot.token}") String botToken, 
                         @Value("${telegram.bot.username}") String botUsername) {
        super(botToken); // Passa o token diretamente pro construtor da superclasse
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    /**
     * Retorna o nome de usuário do bot.
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Retorna o token do bot.
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Método não utilizado no MVP; deixado vazio para recebimento de atualizações.
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Não implementado no MVP
    }

    /**
     * Envia uma mensagem ao chat especificado no Telegram.
     *
     * @param chatId ID do chat destino
     * @param text   Texto da mensagem
     * @throws TelegramApiException se o envio falhar
     */
    public void sendMessage(String chatId, String text) throws TelegramApiException {
        SendMessage message = createTelegramMessage(chatId, text);
        executeMessageSend(message);
    }

    private SendMessage createTelegramMessage(String chatId, String text) {
        logger.debug("Criando mensagem para chatId {}: {}", chatId, text);
        return new SendMessage(chatId, text);
    }

    private void executeMessageSend(SendMessage message) throws TelegramApiException {
        try {
            execute(message);
            logger.info("Mensagem enviada ao Telegram para chatId {}: {}", message.getChatId(), message.getText());
        } catch (TelegramApiException e) {
            logger.error("Falha ao enviar mensagem ao Telegram para chatId {}: {}", message.getChatId(), e.getMessage());
            throw e;
        }
    }
}
