---
name: crq-pr-review
description: >
  Project-specific PR code review skill for the crq-approval repository (Spring Boot 3.2 / Java 17 backend + React 18 frontend).
  Use this skill whenever the user asks to review a PR, check a branch, or validate code changes in the crq-approval project.
  It runs a full structured review covering correctness, static analysis, security, performance, and project conventions,
  then presents a ✅/❌/⚠️ checklist and offers to auto-fix any issues found.
---

# CRQ Approval — PR Review Skill

You are a senior developer who knows the crq-approval codebase deeply. Run a thorough review and present results as a structured checklist. Then offer to fix any issues automatically.

## Step 1 — Fetch the PR

If a PR number is given, run:
```bash
gh pr view <number>
gh pr diff <number>
```
If no PR number is given, run `gh pr list` and ask the user which one to review.
Always run from the repo root: `/Users/auragonipraveengoud/Documents/claudecodews/crq-approval`

---

## Step 2 — Run the full checklist

Evaluate every item below against the diff. Assign a status to each:
- ✅ Pass — looks good
- ❌ Fail — definite problem, must fix
- ⚠️ Warning — potential issue or suggestion
- ➖ N/A — not applicable to this PR

---

### A. PR Hygiene

| # | Check | Status | Notes |
|---|-------|--------|-------|
| A1 | **Branch name** follows `<type>/<short-description>` (e.g. `feat/date-range-filter`, `fix/null-pointer`) | | |
| A2 | **PR title** is concise (<70 chars), uses conventional prefix (`feat:`, `fix:`, `refactor:`, `chore:`) | | |
| A3 | **PR description** has a Summary section explaining what and why | | |
| A4 | **PR description** has a Test plan / checklist | | |
| A5 | **Commit messages** follow conventional commits format | | |
| A6 | No merge commits or WIP commits in the branch | | |

---

### B. Correctness

| # | Check | Status | Notes |
|---|-------|--------|-------|
| B1 | No logic errors or off-by-one in date/time range filtering | | |
| B2 | Null checks present where inputs can be null (request body, optional fields) | | |
| B3 | Exception types caught are specific, not bare `Exception` where avoidable | | |
| B4 | `LocalDateTime` / `LocalDate` used correctly (no timezone confusion) | | |
| B5 | New fields on JPA entities have appropriate column definitions | | |

---

### C. Project Conventions (Java)

| # | Check | Status | Notes |
|---|-------|--------|-------|
| C1 | `Stream.toList()` used instead of `collect(Collectors.toList())` (Java 17, Sonar S6204) | | |
| C2 | `saveAll()` used for batch DB updates — not `save()` in a loop | | |
| C3 | `ResponseEntity<Object>` used for mixed response types — not `ResponseEntity<?>` (Sonar S1452) | | |
| C4 | `DateTimeFormatter` instances are `static final` constants — not instantiated per request | | |
| C5 | New external services implement the correct port interface (`OneDrivePort`, `RemedyPort`, `EmailPort`) | | |
| C6 | Mock beans annotated with `@Profile("mock")`, real beans with `@Profile("!mock")` | | |
| C7 | No duplicate imports; no unused imports left in changed files | | |
| C8 | Javadoc on public methods stays accurate after changes | | |

---

### D. Static Analysis / Sonar

| # | Check | Status | Notes |
|---|-------|--------|-------|
| D1 | No raw wildcard return types (`ResponseEntity<?>`, `List<?>`) | | |
| D2 | No non-ASCII characters in log messages (arrows `→`, etc.) — use `->` | | |
| D3 | No fields that can be `static final` but aren't | | |
| D4 | No stale/dead code left over from the change | | |
| D5 | Javadoc comments don't describe removed behaviour | | |

---

### E. Security

| # | Check | Status | Notes |
|---|-------|--------|-------|
| E1 | No credentials, tokens, or secrets added to any file | | |
| E2 | User-supplied strings are logged but never executed | | |
| E3 | No SQL/JPQL built by string concatenation | | |
| E4 | `@RequestBody` inputs validated before use (nulls, format, range) | | |

---

### F. Performance

| # | Check | Status | Notes |
|---|-------|--------|-------|
| F1 | No N+1 DB queries introduced (check loops that call `save()` or `findBy*()`) | | |
| F2 | No unnecessary full-table loads where a filtered query would do | | |
| F3 | Excel parsing / OneDrive download not triggered more than once per job run | | |

---

### G. Backward Compatibility

| # | Check | Status | Notes |
|---|-------|--------|-------|
| G1 | Existing REST API contracts unchanged (endpoints, request/response shape) | | |
| G2 | No removal of `application.properties` keys that SIT/prod environments rely on | | |
| G3 | JPA entity changes won't break existing H2 or PostgreSQL schema (`ddl-auto=update` safe) | | |
| G4 | Frontend API calls still match backend endpoint signatures | | |

---

### H. Test & Quality

| # | Check | Status | Notes |
|---|-------|--------|-------|
| H1 | Mock profile (`MockDataInitializer`) still seeds valid data after this change | | |
| H2 | New service methods have updated signatures reflected in all callers | | |
| H3 | Frontend client-side validation matches backend validation rules | | |

---

## Step 3 — Summary

After filling the table, produce:

```
## Review Summary

### Must Fix (❌)
- <item> — <one-line reason>

### Suggestions (⚠️)
- <item> — <one-line reason>

### Passed (✅)
- A–H all clear (list categories)
```

Give an overall verdict:
- **APPROVED** — no ❌ items
- **CHANGES REQUESTED** — one or more ❌ items
- **APPROVED WITH SUGGESTIONS** — no ❌ but has ⚠️ items

---

## Step 4 — Auto-fix offer

After the summary, always ask:

> **Would you like me to auto-fix the ❌ and ⚠️ items?**
> I can fix: [list the specific items by ID, e.g. C1, D2, E4]
> Just say "yes fix all" or name specific items (e.g. "fix C1 and D2").

When the user confirms:
1. Apply fixes directly to the source files
2. Compile the backend to verify: `cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./mvnw compile -q`
3. Commit the fixes to the same branch with message: `fix: address PR review findings (<item IDs>)`
4. Push to remote

---

## Known Gotchas (check these specifically)

- **H2 URL**: Never combine `AUTO_SERVER=TRUE` with `DB_CLOSE_ON_EXIT=FALSE`
- **Graph SDK v6**: `root()` removed — must resolve drive ID first via `.drive().get()` then use `drives().byDriveId()`
- **Hibernate 6 JPQL**: `DATE()` returns `Object` — use `findByProcessedAtBetween(LocalDateTime, LocalDateTime)` instead
- **Excel columns**: 0=CRQ Number, 1=Title, 2=Assignee, 3=Description, 4=Last Updated
- **Java version**: JAVA_HOME must point to `/opt/homebrew/opt/openjdk@17`
- **Vite**: Only supports up to v7 with current `@vitejs/plugin-react` — don't upgrade to v8
