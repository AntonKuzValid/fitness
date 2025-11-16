# Fitness tracker

This project contains a Spring Boot backend and a Vue 3 front-end that expose a small
fitness exercise tracker. Exercises are stored inside a Google Sheet where columns 3-6
store the static exercise data (name, repetitions, weight, comment) and column 7 stores
the most recent result. The backend exposes REST APIs backed by the Google Sheets API, and
the front-end consumes the APIs to display exercises and submit updated results.

## Requirements

- Java 17+
- Maven 3.9+
- Node.js 20+ / npm 10+
- A Google Cloud project with the Sheets API enabled and service account credentials that
  can edit your spreadsheet

## Backend setup

1. Create or identify a Google Sheet. Make sure columns C-G (3-7) follow this layout:
   | Column | Purpose          |
   | ------ | ---------------- |
   | C      | Exercise name    |
   | D      | Number of reps   |
   | E      | Weight           |
   | F      | Comment          |
   | G      | Result that the web app updates |

2. Share the sheet with your service account email address and note the sheet ID from the
   URL.
3. Download the service account JSON file and point the `GOOGLE_APPLICATION_CREDENTIALS`
   environment variable to its location.
4. Export the additional settings (they all have sensible defaults):

```bash
export GOOGLE_SHEETS_ID="your-spreadsheet-id"
export GOOGLE_SHEETS_WORKSHEET="Exercises"     # tab name
export GOOGLE_SHEETS_RANGE="Exercises!A2:G"     # range that contains exercise rows
export GOOGLE_SHEETS_START_ROW=2                # first data row
export GOOGLE_SHEETS_RESULT_COLUMN=G            # column where results should be saved
```

5. Start the API:

```bash
cd backend
./mvnw spring-boot:run   # or mvn spring-boot:run if you have Maven installed
```

The server listens on `http://localhost:8080` and exposes the following endpoints:

- `GET /api/exercises` — return all exercises from the sheet
- `GET /api/exercises/{rowNumber}` — read one exercise by row number
- `POST /api/exercises/{rowNumber}/result` — update column 7 with the provided result

## Front-end setup

The Vue app consumes the backend and provides a master/detail UI with an input for
saving your latest result. The API base URL defaults to `http://localhost:8080/api` but
can be overridden by creating a `.env` file with `VITE_API_BASE_URL`.

```bash
cd frontend
npm install
npm run dev    # starts Vite on http://localhost:5173
```

When you click an exercise in the list you will see the details plus an input box to save
results which writes back to column 7 in Google Sheets.
