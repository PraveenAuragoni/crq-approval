# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CRQ Approval is a full-stack automation system:
- **Backend**: Spring Boot 3.2 / Java 17 (`backend/`)
- **Frontend**: React 18 + Vite 7 (`frontend/`)
- **Purpose**: Reads CRQ records from an OneDrive Excel file daily at 5:30 PM, checks their status via the Remedy REST API, and emails notifications for approved CRQs ("Request in Change" status). Also supports manual ad-hoc runs from the UI for CRQs updated after 5:30 PM.

## Commands

### Backend

```bash
cd backend

# Run in MOCK mode (no external credentials, demo data seeded on startup)
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run -Dspring-boot.run.profiles=mock

# Run normally (requires real credentials in application.properties)
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run

# Compile only
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw compile

# Build fat JAR (with React embedded from ../frontend/dist)
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean package -DskipTests
```

Backend runs on **port 8081** (default and SIT). H2 console at `http://localhost:8081/h2-console`.

### Frontend

```bash
cd frontend
npm install
npm run dev     # dev server on port 5173, proxies /api → http://localhost:8081
npm run build   # outputs to frontend/dist/
```

### Full SIT Package Build

```bash
# From repo root — builds React, embeds into Spring Boot JAR, zips deployment package
bash build-sit.sh
# Output: crq-approval-sit-1.0.0.zip
```

### Kill the backend process

```bash
lsof -ti:8081 | xargs kill -9
```

## Architecture

### Spring Profiles

| Profile | Database | OneDrive | Remedy | Email |
|---|---|---|---|---|
| *(default)* | H2 file (`./data/crqdb`) | Real MS Graph API | Real Remedy REST | Real SMTP |
| `mock` | H2 in-memory (reset on restart) | Generated in-memory Excel | Hardcoded statuses | Logged only |
| `sit` | PostgreSQL | Real MS Graph API | Real Remedy REST | Real SMTP |

### External Service Abstraction

Three interfaces decouple real vs mock implementations:
- `OneDrivePort` → `OneDriveService` (`@Profile("!mock")`) / `MockOneDriveService` (`@Profile("mock")`)
- `RemedyPort` → `RemedyService` (`@Profile("!mock")`) / `MockRemedyService` (`@Profile("mock")`)
- `EmailPort` → `EmailService` (`@Profile("!mock")`) / `MockEmailService` (`@Profile("mock")`)

`CrqService` injects these by interface, so no changes needed when switching profiles.

### Processing Pipeline (`CrqService.runJob`)

```
OneDrivePort.downloadExcelFile()
  → ExcelService.parseExcel()         (Apache POI, columns: CRQ#, Title, Assignee, Desc, LastUpdated)
  → [filter by lastUpdated > cutoff]  (ad-hoc only)
  → RemedyPort.getCrqStatus(crqNumber) per row
  → save Crq entity
  → EmailPort.sendApprovalEmail()     (one consolidated email for all approved)
  → update emailSent + emailSentAt
  → save ProcessingLog
```

### Scheduler

`CrqScheduler` fires `crqService.runScheduledJob()` via `@Scheduled(cron = "${crq.scheduler.cron}")` — default `0 30 17 * * ?` (5:30 PM daily).

### Ad-hoc Run Logic

Ad-hoc filters Excel rows where `lastUpdatedInExcel > today@17:30`. The threshold is configurable via `crq.adhoc.threshold-time` in properties.

### Mock Data

`MockDataInitializer` (`@Profile("mock")`) runs `ApplicationRunner` on startup, calling both `runScheduledJob()` and `runAdhocJob("demo-user")` to pre-populate the dashboard. Mock Excel has 10 CRQs; the last 3 have timestamps after 17:30 for ad-hoc demo. 6 of 10 are "approved" in `MockRemedyService`.

### REST API (`/api/crq`)

| Endpoint | Description |
|---|---|
| `GET /dashboard` | Stats + today's CRQs + last 10 logs |
| `GET /list` | All CRQ records |
| `GET /logs` | All processing logs |
| `POST /adhoc` | Trigger ad-hoc run `{"triggeredBy":"name"}` |
| `POST /run-now` | Force full scheduled run |

### Frontend Structure

Single-page app with sidebar navigation. No routing library used — tab state managed in `App.jsx`. API calls centralized in `src/services/api.js` (Axios, baseURL `/api/crq`).

### Deployment (SIT)

The Maven build copies `frontend/dist/` into `target/classes/static/`, so the Spring Boot fat JAR serves the React app at `/`. No separate web server needed — everything runs on port 8081.

## Key Configuration Files

- `backend/src/main/resources/application.properties` — default (dev/H2), port 8081
- `backend/src/main/resources/application-mock.properties` — mock profile, in-memory H2
- `backend/src/main/resources/application-sit.properties` — SIT profile, PostgreSQL, port 8081
- `frontend/vite.config.js` — dev proxy: `/api` → `http://localhost:8081`

## Known Gotchas

- **Java version**: System Maven uses Java 8. Always prefix with `JAVA_HOME=$(/usr/libexec/java_home -v 17)`.
- **H2 URL**: Do not combine `AUTO_SERVER=TRUE` with `DB_CLOSE_ON_EXIT=FALSE` — H2 2.x rejects it.
- **Graph SDK v6**: `users().byUserId().drive()` returns a limited `DriveRequestBuilder` with no `root()` or `items()`. Resolve drive ID first via `.drive().get()`, then use `drives().byDriveId(id).items().byDriveItemId("root:/path:")`.
- **Hibernate 6 JPQL**: `DATE()` function returns `Object`, breaking type-safe comparisons. Use `findByProcessedAtBetween(startOfDay, endOfDay)` with `LocalDateTime` parameters instead.
- **Excel columns**: 0=CRQ Number, 1=Title, 2=Assignee, 3=Description, 4=Last Updated (date cell).
