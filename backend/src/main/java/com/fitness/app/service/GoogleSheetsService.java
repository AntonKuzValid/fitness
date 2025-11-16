package com.fitness.app.service;

import com.fitness.app.config.GoogleSheetsProperties;
import com.fitness.app.model.Exercise;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleSheetsService {

    private static final String SHEETS_BASE_URL = "https://sheets.googleapis.com/v4/spreadsheets";

    private final HttpRequestFactory requestFactory;
    private final GoogleSheetsProperties properties;
    private final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

    public GoogleSheetsService(HttpRequestFactory requestFactory, GoogleSheetsProperties properties) {
        this.requestFactory = requestFactory;
        this.properties = properties;
    }

    public List<Exercise> getExercises() {
        try {
            GenericUrl url = new GenericUrl(String.format("%s/%s/values/%s",
                    SHEETS_BASE_URL,
                    properties.getSpreadsheetId(),
                    encode(properties.getDataRange())));
            HttpResponse response = requestFactory.buildGetRequest(url).execute();
            SheetsValueResponse parsed = response.parseAs(SheetsValueResponse.class);
            List<List<Object>> values = parsed.getValues();
            if (values == null) {
                return Collections.emptyList();
            }

            List<Exercise> exercises = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                int rowNumber = properties.getStartRow() + i;
                exercises.add(mapRow(rowNumber, row));
            }
            return exercises;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read exercises from Google Sheets", e);
        }
    }

    public Optional<Exercise> findExercise(int rowNumber) {
        return getExercises().stream()
                .filter(exercise -> exercise.getRowNumber() == rowNumber)
                .findFirst();
    }

    public void updateResult(int rowNumber, String result) {
        String range = String.format("%s!%s%d", properties.getWorksheetName(), properties.getResultColumn(), rowNumber);
        try {
            GenericUrl url = new GenericUrl(String.format("%s/%s/values/%s",
                    SHEETS_BASE_URL,
                    properties.getSpreadsheetId(),
                    encode(range)));
            url.set("valueInputOption", "RAW");
            String payload = gsonFactory.toString(Collections.singletonMap("values", List.of(List.of(result))));
            ByteArrayContent content = ByteArrayContent.fromString("application/json", payload);
            requestFactory.buildPutRequest(url, content).execute();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save result to Google Sheets", e);
        }
    }

    private Exercise mapRow(int rowNumber, List<Object> row) {
        String name = getCell(row, 2);
        String repetitions = getCell(row, 3);
        String weight = getCell(row, 4);
        String comment = getCell(row, 5);
        String result = getCell(row, 6);
        return new Exercise(rowNumber, name, repetitions, weight, comment, result);
    }

    private String getCell(List<Object> row, int index) {
        if (index < row.size()) {
            Object value = row.get(index);
            return value == null ? "" : value.toString();
        }
        return "";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class SheetsValueResponse {
        private List<List<Object>> values;

        public List<List<Object>> getValues() {
            return values;
        }

        public void setValues(List<List<Object>> values) {
            this.values = values;
        }
    }
}
