# Technical Specification Document (TSD)
## CRQ Approval Automation System

**Version:** 1.0  
**Date:** 2026-05-11  
**Status:** Draft

---

## 1. System Architecture Overview

CRQ Approval is a monolithic full-stack application with a Spring Boot backend and a React single-page frontend. In production (SIT), the React build is embedded inside the Spring Boot JAR and served as static resources — no separate web server is required.

```
┌──────────────────────────────────────────────────────────────┐
│                   Browser (React SPA)                        │
│   Dashboard | CRQ List | Ad-Hoc Approval | Processing Logs   │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTP /api/crq/*
┌──────────────────────▼───────────────────────────────────────┐
│              Spring Boot Application (:8081)                 │
│                                                              │
│  CrqController  →  CrqService  →  OneDrivePort              │
│                              →  ExcelService                 │
│                              →  RemedyPort                   │
│                              →  EmailPort                    │
│                              →  CrqRepository               │
│                              →  ProcessingLogRepository      │
│                                                              │
│  CrqScheduler  →  CrqService                                 │
└──────┬───────────────┬─────────────────┬────────────────────┘
       │               │                 │
  ┌────▼────┐   ┌──────▼──────┐   ┌──────▼──────┐
  │  H2 /   │   │  Microsoft  │   │  Remedy     │
  │PostgreSQL│   │  Graph API  │   │  REST API   │
  └─────────┘   │ (OneDrive)  │   └──────┬──────┘
                └─────────────┘          │
                                   ┌─────▼──────┐
                                   │  SMTP Mail │
                                   │  Server    │
                                   └────────────┘
```

---

## 2. Technology Stack

### Backend

| Component | Technology | Version |
|---|---|---|
| Runtime | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Build Tool | Apache Maven | 3.x (wrapper) |
| ORM | Spring Data JPA / Hibernate | 6.x |
| Database (dev) | H2 (file-based) | 2.x |
| Database (SIT) | PostgreSQL | — |
| Excel Parsing | Apache POI | 5.2.5 |
| OneDrive Integration | Microsoft Graph SDK | 6.4.0 |
| Azure Auth | Azure Identity | 1.12.2 |
| Code Generation | Lombok | — |
| JSON | Jackson | — |
| Email | Spring Mail (Jakarta Mail) | — |
| Scheduling | Spring `@Scheduled` | — |

### Frontend

| Component | Technology | Version |
|---|---|---|
| Framework | React | 18.3.1 |
| Build Tool | Vite | 7.0.0 |
| HTTP Client | Axios | 1.7.2 |
| Styling | Vanilla CSS (custom) | — |
| State Management | React Hooks (`useState`) | — |
| Routing | None (tab-based `useState`) | — |

---

## 3. Spring Profile Configuration

Three Spring profiles control which implementations are active:

| Profile | DB | OneDrive | Remedy | Email | Port |
|---|---|---|---|---|---|
| *(default)* | H2 file `./data/crqdb` | Real MS Graph API | Real Remedy REST | Real SMTP | 8081 |
| `mock` | H2 in-memory `crqmockdb` | Generated in-memory Excel | Hardcoded statuses | Console log only | 8081 |
| `sit` | PostgreSQL | Real MS Graph API | Real Remedy REST | Real SMTP | 8081 |

Activate mock profile: `-Dspring-boot.run.profiles=mock`  
Activate SIT profile: add `spring.profiles.active=sit` to the environment or JVM args.

---

## 4. Package Structure

```
com.crq.approval
├── CrqApprovalApplication.java      @SpringBootApplication, @EnableScheduling
├── controller/
│   └── CrqController.java           REST endpoints at /api/crq
├── service/
│   ├── CrqService.java              Core orchestration logic
│   ├── OneDrivePort.java            Interface: download Excel from OneDrive
│   ├── OneDriveService.java         Real implementation (@Profile("!mock"))
│   ├── RemedyPort.java              Interface: fetch CRQ status from Remedy
│   ├── RemedyService.java           Real implementation (@Profile("!mock"))
│   ├── EmailPort.java               Interface: send approval email
│   ├── EmailService.java            Real implementation (@Profile("!mock"))
│   └── ExcelService.java            Apache POI Excel parser
├── repository/
│   ├── CrqRepository.java           Spring Data JPA for Crq entity
│   └── ProcessingLogRepository.java Spring Data JPA for ProcessingLog entity
├── model/
│   ├── Crq.java                     JPA entity
│   └── ProcessingLog.java           JPA entity
├── dto/
│   ├── CrqDto.java                  API response DTO (has static from())
│   ├── DashboardDto.java            Dashboard response DTO (has static from())
│   └── ProcessingLogDto.java        Log response DTO (has static from())
├── mock/
│   ├── MockDataInitializer.java     Seeds demo data on startup (@Profile("mock"))
│   ├── MockOneDriveService.java     Returns generated in-memory Excel
│   ├── MockRemedyService.java       Returns hardcoded statuses (6/10 approved)
│   └── MockEmailService.java        Logs email content to console
├── config/
│   ├── AppConfig.java               Spring beans (RestTemplate, etc.)
│   └── WebConfig.java               CORS configuration
└── scheduler/
    └── CrqScheduler.java            @Scheduled daily 5:30 PM trigger
```

---

## 5. External Service Abstraction (Port Pattern)

All three external integrations are hidden behind interfaces. `CrqService` depends only on the interfaces, making the mock/real switch transparent.

### OneDrivePort
```java
InputStream downloadExcelFile();
OffsetDateTime getFileLastModifiedTime();
```
Real: authenticates with Azure `ClientSecretCredential`, uses Graph SDK to resolve drive ID then download file at the configured path.

### RemedyPort
```java
String getCrqStatus(String crqNumber);
boolean isApproved(String status);
```
Real: sends `GET` request to Remedy REST API with bearer token auth. `isApproved` compares status to `crq.remedy.approved-status`.

### EmailPort
```java
void sendApprovalEmail(List<Crq> approvedCrqs);
void sendApprovalEmail(Crq crq);
```
Real: uses `JavaMailSender` to send HTML/text email via configured SMTP server.

---

## 6. Database Schema

### Table: `crq`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Surrogate key |
| `crq_number` | VARCHAR | NOT NULL | CRQ identifier from Excel |
| `title` | VARCHAR | | CRQ title |
| `assignee` | VARCHAR | | Assigned person/team |
| `description` | VARCHAR | | CRQ description |
| `remedy_status` | VARCHAR | | Status fetched from Remedy API |
| `approved` | BOOLEAN | | True if remedy_status == approved-status |
| `email_sent` | BOOLEAN | | True after notification email is sent |
| `email_sent_at` | TIMESTAMP | | When the email was sent |
| `processed_at` | TIMESTAMP | | When this record was created |
| `last_updated_in_excel` | TIMESTAMP | | Value from Excel column 4 |
| `batch_type` | VARCHAR | | SCHEDULED or ADHOC |
| `batch_run_at` | TIMESTAMP | | Batch run identifier/start time |

### Table: `processing_log`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Surrogate key |
| `run_at` | TIMESTAMP | | When the run started |
| `batch_type` | VARCHAR | | SCHEDULED or ADHOC |
| `total_crqs_read` | INT | | Number of CRQ rows parsed from Excel |
| `approved_count` | INT | | Number of CRQs with approved status |
| `emails_sent` | INT | | 1 if consolidated email was sent, 0 otherwise |
| `status` | VARCHAR | | SUCCESS, PARTIAL, or FAILED |
| `error_message` | VARCHAR(2000) | | Error detail if status != SUCCESS |
| `triggered_by` | VARCHAR | | "SCHEDULER" or user name (ad-hoc) |

### DDL Notes
- H2 (dev/mock): `spring.jpa.hibernate.ddl-auto=create-drop` (mock) / `update` (dev)
- PostgreSQL (SIT): `spring.jpa.hibernate.ddl-auto=update` — schema is managed by Hibernate; no migration tool is used
- Do not combine `AUTO_SERVER=TRUE` with `DB_CLOSE_ON_EXIT=FALSE` in H2 2.x — it will fail

---

## 7. Processing Pipeline

`CrqService.runJob(batchType, fromDateTime, toDateTime)` is the single entry point for both scheduled and ad-hoc runs.

```
1. OneDrivePort.downloadExcelFile()
       ↓ InputStream
2. ExcelService.parseExcel(stream)
       ↓ List<ExcelCrqRow>  (skips header row; skips rows with blank CRQ#)
3. [If ADHOC] filter rows where lastUpdatedInExcel ∈ [fromDateTime, toDateTime]
       ↓ filtered List<ExcelCrqRow>
4. For each row:
   a. RemedyPort.getCrqStatus(crqNumber) → remedyStatus
   b. RemedyPort.isApproved(remedyStatus) → approved (boolean)
   c. Build Crq entity; set batchType, batchRunAt, processedAt
   d. crqRepository.save(crq)
5. Collect all Crq entities where approved == true
6. If approvedList is not empty:
   a. EmailPort.sendApprovalEmail(approvedList)
   b. For each approved Crq: set emailSent=true, emailSentAt=now; save
7. Build ProcessingLog:
   - status = SUCCESS (no errors) / PARTIAL (some errors) / FAILED (total failure)
   - totalCrqsRead, approvedCount, emailsSent
8. processingLogRepository.save(log)
```

Error handling: individual Remedy API failures are caught per CRQ, logged, and processing continues. The final log status reflects whether all, some, or no CRQs were processed successfully.

---

## 8. REST API Specification

Base path: `/api/crq`

### GET /dashboard

Returns dashboard summary for the current day.

**Response: `DashboardDto`**
```json
{
  "totalToday": 10,
  "approvedToday": 6,
  "emailsSentToday": 1,
  "pendingToday": 4,
  "lastScheduledRun": "2026-05-11T17:30:00",
  "lastAdhocRun": "2026-05-11T14:22:10",
  "recentCrqs": [ /* List<CrqDto> — today's CRQs */ ],
  "recentLogs": [ /* List<ProcessingLogDto> — last 10 logs */ ]
}
```

---

### GET /list

Returns all CRQ records, ordered by `processedAt` descending.

**Response: `List<CrqDto>`**
```json
[
  {
    "id": 1,
    "crqNumber": "CRQ000001234",
    "title": "Network change",
    "assignee": "John Smith",
    "description": "...",
    "remedyStatus": "Request in Change",
    "approved": true,
    "emailSent": true,
    "emailSentAt": "2026-05-11T17:31:05",
    "processedAt": "2026-05-11T17:30:45",
    "lastUpdatedInExcel": "2026-05-11T16:00:00",
    "batchType": "SCHEDULED",
    "batchRunAt": "2026-05-11T17:30:00"
  }
]
```

---

### GET /logs

Returns all processing log entries, ordered by `runAt` descending.

**Response: `List<ProcessingLogDto>`**
```json
[
  {
    "id": 1,
    "runAt": "2026-05-11T17:30:00",
    "batchType": "SCHEDULED",
    "totalCrqsRead": 10,
    "approvedCount": 6,
    "emailsSent": 1,
    "status": "SUCCESS",
    "errorMessage": null,
    "triggeredBy": "SCHEDULER"
  }
]
```

---

### POST /adhoc

Triggers an ad-hoc CRQ processing run for CRQs updated within the given date-time range.

**Request Body:**
```json
{
  "triggeredBy": "jane.doe",
  "fromDateTime": "2026-05-11T14:00:00",
  "toDateTime": "2026-05-11T17:30:00"
}
```

**Validation:**
- `triggeredBy`: required, not blank
- `fromDateTime`: required, ISO-8601 local date-time string
- `toDateTime`: required, ISO-8601 local date-time string; must be after `fromDateTime`

**Response: `ProcessingLogDto`** (the log entry created for this run)

**HTTP 400** returned if validation fails.

---

### POST /run-now

Forces an immediate full scheduled run (all rows, no date filter). Administrative use only.

**Request Body:** none  
**Response: `ProcessingLogDto`**

---

## 9. Frontend Architecture

### Component Tree

```
App.jsx
├── Sidebar (navigation — Dashboard, CRQ List, Ad-Hoc Approval, Processing Logs)
└── Main content area (conditionally rendered by activeTab state)
    ├── Dashboard.jsx        ← activeTab === 'dashboard'
    ├── CrqTable.jsx         ← activeTab === 'crqList'
    ├── AdHocApproval.jsx    ← activeTab === 'adhoc'
    └── ProcessingLogs.jsx   ← activeTab === 'logs'
```

### State Management

- `App.jsx` holds `activeTab` state (string); no global store
- Each component manages its own `data`, `loading`, and `error` states via `useState`
- No context, no Redux, no Zustand

### API Layer (`src/services/api.js`)

All HTTP calls go through a single Axios instance with `baseURL: '/api/crq'`. In development the Vite dev server proxies `/api` to `http://localhost:8081`.

| Function | Method | Path | Returns |
|---|---|---|---|
| `getDashboard()` | GET | `/dashboard` | DashboardDto |
| `getAllCrqs()` | GET | `/list` | CrqDto[] |
| `getLogs()` | GET | `/logs` | ProcessingLogDto[] |
| `triggerAdhoc(by, from, to)` | POST | `/adhoc` | ProcessingLogDto |
| `runNow()` | POST | `/run-now` | ProcessingLogDto |

### Client-Side Filtering (CrqTable)

Filtering in `CrqTable.jsx` is done entirely client-side after fetching the full list. The component holds the full dataset in memory and applies search/filter predicates on render. No server-side pagination is implemented.

---

## 10. Scheduler Configuration

```
Class:  com.crq.approval.scheduler.CrqScheduler
Method: @Scheduled(cron = "${crq.scheduler.cron}")
Default cron: 0 30 17 * * ?  →  5:30 PM every day
```

The cron expression is configurable per environment via `application.properties`. Spring's `@EnableScheduling` is declared on the main application class.

---

## 11. CORS Configuration

`WebConfig.java` registers a CORS mapping for `/api/**`:

| Setting | Value |
|---|---|
| Allowed Origins | Comma-separated list from `cors.allowed-origins` property |
| Allowed Methods | GET, POST, PUT, DELETE, OPTIONS |
| Allowed Headers | `*` |
| Allow Credentials | true |

Typical dev value: `http://localhost:5173,http://localhost:3000`

---

## 12. Configuration Properties Reference

### application.properties (shared keys)

| Property | Description |
|---|---|
| `server.port` | HTTP port (default: 8081) |
| `crq.scheduler.cron` | Cron expression for scheduled run |
| `crq.remedy.base-url` | Remedy REST API base URL |
| `crq.remedy.username` | Remedy API username |
| `crq.remedy.password` | Remedy API password |
| `crq.remedy.approved-status` | Status string that means "approved" |
| `crq.onedrive.tenant-id` | Azure AD tenant ID |
| `crq.onedrive.client-id` | Azure AD app client ID |
| `crq.onedrive.client-secret` | Azure AD app client secret |
| `crq.onedrive.user-email` | OneDrive owner's email (used to locate drive) |
| `crq.onedrive.file-path` | Path to Excel file within OneDrive |
| `crq.email.from` | Sender email address |
| `crq.email.to` | Recipient email address(es) |
| `crq.email.subject` | Email subject line |
| `spring.mail.host` | SMTP host |
| `spring.mail.port` | SMTP port |
| `spring.mail.username` | SMTP username |
| `spring.mail.password` | SMTP password |
| `cors.allowed-origins` | Comma-separated allowed CORS origins |

### application-sit.properties (SIT overrides)

| Property | Value |
|---|---|
| `spring.datasource.url` | PostgreSQL JDBC URL |
| `spring.datasource.username` | PostgreSQL username |
| `spring.datasource.password` | PostgreSQL password |
| `spring.jpa.database-platform` | `org.hibernate.dialect.PostgreSQLDialect` |
| `spring.jpa.hibernate.ddl-auto` | `update` |

---

## 13. Build and Deployment

### Development Build

```bash
# Backend (mock mode — no credentials needed)
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run -Dspring-boot.run.profiles=mock

# Frontend (separate dev server, proxies /api to :8081)
cd frontend
npm install && npm run dev
```

### SIT Deployment Package

`build-sit.sh` produces `crq-approval-sit-1.0.0.zip` containing:

```
crq-approval-sit-1.0.0/
├── crq-approval-1.0.0.jar     # Fat JAR with React embedded at /static
├── application-sit.properties # Environment config (fill in credentials)
├── start.sh                   # java -jar -Dspring.profiles.active=sit ...
└── stop.sh                    # Kills the running process
```

The Maven `package` phase copies `frontend/dist/` to `target/classes/static/` via the `maven-resources-plugin`, so Spring Boot's static resource handler serves the React app at `/`.

### Maven Build Steps (package phase)

1. `mvn clean` — cleans target
2. `npm install && npm run build` — React build to `frontend/dist/`
3. `maven-resources-plugin` copies `frontend/dist/` → `target/classes/static/`
4. Spring Boot Maven plugin packages the fat JAR

---

## 14. Known Technical Constraints

| Constraint | Detail |
|---|---|
| Java version | System Maven defaults to Java 8. All `mvnw` commands must be prefixed with `JAVA_HOME=$(/usr/libexec/java_home -v 17)` |
| H2 2.x compatibility | Cannot combine `AUTO_SERVER=TRUE` with `DB_CLOSE_ON_EXIT=FALSE` in H2 2.x JDBC URL |
| Graph SDK v6 | `users().byUserId().drive()` returns a limited builder. Must resolve drive ID first via `.drive().get()`, then call `drives().byDriveId(id).items().byDriveItemId("root:/path:")` |
| Hibernate 6 JPQL | `DATE()` function returns `Object`, not `LocalDate`, breaking comparisons. Use `findByProcessedAtBetween(LocalDateTime, LocalDateTime)` instead |
| No test suite | No JUnit or Vitest tests exist. `./mvnw test` and `npm test` have nothing to run |
| No linting tools | No Checkstyle, SpotBugs, ESLint, or Prettier is configured |
| react-router-dom | Installed as a dependency but not used — all navigation is `useState` tab switching |
| Client-side filtering | CRQ list filtering is entirely in-browser; all records are fetched on load — this will not scale to large datasets |
