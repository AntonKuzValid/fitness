package com.fitness.app.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitness.app.config.TelegramBotProperties;
import com.fitness.app.service.GoogleSheetsService;
import com.fitness.app.service.GoogleSheetsService.Exercise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("${telegram.bot.webhook-path:/telegram/webhook}")
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${telegram.bot.token:}')")
public class SheetTelegramBot {

    private static final Logger log = LoggerFactory.getLogger(SheetTelegramBot.class);

    private static final Pattern EXERCISE_VALUE_PATTERN = Pattern.compile("\\s*(\\d+)\\s*-\\s*([\\d.,]+)\\s*");

    private final GoogleSheetsService googleSheetsService;
    private final RestClient telegramClient;
    private final Map<String, List<Exercise>> lastSentExercises = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, String>> recordedExerciseValues = new ConcurrentHashMap<>();

    public SheetTelegramBot(GoogleSheetsService googleSheetsService,
                            TelegramBotProperties properties,
                            RestClient.Builder restClientBuilder) {
        this.googleSheetsService = googleSheetsService;
        this.telegramClient = restClientBuilder
                .baseUrl("https://api.telegram.org/bot" + properties.getToken())
                .build();
    }

    @PostMapping
    public ResponseEntity<Void> onWebhookUpdate(@RequestBody(required = false) Update update) {
        if (!hasReadableMessage(update)) {
            return ResponseEntity.ok().build();
        }

        String chatId = update.getMessage().getChatId().toString();
        String message = update.getMessage().getText().trim();
        if (isHelloCommand(message)) {
            handleGreeting(chatId);
        } else if (matchesExerciseValue(message)) {
            handleExerciseValue(chatId, message);
        } else {
            sendMessage(chatId, "Напиши 'привет', чтобы получить тренировку, или пришли номер упражнения и значение в формате 1-2.");
        }

        return ResponseEntity.ok().build();
    }

    private boolean hasReadableMessage(Update update) {
        return update != null && update.hasMessage() && update.getMessage().hasText();
    }

    private void sendMessage(String chatId, String text) {
        try {
            telegramClient.post()
                    .uri("/sendMessage")
                    .body(new SendMessageRequest(chatId, text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Unable to send Telegram response", ex);
        }
    }

    private boolean isHelloCommand(String message) {
        return message != null && "привет".equalsIgnoreCase(message.trim());
    }

    private boolean matchesExerciseValue(String message) {
        return message != null && EXERCISE_VALUE_PATTERN.matcher(message).matches();
    }

    private void handleGreeting(String chatId) {
        List<Exercise> exercises = googleSheetsService.readExercises();
        if (exercises.isEmpty()) {
            sendMessage(chatId, "Привет! Не удалось найти упражнения на сегодня.");
            return;
        }

        sendMessage(chatId, "Привет! Вот тренировка на сегодня:");
        int index = 1;
        for (Exercise exercise : exercises) {
            sendMessage(chatId, formatExerciseMessage(index, exercise));
            index++;
        }
        sendMessage(chatId, "Когда закончишь, пришли номер упражнения и его текущее значение в формате 1-2.");
        lastSentExercises.put(chatId, exercises);
    }

    private void handleExerciseValue(String chatId, String message) {
        Matcher matcher = EXERCISE_VALUE_PATTERN.matcher(message);
        if (!matcher.matches()) {
            sendMessage(chatId, "Не понял сообщение. Используй формат номер-значение, например 1-2.");
            return;
        }
        int exerciseNumber = Integer.parseInt(matcher.group(1));
        String value = matcher.group(2);
        recordedExerciseValues
                .computeIfAbsent(chatId, id -> new ConcurrentHashMap<>())
                .put(exerciseNumber, value);

        String exerciseName = resolveExerciseName(chatId, exerciseNumber);
        if (StringUtils.hasText(exerciseName)) {
            sendMessage(chatId, "Записал упражнение " + exerciseNumber + " (" + exerciseName + "): " + value);
        } else {
            sendMessage(chatId, "Записал упражнение " + exerciseNumber + ": " + value);
        }
    }

    private String resolveExerciseName(String chatId, int exerciseNumber) {
        return Optional.ofNullable(lastSentExercises.get(chatId))
                .filter(list -> exerciseNumber >= 1 && exerciseNumber <= list.size())
                .map(list -> list.get(exerciseNumber - 1).name())
                .orElse("");
    }

    private String formatExerciseMessage(int index, Exercise exercise) {
        StringBuilder message = new StringBuilder();
        message.append("Упражнение ").append(index).append(": ").append(exercise.name());
        appendDetail(message, "Вес", exercise.weight());
        appendDetail(message, "Подходы", exercise.sets());
        appendDetail(message, "Повторения", exercise.repetitions());
        appendDetail(message, "Запас", exercise.reserve());
        appendDetail(message, "Отдых", exercise.rest());
        appendDetail(message, "Видео", exercise.videoLink());
        appendDetail(message, "Комментарий", exercise.comment());
        return message.toString().trim();
    }

    private void appendDetail(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append("\n• ").append(label).append(": ").append(value.trim());
        }
    }

    private record SendMessageRequest(@JsonProperty("chat_id") String chatId, String text) {
    }
}
