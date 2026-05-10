# CRQ Approval System

Automates reading CRQs from an OneDrive Excel file, checking their status in the Remedy API, and sending approval emails — with a React dashboard for monitoring and ad-hoc runs.

---

## Architecture

```
OneDrive (Excel) → Spring Boot → Remedy API → Email (SMTP)
                       ↕
                 H2 / PostgreSQL
                       ↕
                  React Dashboard
```

---

## Features

| Feature | Details |
|---|---|
| Scheduled Job | Daily at **5:30 PM** via Spring `@Scheduled` |
| Remedy Check | Calls Remedy REST API, checks for **"Request in Change"** status |
| Email Notification | Sends HTML email with all approved CRQs |
| Dashboard | Live stats, today's CRQs, last run times |
| Ad-Hoc Run | Manual trigger from UI for CRQs updated **after 5:30 PM** |
| Processing Logs | Full audit log of every scheduled and ad-hoc run |

---

## Quick Start

### Backend

```bash
cd backend
# Fill in application.properties (see Configuration below)
./mvnw spring-boot:run
```

Runs at: `http://localhost:8080`
H2 Console: `http://localhost:8080/h2-console`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at: `http://localhost:5173`

---

## Configuration (`backend/src/main/resources/application.properties`)

### OneDrive (Microsoft Graph API)

1. Register an app in Azure Portal → **App registrations**
2. Grant permissions: `Files.Read.All` (Application permission)
3. Create a client secret
4. Fill in:

```properties
onedrive.tenant-id=YOUR_AZURE_TENANT_ID
onedrive.client-id=YOUR_AZURE_CLIENT_ID
onedrive.client-secret=YOUR_AZURE_CLIENT_SECRET
onedrive.file-path=/CRQ/crq-list.xlsx      # path in OneDrive
onedrive.drive-user=user@company.com        # or "me" for delegated
```

### Remedy API

```properties
remedy.base-url=https://your-remedy-server.example.com/api
remedy.username=YOUR_REMEDY_USERNAME
remedy.password=YOUR_REMEDY_PASSWORD
remedy.approved-status=Request in Change
```

### Email (SMTP)

```properties
spring.mail.host=smtp.office365.com        # or smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@company.com
spring.mail.password=YOUR_EMAIL_PASSWORD
email.from=crq-approval@company.com
email.to=team@company.com,manager@company.com
```

### Scheduler

```properties
# Default: every day at 5:30 PM
crq.scheduler.cron=0 30 17 * * ?
# Ad-hoc cutoff time (HH:mm)
crq.adhoc.threshold-time=17:30
```

---

## Excel File Format

The OneDrive Excel file must have **row 1 as headers** and data from row 2:

| Column A | Column B | Column C | Column D | Column E |
|---|---|---|---|---|
| CRQ Number | Title | Assignee | Description | Last Updated |

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/crq/dashboard` | Dashboard stats + recent data |
| GET | `/api/crq/list` | All CRQ records |
| GET | `/api/crq/logs` | All processing logs |
| POST | `/api/crq/adhoc` | Trigger ad-hoc run (body: `{"triggeredBy":"name"}`) |
| POST | `/api/crq/run-now` | Force full scheduled run manually |

---

## Production Database (PostgreSQL)

Uncomment in `pom.xml` and `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/crqdb
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```
