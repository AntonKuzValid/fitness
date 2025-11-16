package com.fitness.app.model;

public class Exercise {

    private final int rowNumber;
    private final String name;
    private final String repetitions;
    private final String weight;
    private final String comment;
    private final String result;

    public Exercise(int rowNumber, String name, String repetitions, String weight, String comment, String result) {
        this.rowNumber = rowNumber;
        this.name = name;
        this.repetitions = repetitions;
        this.weight = weight;
        this.comment = comment;
        this.result = result;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getName() {
        return name;
    }

    public String getRepetitions() {
        return repetitions;
    }

    public String getWeight() {
        return weight;
    }

    public String getComment() {
        return comment;
    }

    public String getResult() {
        return result;
    }
}
