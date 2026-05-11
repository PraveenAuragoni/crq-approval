# Functional Specification Document (FSD)
## CRQ Approval Automation System

**Version:** 1.0  
**Date:** 2026-05-11  
**Status:** Draft

---

## 1. Purpose

This document describes the functional requirements of the CRQ Approval Automation System. The system automates the daily process of identifying Change Request (CRQ) records that have reached the "Request in Change" (approved) status in the Remedy ITSM platform, and notifying relevant stakeholders via email.

---

## 2. Background and Business Context

Change Request (CRQ) records are managed in the Remedy ITSM platform. A master list of CRQs is maintained in a Microsoft Excel file stored on OneDrive. Currently, operations teams manually check Remedy each day to identify newly approved CRQs and notify approvers by email — a time-consuming and error-prone process.

This system eliminates that manual effort by:
- Automatically reading the CRQ list from OneDrive each day at 5:30 PM
- Checking each CRQ's status against the Remedy API
- Sending a single consolidated approval notification email for all CRQs that reached the approved status
- Providing a web dashboard for monitoring run history and manually triggering ad-hoc runs

---

## 3. Stakeholders

| Role | Description |
|---|---|
| Operations Team | Receives approval notification emails; may trigger ad-hoc runs via the UI |
| Change Manager | Maintains the OneDrive Excel file with the CRQ list |
| System Administrator | Configures credentials, scheduler, and deploys the application |

---

## 4. Functional Requirements

### 4.1 Scheduled Daily CRQ Processing

**FR-01** The system shall automatically trigger a CRQ processing run every day at 5:30 PM.

**FR-02** On each scheduled run, the system shall download the current CRQ Excel file from a configured OneDrive location.

**FR-03** The system shall parse all CRQ rows from the Excel file. The expected column order is:
| Column Index | Field |
|---|---|
| 0 | CRQ Number |
| 1 | Title |
| 2 | Assignee |
| 3 | Description |
| 4 | Last Updated (date/time) |

**FR-04** For each CRQ, the system shall query the Remedy REST API to retrieve the current status.

**FR-05** A CRQ is considered **approved** if its Remedy status equals the value configured in `crq.remedy.approved-status` (default: `"Request in Change"`).

**FR-06** The system shall send a single consolidated email notification listing all CRQs that are approved in the current run. If no CRQs are approved, no email is sent.

**FR-07** After sending the email, the system shall record `emailSent = true` and `emailSentAt = <timestamp>` against each approved CRQ.

**FR-08** After each run (success or failure), the system shall create a processing log entry recording the outcome.

---

### 4.2 Ad-Hoc CRQ Processing

**FR-09** An authorised user shall be able to trigger a CRQ processing run manually from the web UI at any time.

**FR-10** The ad-hoc run form shall require the following inputs:
- **Name / User ID** — the name of the person triggering the run (free-text, required)
- **From Date-Time** — start of the date-time range for filtering CRQs (required)
- **To Date-Time** — end of the date-time range for filtering CRQs (required)

**FR-11** The ad-hoc run shall only process CRQs whose `Last Updated` timestamp (from the Excel file) falls within the specified From–To range (inclusive).

**FR-12** The ad-hoc run shall follow the same processing logic as the scheduled run (Remedy status check, email, logging), but is labelled with batch type `ADHOC` and records the triggering user's name.

**FR-13** The UI shall validate that:
- Name/User ID is not blank
- Both From and To date-times are provided
- From date-time is earlier than To date-time

**FR-14** After the run completes, the UI shall display the result summary including: run timestamp, batch type, total CRQs read, approved count, emails sent count, and status.

---

### 4.3 Email Notification

**FR-15** The approval email shall be a consolidated notification listing all approved CRQs from the current run.

**FR-16** The email recipient(s) shall be configurable via the `crq.email.to` property.

**FR-17** The email sender address shall be configurable via the `crq.email.from` property.

**FR-18** The email subject shall be configurable via the `crq.email.subject` property.

---

### 4.4 Dashboard

**FR-19** The dashboard shall display the following summary statistics for the current day:
- Total CRQs processed today
- Approved CRQs today
- Emails sent today
- Pending (non-approved) CRQs today

**FR-20** The dashboard shall display the timestamp of the last scheduled run and the last ad-hoc run.

**FR-21** The dashboard shall show a table of CRQs processed today, including CRQ number, title, assignee, Remedy status, approval badge, and email sent status.

**FR-22** The dashboard shall show the 10 most recent processing log entries.

**FR-23** The dashboard shall provide a Refresh button to reload data without navigating away.

---

### 4.5 CRQ List

**FR-24** A dedicated screen shall show all CRQ records across all runs.

**FR-25** The list shall support text search across CRQ number, title, and assignee (case-insensitive).

**FR-26** The list shall support filtering by status:
- **ALL** — all records
- **APPROVED** — only approved CRQs
- **PENDING** — only non-approved CRQs
- **EMAIL_SENT** — only CRQs where the notification email has been sent

**FR-27** The list shall display a record count showing `X of Y` (filtered vs total).

---

### 4.6 Processing Logs

**FR-28** A dedicated screen shall show all processing log entries across all runs, sorted by most recent first.

**FR-29** Each log entry shall display: run timestamp, batch type (SCHEDULED/ADHOC), total CRQs read, approved count, emails sent, status (SUCCESS / PARTIAL / FAILED), triggered-by (SCHEDULER or user name), and any error message.

**FR-30** Long error messages shall be truncated in the table display to keep the view readable.

---

### 4.7 Manual Full Run Trigger

**FR-31** The system shall expose a backend endpoint (`POST /api/crq/run-now`) to force a full scheduled run immediately. This is intended for administrative/testing use.

---

## 5. Use Cases

### UC-01: Scheduled Daily Run

**Actor:** System (CrqScheduler)  
**Trigger:** Clock reaches 5:30 PM  
**Flow:**
1. Scheduler fires; calls `CrqService.runScheduledJob()`
2. Excel file downloaded from OneDrive
3. All rows parsed; Remedy status fetched for each CRQ
4. Each CRQ saved to the database
5. All approved CRQs collected; consolidated email sent
6. Email sent status updated on each approved CRQ
7. ProcessingLog created with SUCCESS / PARTIAL / FAILED status

**Alternate Flow (Remedy API error):** System logs the error per CRQ, marks `remedyStatus = "ERROR"`, and continues processing remaining CRQs. Log is recorded as PARTIAL if some succeeded.

**Alternate Flow (OneDrive unavailable):** System catches exception; ProcessingLog recorded as FAILED with error message; no email sent.

---

### UC-02: Ad-Hoc Run from UI

**Actor:** Operations Team member  
**Trigger:** User submits Ad-Hoc Approval form  
**Pre-condition:** User has provided a valid name and a valid date-time range  
**Flow:**
1. UI sends `POST /api/crq/adhoc` with triggeredBy, fromDateTime, toDateTime
2. Backend filters Excel rows to those within the date-time range
3. Same processing pipeline as UC-01 runs on filtered rows
4. Result DTO returned to UI; UI displays the run summary

---

### UC-03: Monitor Dashboard

**Actor:** Operations Team member  
**Trigger:** User opens the web application or clicks Refresh  
**Flow:**
1. UI calls `GET /api/crq/dashboard`
2. Dashboard displays today's stats, last run times, recent CRQs, and recent logs

---

### UC-04: Search and Filter CRQ Records

**Actor:** Operations Team member  
**Trigger:** User navigates to CRQ List and enters search text or clicks a filter  
**Flow:**
1. UI loads all records via `GET /api/crq/list` on mount
2. User types in search box — UI filters client-side by CRQ number, title, or assignee
3. User clicks APPROVED / PENDING / EMAIL_SENT — UI applies the corresponding filter
4. Record count updates to reflect filtered results

---

## 6. Business Rules

| Rule ID | Description |
|---|---|
| BR-01 | A CRQ is approved if and only if its Remedy status equals `crq.remedy.approved-status` |
| BR-02 | The email is sent once per batch run; individual per-CRQ emails are not sent |
| BR-03 | Ad-hoc runs filter on `lastUpdatedInExcel` — not on `processedAt` or the current time |
| BR-04 | The first row of the Excel file is treated as a header and skipped |
| BR-05 | Rows with a blank CRQ number are skipped during parsing |
| BR-06 | Scheduled runs process all rows in the Excel file regardless of date |
| BR-07 | Processing logs are always created regardless of success or failure |

---

## 7. Screen Inventory

| Screen | Navigation Label | Primary Actions |
|---|---|---|
| Dashboard | Dashboard | View stats, refresh, view recent CRQs and logs |
| CRQ List | CRQ List | Search, filter, view all CRQ records |
| Ad-Hoc Approval | Ad-Hoc Approval | Fill form, trigger run, view result |
| Processing Logs | Processing Logs | View all historical run logs, refresh |

---

## 8. Non-Functional Requirements

| NFR | Description |
|---|---|
| NFR-01 | Scheduled run must complete within the processing window before the next scheduled time |
| NFR-02 | The UI must load the dashboard within 3 seconds under normal conditions |
| NFR-03 | All external credentials (OneDrive, Remedy, SMTP) must be stored in configuration files, not in source code |
| NFR-04 | The application must run as a single deployable JAR with no external web server |
| NFR-05 | The mock profile must allow full demonstration without any external service credentials |
