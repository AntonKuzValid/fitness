package com.fitness.app.controller;

import com.fitness.app.dto.ResultUpdateRequest;
import com.fitness.app.model.Exercise;
import com.fitness.app.service.GoogleSheetsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@CrossOrigin(origins = "*")
public class ExerciseController {

    private final GoogleSheetsService googleSheetsService;

    public ExerciseController(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @GetMapping
    public List<Exercise> getExercises() {
        return googleSheetsService.getExercises();
    }

    @GetMapping("/{rowNumber}")
    public ResponseEntity<Exercise> getExercise(@PathVariable int rowNumber) {
        return googleSheetsService.findExercise(rowNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{rowNumber}/result")
    public ResponseEntity<Void> saveResult(@PathVariable int rowNumber,
                                           @RequestBody @Valid ResultUpdateRequest request) {
        googleSheetsService.updateResult(rowNumber, request.getResult());
        return ResponseEntity.noContent().build();
    }
}
