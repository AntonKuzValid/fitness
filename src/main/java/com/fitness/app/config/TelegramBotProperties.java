package com.fitness.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {

    private String token;
    private String username = "fitness-sheet-reader-bot";
    private String webhookUrl;
    private Map<String, String> clients;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Map<String, String> getClients() {
        return clients;
    }

    public void setClients(Map<String, String> clients) {
        this.clients = clients;
    }
}
