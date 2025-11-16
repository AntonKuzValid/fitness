package com.fitness.app.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleSheetsConfig {

    @Bean
    public GoogleCredentials googleCredentials(GoogleSheetsProperties properties) {
        try (FileInputStream credentialsStream = new FileInputStream(properties.getCredentialsFile())) {
            return GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load Google credentials", e);
        }
    }

    @Bean
    public HttpRequestFactory httpRequestFactory(GoogleCredentials credentials) {
        try {
            HttpRequestInitializer initializer = request -> {
                new HttpCredentialsAdapter(credentials).initialize(request);
                request.setParser(new JsonObjectParser(GsonFactory.getDefaultInstance()));
            };
            return GoogleNetHttpTransport.newTrustedTransport().createRequestFactory(initializer);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to create Google Sheets HTTP client", e);
        }
    }
}
