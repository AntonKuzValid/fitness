# Fitness Google Sheets bot

This repository now contains a single Spring Boot (Java 21) service built with Gradle. The
service reads the public Google Sheet that backs the workouts and exposes the data in two
ways:

1. REST endpoints that return the entire sheet or individual cells.
2. A Telegram bot that answers "read" with the value stored in cell **A1**.

## Requirements

- Java 21+
- Gradle (the wrapper `./gradlew` is included)
- Network access to Google Sheets and the Telegram Bot API

## Configuration

Environment variables control the connection to Google Sheets and Telegram. The defaults
match the spreadsheet shared in the task description, so you only have to override the
values when pointing to a different document.

```bash
export GOOGLE_SHEETS_ID="1-HN3fM6N9PswKHqMc6Xbb52XJ0pXR_tJOiJUcGRAKyE"
export GOOGLE_SHEETS_GID=0                         # worksheet gid from the sheet URL
export TELEGRAM_BOT_TOKEN="8553072138:AAGqsswP014ayqMcgUSa8VtkE1SwKTSJG_U"
export TELEGRAM_BOT_USERNAME="fitness-sheet-reader-bot"  # change to your bot username
```

The token and username are required for the Telegram bot to register. When the token is
missing, the web API still works but the bot component is not started.

## Running the application

```bash
./gradlew bootRun
```

The server listens on `http://localhost:8080`.

### REST API

- `GET /api/sheet/rows` — returns all rows from the configured worksheet as JSON.
- `GET /api/sheet/cell/{cellReference}` — returns the value stored in the requested cell
  (A1 style reference such as `A1`, `C5`, ...).

### Telegram bot

Send `read` to the configured bot. The bot fetches the worksheet, reads cell A1, and replies
with the stored value (or a hint when the cell is empty).

## Building & testing

```bash
./gradlew clean build
```

The build uses Java 21 toolchains and produces a runnable Spring Boot jar in
`build/libs/`.
