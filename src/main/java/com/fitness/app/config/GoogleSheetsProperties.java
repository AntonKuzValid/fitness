package com.fitness.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.sheets")
public class GoogleSheetsProperties {

    /**
     * Numeric gid of the worksheet to read ("0" for the first tab).
     */
    private String worksheetGid = "0";

    /**
     * Raw service account JSON used for write access.
     */
    private String serviceAccountKeyJson;

    public String getWorksheetGid() {
        return worksheetGid;
    }

    public void setWorksheetGid(String worksheetGid) {
        this.worksheetGid = worksheetGid;
    }

    public String getServiceAccountKeyJson() {
        return serviceAccountKeyJson;
    }

    public void setServiceAccountKeyJson(String serviceAccountKeyJson) {
        this.serviceAccountKeyJson = serviceAccountKeyJson;
    }
}
