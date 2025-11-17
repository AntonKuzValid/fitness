package com.fitness.app.bot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitness.app.config.TelegramBotProperties;
import com.fitness.app.service.ExerciseResultStorage;
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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("${telegram.bot.webhook-path:/telegram/webhook}")
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${telegram.bot.token:}')")
public class SheetTelegramBot {

    private static final Logger log = LoggerFactory.getLogger(SheetTelegramBot.class);
    private static final String DONE_REACTION = "\uD83D\uDC4D";

    private final GoogleSheetsService googleSheetsService;
    private final RestClient telegramClient;
    private final ExerciseResultStorage exerciseResultStorage;
    private final Map<String, List<Exercise>> lastSentExercises = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentMap<Integer, Integer>> exerciseMessageLookup = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> completedExercises = new ConcurrentHashMap<>();

    public SheetTelegramBot(GoogleSheetsService googleSheetsService,
                            ExerciseResultStorage exerciseResultStorage,
                            TelegramBotProperties properties,
                            RestClient.Builder restClientBuilder) {
        this.googleSheetsService = googleSheetsService;
        this.exerciseResultStorage = exerciseResultStorage;
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
        Optional<ExerciseValue> exerciseValue = parseExerciseValue(update, chatId);
        if (isHelloCommand(message)) {
            handleGreeting(chatId);
        } else if (exerciseValue.isPresent()) {
            handleExerciseValue(chatId, exerciseValue.get());
        } else {
            sendMessage(chatId, "Напиши 'привет', чтобы получить тренировку, или ответь на сообщение с упражнением чтобы записать результат");
        }

        return ResponseEntity.ok().build();
    }

    private boolean hasReadableMessage(Update update) {
        return update != null && update.hasMessage() && update.getMessage().hasText();
    }

    private Integer sendMessage(String chatId, String text) {
        try {
            SendMessageResponse response = telegramClient.post()
                .uri("/sendMessage")
                .body(new SendMessageRequest(chatId, text))
                .retrieve()
                .body(SendMessageResponse.class);
            if (response != null && response.result() != null) {
                return response.result().messageId();
            }
        } catch (Exception ex) {
            log.warn("Unable to send Telegram response", ex);
        }
        return null;
    }

    private boolean isHelloCommand(String message) {
        return message != null && "привет".equalsIgnoreCase(message.trim());
    }

    private void handleGreeting(String chatId) {
        List<Exercise> exercises = googleSheetsService.readExercises();
        if (exercises.isEmpty()) {
            sendMessage(chatId, "Привет! Не удалось найти упражнения на сегодня.");
            return;
        }

        sendMessage(chatId, "Привет! Вот тренировка на сегодня:");
        int index = 1;
        ConcurrentMap<Integer, Integer> chatMessages = new ConcurrentHashMap<>();
        exerciseMessageLookup.put(chatId, chatMessages);
        completedExercises.put(chatId, ConcurrentHashMap.newKeySet());
        for (Exercise exercise : exercises) {
            Integer messageId = sendMessage(chatId, formatExerciseMessage(index, exercise));
            if (messageId != null) {
                chatMessages.put(messageId, index);
            }
            index++;
        }
        sendMessage(chatId, "Напиши 'привет', чтобы получить тренировку, или ответь на сообщение с упражнением чтобы записать результат");
        lastSentExercises.put(chatId, exercises);
    }

    private void handleExerciseValue(String chatId, ExerciseValue exerciseValue) {
        Exercise exercise = resolveExercise(chatId, exerciseValue.exerciseNumber());
        if (exercise == null) {
            sendMessage(chatId, "Не нашёл упражнение с номером " + exerciseValue.exerciseNumber() + ". Сначала запроси тренировку.");
            return;
        }

        exerciseResultStorage.storeResult(exercise.rowNumber(), exerciseValue.value());
        markExerciseCompletion(chatId, exerciseValue);

        String exerciseName = exercise.name();
        if (StringUtils.hasText(exerciseName)) {
            sendMessage(chatId, "Записал упражнение " + exerciseValue.exerciseNumber() + " (" + exerciseName + "): " + exerciseValue.value());
        } else {
            sendMessage(chatId, "Записал упражнение " + exerciseValue.exerciseNumber() + ": " + exerciseValue.value());
        }
    }

    private Exercise resolveExercise(String chatId, int exerciseNumber) {
        return Optional.ofNullable(lastSentExercises.get(chatId))
            .filter(list -> exerciseNumber >= 1 && exerciseNumber <= list.size())
            .map(list -> list.get(exerciseNumber - 1))
            .orElse(null);
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

    private Optional<ExerciseValue> parseExerciseValue(Update update, String chatId) {
        if (update == null || update.getMessage() == null) {
            return Optional.empty();
        }
        String message = update.getMessage().getText();
        if (!StringUtils.hasText(message)) {
            return Optional.empty();
        }

        Message reply = update.getMessage().getReplyToMessage();
        if (reply == null) {
            return Optional.empty();
        }
        Integer replyExerciseNumber = resolveExerciseNumberFromReply(reply, chatId);
        if (replyExerciseNumber != null) {
            return Optional.of(new ExerciseValue(replyExerciseNumber, message, reply.getMessageId()));
        }
        return Optional.empty();
    }

    private Integer resolveExerciseNumberFromReply(Message reply, String chatId) {
        ConcurrentMap<Integer, Integer> chatMapping = exerciseMessageLookup.get(chatId);
        if (chatMapping == null) {
            return null;
        }
        return chatMapping.get(reply.getMessageId());
    }

    private void markExerciseCompletion(String chatId, ExerciseValue exerciseValue) {
        if (exerciseValue.exerciseMessageId() != null) {
            addDoneReaction(chatId, exerciseValue.exerciseMessageId());
        }
        Set<Integer> completed = completedExercises.computeIfAbsent(chatId, key -> ConcurrentHashMap.newKeySet());
        boolean isNewlyCompleted = completed.add(exerciseValue.exerciseNumber());
        if (isNewlyCompleted && areAllExercisesCompleted(chatId, completed)) {
            sendMessage(chatId, "Тренировка закончена!");
        }
    }

    private boolean areAllExercisesCompleted(String chatId, Set<Integer> completed) {
        List<Exercise> exercises = lastSentExercises.get(chatId);
        return exercises != null && !exercises.isEmpty() && completed.size() >= exercises.size();
    }

    private void addDoneReaction(String chatId, Integer messageId) {
        if (messageId == null) {
            return;
        }
        try {
            telegramClient.post()
                .uri("/setMessageReaction")
                .body(new SetMessageReactionRequest(chatId, messageId, List.of(new ReactionTypeEmoji(DONE_REACTION))))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Unable to set done reaction for message {}", messageId, ex);
        }
    }

    private record SendMessageRequest(@JsonProperty("chat_id") String chatId, String text) {
    }

    private record SendMessageResponse(boolean ok, TelegramMessage result) {
    }

    private record TelegramMessage(@JsonProperty("message_id") Integer messageId) {
    }

    private record SetMessageReactionRequest(@JsonProperty("chat_id") String chatId,
                                             @JsonProperty("message_id") Integer messageId,
                                             List<ReactionTypeEmoji> reaction) {
    }

    private record ReactionTypeEmoji(@JsonProperty("type") String type, String emoji) {
        private ReactionTypeEmoji(String emoji) {
            this("emoji", emoji);
        }
    }

    private record ExerciseValue(int exerciseNumber, String value, Integer exerciseMessageId) {
    }
}
