# AI Backend Testing Guidelines

> **Purpose:** This document is the mandatory testing standard for AI agents and developers working on this backend. Read it before creating, modifying, or refactoring backend code. Use it to derive tests for the exact domain and behavior being changed.
>
> **Repository:** `xuandai19/PW_YardMS_BE`  
> **Backend stack observed in this repository:** Java, Spring Boot, Maven, Spring Data JPA, PostgreSQL, Spring Security/JWT, Bean Validation, JUnit 5, Mockito, Spring Boot Test, H2 and Testcontainers.
>
> **Important:** This is a testing policy, not a substitute for business requirements. Never invent business rules, field limits, role permissions, status transitions, currency rules, or expected HTTP responses. Inspect the current domain implementation, DTOs, entities, migrations, API contracts, security configuration and existing tests first. If a requirement is unclear, document the assumption or ask for clarification.

---

## 1. Mandatory instructions for AI

Whenever asked to create or change backend functionality:

1. Identify the affected domain(s), API endpoints, service methods, entities, repositories, events/jobs and external integrations.
2. Read this document and inspect the actual implementation before writing tests.
3. Inspect existing tests and follow their conventions. Reuse existing test utilities and fixtures where appropriate.
4. Derive test cases from:
   - Explicit business requirements/specifications.
   - Existing behavior and API contracts.
   - Validation annotations and custom validators.
   - Database constraints and Flyway migrations.
   - Authorization rules and security configuration.
   - The common checklist in this document.
5. Create or update tests in the same change as the production code. Do not leave testing as an optional future task.
6. Cover both successful and unsuccessful paths, boundary values, authorization, persistence and relevant side effects.
7. Run the relevant tests. Prefer running the full backend test suite after domain-level tests pass.
8. Fix failures caused by the change. Never delete, weaken, skip or modify an existing test merely to make the build pass unless the requirement itself has intentionally changed and the reason is documented.
9. Do not claim tests passed unless the test command was actually executed and its result was checked.
10. In the final summary, report changed files, test cases added/updated, commands executed, pass/fail results and any untested risks.

### Definition of Done

A backend change is not complete until:

- [ ] Applicable tests have been added or updated.
- [ ] Positive and negative scenarios are covered.
- [ ] Validation and boundary cases are covered.
- [ ] Authentication and authorization are tested where applicable.
- [ ] Database state and side effects are verified where applicable.
- [ ] Existing relevant tests still pass.
- [ ] No test relies on production data, real credentials or uncontrolled external services.
- [ ] Test results and known limitations are reported.

---

## 2. Test levels and technology

Choose the smallest test level that proves the behavior. Use more than one level when necessary.

| Test level | What to verify | Preferred approach |
|---|---|---|
| Unit test | Pure business rules, calculations, validators, mapping and service branches | JUnit 5 + Mockito |
| Repository test | Queries, filters, sorting, constraints and persistence mapping | Spring Data JPA test; PostgreSQL-compatible test database |
| Controller/API test | Request validation, response status/body, serialization and exception mapping | MockMvc / Spring Boot Test |
| Security test | Authentication, role/authority checks and ownership boundaries | Spring Security Test + MockMvc |
| Integration test | Multiple layers working together, transactions and database effects | Spring Boot Test + Testcontainers |
| Async/integration adapter test | Kafka, MQTT, scheduled jobs, mail, file storage and external APIs | Mocked ports/adapters or isolated test containers |
| Smoke test | Application context and essential endpoints start correctly | Spring Boot Test |

### Test design rules

- Prefer deterministic tests: same input and controlled dependencies must produce the same result.
- Use Arrange – Act – Assert (Given – When – Then).
- Give tests descriptive names, preferably `methodName_condition_expectedResult`.
- Each test should verify one behavior; avoid tests that depend on execution order.
- Do not use arbitrary `Thread.sleep()` for asynchronous tests. Use Awaitility, latches, polling with a bounded timeout, or the project's existing async test utility.
- Use fixed `Clock` / fixed timestamps for time-dependent business rules.
- Use isolated test data. Clean up database state between tests.
- Do not call real payment, email, GPS, MQTT, Kafka, MinIO or other external production services from ordinary unit tests.
- Do not assert only that a method returned successfully. Verify the meaningful result, persisted state, emitted event or interaction.
- Avoid over-mocking repository behavior in tests whose purpose is to verify database queries.
- Do not test private methods directly. Test observable behavior through public methods or API endpoints.

---

## 3. Common backend test checklist

Apply every relevant item. Mark items **N/A** only when they genuinely do not apply to the changed backend behavior. UI-only checks must be assigned to frontend/UI tests rather than fabricated as backend tests.

| ID | Concern | Backend test expectation |
|---|---|---|
| B01 | Required fields | Missing, null and blank required values are rejected. Verify validation annotations/custom validators and API error response. |
| B02 | Validation messages | API returns a clear, field-identifiable validation error using the project's standard error response format. |
| B03 | Consistent error format | Validation, business, not-found, conflict and unexpected errors follow the common API error contract. Do not leak stack traces or internal details. |
| B04 | Confirmation before delete | Backend enforces delete preconditions and business rules. UI confirmation dialogs are verified in frontend tests. |
| B05 | Select all / deselect all | For bulk APIs, test empty selection, valid selection, duplicate IDs, invalid IDs, partial failures and authorization for every affected record. UI selection controls are frontend tests. |
| B06 | Currency and money | Verify currency, precision, rounding, scale, non-negative/negative rules and overflow according to specification. Use `BigDecimal` for monetary values; never assume a currency or scale. |
| B07 | Default sorting | Verify documented default ordering when no sort is supplied. UI table presentation is frontend-owned. |
| B08 | Reset behavior | Verify backend defaults only when the API has defined default/filter semantics. Form reset buttons are frontend tests. |
| B09 | Numeric formatting | Verify numeric type, precision, scale and serialization contract. Presentation formatting is frontend-owned. |
| B10 | Maximum values | Test exact maximum, maximum + smallest invalid increment, and values beyond supported numeric/database range. Invalid values must not be persisted. |
| B11 | Special characters | Test quotes, apostrophes, Unicode, accents, emoji, control characters and injection-shaped input. Accept legitimate text; reject/encode only according to field rules. Verify parameterized queries and safe output handling. |
| B12 | Naming conventions | Review DTO/API/property names and resource keys against repository conventions. Usually a code-review check, not a runtime test. |
| B13 | Sorting after mutations | After create/update/delete, verify list queries return correct records and ordering. Test pagination boundaries and stable ordering where relevant. |
| B14 | Timeout | Test configured client/request/job timeouts and graceful failure behavior for integrations. Do not create flaky tests based on real elapsed network delays. |
| B15 | Cookies and tokens | Verify cookie/token creation, attributes, expiration, refresh/revocation and clearing behavior where used. Never expose secrets in responses or logs. |
| B16 | File download | Verify authorization, correct content type, filename, content/bytes, missing file behavior and path traversal protection. |
| B17 | Resource configuration | Verify configurable values are read from configuration/database as designed, with safe defaults and validation. Avoid hard-coded environment-specific secrets. |
| B18 | Resource key naming | Check resource/message key consistency and missing-key behavior. |
| B19 | Markup validation | HTML/CSS markup is frontend scope. Backend tests may verify generated document content or response content type when the backend produces files/documents. |
| B20 | Crash/unavailable behavior | Verify health/readiness endpoints and controlled error responses. Reverse-proxy maintenance pages and browser redirects are deployment/frontend scope. |
| B21 | Spelling/grammar | Review API messages, validation messages, generated documents and user-facing backend text. |
| B22 | Numeric input | Non-numeric input for numeric DTO fields must produce a controlled 4xx response, not an unhandled 500. |
| B23 | Negative numbers | Test negative values according to each field's business rule. Do not reject negative values globally if a domain permits them. |
| B24 | Decimal values | Test integer-only and decimal fields separately, including scale and rounding rules. Do not assume all numeric fields accept decimals. |
| B25 | Button behavior | Buttons are frontend scope. Verify the corresponding backend endpoint and side effects. |
| B26 | Duplicate submit | Test duplicate/replayed requests for operations requiring idempotency. Verify unique constraints, idempotency keys or safe conflict handling where required. |
| B27 | Divide by zero | Test zero denominators and missing operands in calculations. Return a defined business/validation error; never allow an uncontrolled arithmetic failure. |
| B28 | Leading/trailing spaces | Test leading/trailing whitespace, whitespace-only input and normalization. Trim only where the field's specification allows it. |
| B29 | Privacy Policy / FAQ | Content pages are frontend/content scope. If backend exposes these resources, verify endpoint access and content retrieval. |
| B30 | Home link | Frontend navigation scope; not a backend unit test. |
| B31 | Authorization boundary | Verify users cannot access or mutate records outside their assigned role, ownership, tenant, partner, yard or other scope. |
| B32 | Database integrity | Verify foreign keys, unique constraints, nullability, transaction rollback and prevention of orphan/inconsistent records. |
| B33 | Concurrency and race conditions | For concurrent updates, inventory/capacity, booking, finance or state transitions, verify locking/versioning/atomic updates prevent inconsistent state. |

### Boundary-value minimum

For each constrained field, consider:

- Missing / null.
- Empty string.
- Whitespace-only string.
- Minimum valid value.
- Just below minimum.
- Maximum valid value.
- Just above maximum.
- Wrong data type.
- Invalid format.
- Duplicate value, if uniqueness applies.
- Unicode and special characters, if text is accepted.
- Leading/trailing spaces, if normalization applies.

Do not mechanically apply every input to every field. Select cases based on the field's actual type and specification.

---

## 4. Domain-specific test matrix

The following matrix identifies likely test areas from the repository's domain structure. It is a checklist for inspecting the implementation, **not a statement that every listed rule is already implemented**. Only create tests for behaviors confirmed by code or requirements.

### 4.1 User domain

- Create, read, update and deactivate/delete user according to supported operations.
- Required fields, format, length and normalization.
- Duplicate username/email/phone or other unique identifiers, if applicable.
- Password hashing; never store or return plaintext passwords.
- Password policy and credential update behavior.
- Role/authority assignment and protection against privilege escalation.
- User status transitions and blocked/inactive account behavior.
- User cannot read or modify another user's protected data unless authorized.
- Pagination, filtering, sorting and empty result behavior.
- Concurrent updates and uniqueness race conditions.

### 4.2 Confirmation domain

- Valid and invalid confirmation requests.
- Expired, malformed, already-used and unknown confirmation tokens/codes.
- Token/code single-use behavior where required.
- Unauthorized user cannot confirm another user's operation.
- Repeated confirmation is handled safely and idempotently where appropriate.
- State changes occur atomically; failed confirmation does not leave partial data.
- Rate limiting / attempt limits if implemented.
- Time-bound behavior tested with a fixed clock.

### 4.3 Partner domain

- Partner create/update/read/deactivate/delete according to business rules.
- Required fields, format, length, uniqueness and whitespace normalization.
- Search/filter/pagination/sorting.
- Prevent deletion or deactivation when linked records make it invalid.
- Partner data visibility is limited by role and access scope.
- Relationships to contracts, services, invoices or other domains remain consistent.
- Duplicate requests and concurrent updates.

### 4.4 Contract domain

- Contract creation and update with valid/invalid partner and related references.
- Contract number/reference uniqueness if required.
- Start/end date validation and invalid date ranges.
- Contract status transitions and prohibited transitions.
- Expiry calculations and scheduled expiry processing.
- Upload/download: file size, extension, MIME/content validation, empty file, corrupted file and unauthorized access.
- Storage failure, missing file, duplicate upload and safe cleanup behavior.
- Path traversal and unsafe filename protection.
- Related partner/user access and audit history where implemented.
- Scheduled job repeatability and idempotency.

### 4.5 Service domain

- Service create/update/read and activation/deactivation.
- Required fields, names/codes uniqueness, price and numeric constraints.
- Price precision, scale, currency and valid ranges according to specification.
- Price history: effective date, historical record preservation and correct retrieval.
- Prevent invalid service references in operational workflows.
- Search/filter/pagination/sorting.
- Concurrent price changes and transaction consistency.
- Role-based access to price or service administration.

### 4.6 Vehicle domain

- Vehicle create/update/read and supported lifecycle operations.
- Registration/plate number format and uniqueness according to local/business rules.
- Required vehicle attributes and numeric boundaries (capacity, weight, dimensions, etc. only if present).
- Invalid or inconsistent vehicle status transitions.
- Prevent deletion when the vehicle is referenced by active operations.
- Search/filter/pagination/sorting.
- Access scope and audit behavior.
- Concurrent assignment/update conflicts.

### 4.7 Yard domain

- Yard create/update/read and supported lifecycle operations.
- Required fields, unique yard code/name and valid capacity/configuration.
- Capacity boundaries: zero, negative, exact capacity and over-capacity.
- Slot/area/zone relationships and invalid references, if implemented.
- Prevent deletion when active vehicles, goods, bookings or other dependent records exist.
- Concurrent entry/exit or allocation operations do not exceed capacity.
- Search/filter/pagination/sorting and correct default ordering.
- Role/yard scope authorization.

### 4.8 Goods domain

- Goods create/update/read and supported lifecycle operations.
- Required fields, code/identifier uniqueness and valid quantities.
- Quantity boundaries: zero, negative, fractional and maximum values according to the unit/specification.
- Valid goods status transitions.
- Prevent invalid stock/quantity changes and negative inventory where prohibited.
- Related vehicle, yard, partner, service or contract references.
- Concurrent quantity updates and transaction rollback.
- Search/filter/pagination/sorting.
- Bulk operations: duplicate IDs, missing IDs, partial/atomic behavior and authorization.

### 4.9 Tracking domain

- Valid GPS payload parsing and invalid/missing coordinates.
- Latitude/longitude boundaries and invalid coordinate pairs.
- Timestamp format, timezone, stale/future events and event ordering.
- Vehicle/device identity validation and unknown device handling.
- Duplicate message/event handling and idempotency.
- Kafka/MQTT ingestion failure, retry and recovery behavior if used.
- Consumer processing is safe against duplicate delivery.
- Retention/cleanup behavior according to configured policy.
- Unauthorized access to tracking data and vehicle/yard scope.
- High-volume ingestion: bounded processing, no unbounded memory growth, and appropriate timeouts.
- Do not rely on real GPS devices or external brokers in unit tests.

### 4.10 Finance domain

- All calculations use suitable decimal arithmetic (normally `BigDecimal`).
- Zero denominator, negative, null and overflow cases.
- Rounding mode, decimal scale, currency and timezone follow explicit requirements.
- Invoice create/update/read and unique invoice identifiers where applicable.
- Invoice amount, tax, discount, interest, penalty and total calculations are verified independently.
- Payment/settlement state transitions and duplicate processing.
- Overdue detection based on fixed time and configured timezone.
- Interest/fee calculation at date boundaries and repeated scheduled execution.
- Prevent double charging/double posting and inconsistent balances.
- Transaction rollback when any step fails.
- Authorization and financial data visibility.
- Export/download: file format, content, filename, permissions and missing file handling.
- Dashboard aggregation matches source records and excludes invalid/deleted records according to specification.
- E-invoice/external finance integration errors, timeout and retry behavior are tested with mocks or test adapters.

---

## 5. Cross-domain and infrastructure tests

When a change touches these components, add the relevant tests:

### Authentication and security

- Missing token, malformed token, expired token, invalid signature and revoked token.
- Anonymous access to protected endpoints.
- Authenticated user with insufficient authority.
- Correct role/authority can perform the operation.
- User cannot escalate their role by changing request payload.
- Sensitive fields are not exposed in API responses or logs.
- CORS/CSRF/cookie behavior follows the actual security configuration.
- Login failure/lockout/rate limit behavior only if implemented.
- Password reset/confirmation tokens cannot be reused or accessed by another user.

### API and exception handling

- Correct success and error HTTP status codes.
- Stable response envelope and field-level validation details.
- Invalid JSON, wrong data types, missing parameters and unsupported content types.
- Not found, conflict, forbidden, unauthorized and unexpected exception cases.
- No stack trace, SQL, secrets or internal implementation details in public responses.
- Pagination parameters: negative page, zero/oversized size, invalid sort property and empty result.
- API backward compatibility for changed request/response contracts.

### Database and transactions

- Unique and foreign-key constraints.
- Nullability and column length/precision.
- Rollback when a multi-step operation fails.
- No orphan records after deletion/update.
- Correct cascade behavior; never assume cascade delete is safe.
- Repository query correctness for filters, joins, sorting and pagination.
- Tests that depend on PostgreSQL-specific behavior should run against PostgreSQL (prefer Testcontainers), not only H2.
- Flyway migrations are compatible with a clean database and the expected upgrade path.

### Events, messaging and scheduled jobs

- Correct event payload and routing/topic.
- Duplicate event delivery and idempotency.
- Retry, failure, dead-letter or recovery behavior where configured.
- Consumer does not acknowledge/commit before required processing is complete.
- Scheduled job handles empty data, partial failure and repeated execution.
- Timezone and daylight-saving assumptions are explicit.
- Test asynchronous behavior with bounded waits, not arbitrary sleeps.

### File and document processing

- Empty, oversized, wrong-extension, wrong-MIME and corrupted files.
- File content is validated; do not trust filename or Content-Type alone.
- Safe generated filenames and storage paths.
- Unauthorized upload/download/delete.
- Storage unavailable, file missing and partial-write cleanup.
- Correct response headers and file bytes.
- No path traversal or arbitrary file access.

---

## 6. Test placement and naming

Keep tests aligned with production package/domain structure.

Current test root observed:

`yms-service/src/test/java/com/yms/yms/`

Recommended organization:

```text
yms-service/
└── src/
    └── test/
        └── java/
            └── com/yms/yms/
                ├── domain/
                │   ├── user/
                │   ├── confirmation/
                │   ├── partner/
                │   ├── contract/
                │   ├── service/
                │   ├── vehicle/
                │   ├── yard/
                │   ├── goods/
                │   ├── tracking/
                │   └── finance/
                ├── infrastructure/
                └── [existing application/security integration tests]
```

Use the existing package layout if it differs; do not move unrelated tests just to match this example.

Naming examples:

- `UserServiceTest`
- `UserControllerIntegrationTest`
- `PartnerRepositoryTest`
- `FinanceCalculationTest`
- `TrackingEventConsumerTest`
- `ContractFileStorageTest`

Test method examples:

- `createUser_whenEmailAlreadyExists_returnsConflict`
- `createVehicle_whenRequiredFieldMissing_returnsValidationError`
- `deleteYard_whenActiveVehicleExists_rejectsDeletion`
- `calculateInterest_whenPrincipalIsZero_returnsExpectedResult`
- `consumeGpsEvent_whenMessageIsDuplicated_doesNotDuplicateRecord`

These are naming examples only. Match actual class and method names in the repository.

---

## 7. Running tests

Run commands from the repository root unless the Maven module layout requires otherwise.

Run all tests:

```bash
mvn test
```

Run tests for the service module and its required modules:

```bash
mvn test -pl yms-service -am
```

Run one test class:

```bash
mvn -pl yms-service -Dtest=UserServiceTest test
```

Run a single test method (Surefire-supported syntax):

```bash
mvn -pl yms-service -Dtest=UserServiceTest#methodName test
```

If Testcontainers tests require Docker, confirm Docker is available. If the environment cannot run Docker-dependent tests, report them as **not executed**; do not silently treat them as passed or replace PostgreSQL-specific coverage with H2 and claim equivalence.

After execution, inspect Maven's summary and test reports under:

```text
yms-service/target/surefire-reports/
```

---

## 8. AI execution template

For every code-generation task, the AI should internally follow this sequence and include a concise report in its final response:

```text
1. Change scope
   - Domain(s):
   - API/service/entity/repository/event/job affected:
   - Business requirements:

2. Applicable checklist
   - Common checklist IDs:
   - Domain-specific cases:
   - Security/database/integration cases:
   - N/A items and reasons:

3. Tests implemented
   - Unit tests:
   - Repository tests:
   - API/security tests:
   - Integration/async tests:

4. Test execution
   - Command:
   - Passed:
   - Failed:
   - Not executed:

5. Risks / assumptions
   - Unclear requirements:
   - External dependencies:
   - Remaining test gaps:
```

### Prompt instruction for AI agents

When generating or modifying backend code in this repository:

> Read `AI_TESTING_GUIDELINES.md` first. Inspect the actual affected domain and existing tests. Derive test cases from the requirements and applicable common/domain-specific checklists. Implement or update tests together with production code. Cover success, validation failures, boundaries, authorization, persistence, transaction behavior and side effects as applicable. Do not invent business rules or force frontend-only checks into backend tests. Run the relevant tests and report exact commands and results. Do not claim unexecuted tests passed.

---

## 9. Maintenance of this document

- Update this file when a new domain, integration, security rule or testing convention is introduced.
- Keep domain checklists synchronized with actual business requirements and implementation.
- When a checklist item conflicts with an approved specification, the approved specification takes precedence; document the exception.
- Add regression tests for every fixed bug where practical.
- Prefer adding a reusable test helper when the same setup/assertion is repeated across multiple domains.
- Keep this document technology-aware but avoid hard-coding assumptions that may become stale.


## Domain-specific specifications

Before implementing tests for a domain, read its corresponding file under `.ai/test-specs/`. These files specialize this general policy; they do not override approved business requirements. Update the matching specification when test coverage or domain behavior changes.
