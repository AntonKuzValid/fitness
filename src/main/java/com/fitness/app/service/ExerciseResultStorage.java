package com.fitness.app.service;

import com.fitness.app.config.GoogleSheetsProperties;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class ExerciseResultStorage {

    private static final Logger log = LoggerFactory.getLogger(ExerciseResultStorage.class);
    private static final String RESULT_COLUMN = "K";
    private static final String SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets";

    private final GoogleSheetsProperties properties;
    private final RestClient sheetsClient;
    private final GoogleCredentials credentials;

    public ExerciseResultStorage(GoogleSheetsProperties properties, RestClient.Builder restClientBuilder) {
        Assert.hasText(properties.getServiceAccountKeyJson(), "Google service account key must be configured");
        this.properties = properties;
        this.sheetsClient = restClientBuilder.baseUrl("https://sheets.googleapis.com").build();
        this.credentials = loadCredentials(properties.getServiceAccountKeyJson());
    }

    public void storeResult(String spreadSheetId, int rowNumber, String value) {
        Assert.isTrue(rowNumber > 0, "Row numbers start at 1");
        String range = RESULT_COLUMN + rowNumber;
        ValueRangeBody requestBody = new ValueRangeBody(List.of(List.of(value == null ? "" : value)));
        try {
            sheetsClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v4/spreadsheets/{spreadsheetId}/values/{range}")
                            .queryParam("valueInputOption", "USER_ENTERED")
                            .build(spreadSheetId, range))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Unable to store exercise result for range {}", range, ex);
            throw new IllegalStateException("Unable to store exercise result in Google Sheets", ex);
        }
    }

    private GoogleCredentials loadCredentials(String keyJson) {
        try (InputStream inputStream = new ByteArrayInputStream(keyJson.getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singleton(SHEETS_SCOPE));
            googleCredentials.refreshIfExpired();
            return googleCredentials;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load Google service account credentials", ex);
        }
    }

    private String fetchAccessToken() {
        try {
            synchronized (credentials) {
                credentials.refreshIfExpired();
                AccessToken token = credentials.getAccessToken();
                if (token == null || !StringUtils.hasText(token.getTokenValue())) {
                    throw new IllegalStateException("Unable to obtain access token for Google Sheets");
                }
                return token.getTokenValue();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to refresh Google Sheets access token", ex);
        }
    }

    private record ValueRangeBody(List<List<String>> values) {
    }
}
