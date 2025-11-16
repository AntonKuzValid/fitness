package com.fitness.app.bot;

import com.fitness.app.config.TelegramBotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${telegram.bot.token:}') && " +
        "T(org.springframework.util.StringUtils).hasText('${telegram.bot.webhook-url:}')")
public class TelegramWebhookInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookInitializer.class);

    private final RestClient telegramClient;
    private final TelegramBotProperties properties;

    public TelegramWebhookInitializer(RestClient.Builder restClientBuilder, TelegramBotProperties properties) {
        this.properties = properties;
        this.telegramClient = restClientBuilder
                .baseUrl("https://api.telegram.org/bot" + properties.getToken())
                .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            return;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("url", properties.getWebhookUrl());

        try {
            telegramClient.post()
                    .uri("/setWebhook")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram webhook registered at {}", properties.getWebhookUrl());
        } catch (Exception ex) {
            log.error("Unable to register Telegram webhook", ex);
        }
    }
}
