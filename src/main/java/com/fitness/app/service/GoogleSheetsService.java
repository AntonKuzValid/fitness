package com.fitness.app.service;

import com.fitness.app.config.GoogleSheetsProperties;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class GoogleSheetsService {

    private final GoogleSheetsProperties properties;
    private final RestClient restClient;

    public GoogleSheetsService(GoogleSheetsProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl("https://docs.google.com").build();
    }

    /**
     * Reads the entire worksheet and returns it as a list of rows where every row contains
     * the cell values in display order.
     */
    public List<List<String>> readWorksheet() {
        Assert.hasText(properties.getSpreadsheetId(), "A Google Sheets id must be configured");

        String csvPayload = fetchWorksheetCsv();
        try (CSVParser parser = CSVParser.parse(csvPayload, CSVFormat.DEFAULT)) {
            List<List<String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                record.forEach(value -> row.add(value == null ? "" : value));
                rows.add(Collections.unmodifiableList(row));
            }
            return Collections.unmodifiableList(rows);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse worksheet data", e);
        }
    }

    /**
     * Reads the value stored in the provided cell reference (A1 style).
     */
    public String readCell(String cellReference) {
        List<List<String>> rows = readWorksheet();
        CellCoordinate coordinate = parseCellReference(cellReference);
        if (coordinate.rowIndex() < rows.size()) {
            List<String> row = rows.get(coordinate.rowIndex());
            if (coordinate.columnIndex() < row.size()) {
                return row.get(coordinate.columnIndex());
            }
        }
        return "";
    }

    private String fetchWorksheetCsv() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/spreadsheets/d/{spreadsheetId}/gviz/tq")
                        .queryParam("tqx", "out:csv")
                        .queryParam("gid", properties.getWorksheetGid())
                        .build(properties.getSpreadsheetId()))
                .retrieve()
                .body(String.class);
    }

    private CellCoordinate parseCellReference(String cellReference) {
        Assert.hasText(cellReference, "Cell reference must not be empty");
        String normalized = cellReference.trim().toUpperCase(Locale.US);
        int letterCount = 0;
        while (letterCount < normalized.length() && Character.isLetter(normalized.charAt(letterCount))) {
            letterCount++;
        }
        if (letterCount == 0 || letterCount == normalized.length()) {
            throw new IllegalArgumentException("Invalid cell reference: " + cellReference);
        }
        String columnLetters = normalized.substring(0, letterCount);
        String rowPart = normalized.substring(letterCount);
        int columnIndex = columnLettersToIndex(columnLetters);
        int rowIndex = Integer.parseInt(rowPart) - 1;
        if (rowIndex < 0) {
            throw new IllegalArgumentException("Row numbers start at 1: " + cellReference);
        }
        return new CellCoordinate(rowIndex, columnIndex);
    }

    private int columnLettersToIndex(String letters) {
        int result = 0;
        for (char letter : letters.toCharArray()) {
            if (!Character.isLetter(letter)) {
                throw new IllegalArgumentException("Invalid column reference: " + letters);
            }
            result = result * 26 + (letter - 'A' + 1);
        }
        return result - 1;
    }

    private record CellCoordinate(int rowIndex, int columnIndex) {
    }
}
