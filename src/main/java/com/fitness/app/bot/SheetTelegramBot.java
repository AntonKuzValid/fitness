package com.fitness.app.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitness.app.config.TelegramBotProperties;
import com.fitness.app.service.GoogleSheetsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("${telegram.bot.webhook-path:/telegram/webhook}")
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${telegram.bot.token:}')")
public class SheetTelegramBot {

    private static final Logger log = LoggerFactory.getLogger(SheetTelegramBot.class);

    private final GoogleSheetsService googleSheetsService;
    private final RestClient telegramClient;

    public SheetTelegramBot(GoogleSheetsService googleSheetsService,
                            TelegramBotProperties properties,
                            RestClient.Builder restClientBuilder) {
        this.googleSheetsService = googleSheetsService;
        this.telegramClient = restClientBuilder
                .baseUrl("https://api.telegram.org/bot" + properties.getToken())
                .build();
    }

    @PostMapping
    public ResponseEntity<Void> onWebhookUpdate(@RequestBody(required = false) Update update) {
        if (!hasReadableMessage(update)) {
            return ResponseEntity.ok().build();
        }

        String chatId = update.getMessage().getChatId().toString();
        String message = update.getMessage().getText().trim();
        if ("read".equalsIgnoreCase(message)) {
            String value = googleSheetsService.readCell("A1");
            String response = value == null || value.isBlank()
                    ? "Cell A1 is empty."
                    : "Cell A1: " + value;
            sendMessage(chatId, response);
        } else {
            sendMessage(chatId, "Send 'read' to get the latest value stored in cell A1.");
        }

        return ResponseEntity.ok().build();
    }

    private boolean hasReadableMessage(Update update) {
        return update != null && update.hasMessage() && update.getMessage().hasText();
    }

    private void sendMessage(String chatId, String text) {
        try {
            telegramClient.post()
                    .uri("/sendMessage")
                    .body(new SendMessageRequest(chatId, text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Unable to send Telegram response", ex);
        }
    }

    private record SendMessageRequest(@JsonProperty("chat_id") String chatId, String text) {
    }
}
