package com.fitness.app.dto;

import jakarta.validation.constraints.NotNull;

public class ResultUpdateRequest {

    @NotNull
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
