package com.fitness.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.sheets")
public class GoogleSheetsProperties {

    /** Spreadsheet identifier taken from the Google Sheets URL. */
    private String spreadsheetId;

    /** Numeric gid of the worksheet to read ("0" for the first tab). */
    private String worksheetGid = "0";

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getWorksheetGid() {
        return worksheetGid;
    }

    public void setWorksheetGid(String worksheetGid) {
        this.worksheetGid = worksheetGid;
    }
}
