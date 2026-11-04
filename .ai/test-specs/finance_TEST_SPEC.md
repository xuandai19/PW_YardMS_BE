# Finance Domain — Test Specification

> Status: living test specification. Confirm every scenario against approved requirements and current implementation before asserting expected outcomes.

## 1. Scope
Invoices, calculations, payment/settlement and financial reporting.

## 2. Mandatory inspection before writing tests
- [ ] Read the domain controller/API, request and response DTOs.
- [ ] Read service/use-case classes and domain validation.
- [ ] Read entities, repositories, mappings and relevant Flyway migrations.
- [ ] Read security rules and determine role/resource scope.
- [ ] Read existing tests and reuse project fixtures/utilities.
- [ ] Identify integrations, events, scheduled jobs and side effects.
- [ ] Record unclear or conflicting requirements; do not guess.

## 3. Domain scenarios to evaluate
- [ ] BigDecimal arithmetic
- [ ] zero denominator
- [ ] precision/rounding/currency
- [ ] invoice uniqueness and totals
- [ ] tax/discount/fee rules from requirements
- [ ] payment state transitions
- [ ] duplicate posting/double charge
- [ ] overdue calculations with fixed clock
- [ ] transaction rollback
- [ ] export/download
- [ ] aggregation
- [ ] external integration failures.

## 4. Test layers
- [ ] Unit tests for business rules, validators, calculations and service branches.
- [ ] Repository tests for queries, constraints, filtering, sorting and pagination.
- [ ] API tests for request validation, response contract and exception mapping.
- [ ] Security tests for anonymous, insufficient-role and allowed access.
- [ ] Integration tests for persistence, transactions and cross-domain effects.
- [ ] Async/integration adapter tests for messaging, storage or scheduled work when applicable.

Only add a layer when the behavior exists in this domain. Prefer PostgreSQL/Testcontainers for PostgreSQL-specific behavior.

## 5. Input and boundary coverage
For each constrained field, choose applicable cases:
- [ ] Missing, null, empty and whitespace-only values.
- [ ] Minimum, maximum, just-below and just-above boundaries.
- [ ] Wrong type, invalid format and unsupported enum/state.
- [ ] Duplicate identifiers or replayed requests where relevant.
- [ ] Leading/trailing whitespace and Unicode/special characters where accepted.
- [ ] Decimal, negative and zero values according to the field's actual semantics.

## 6. Persistence and consistency
- [ ] Required database constraints are enforced.
- [ ] Invalid references and orphan records are rejected/prevented.
- [ ] Multi-step operations roll back on failure.
- [ ] Delete/cascade behavior matches approved retention rules.
- [ ] Concurrent updates do not violate domain invariants.
- [ ] List queries return correct filtering, ordering and pagination.

## 7. Security and privacy
- [ ] Protected endpoints reject anonymous requests.
- [ ] Insufficient authorities are rejected.
- [ ] Users cannot access another user's or out-of-scope resource.
- [ ] Sensitive fields are not exposed in API responses or logs.
- [ ] Client-supplied role/owner/scope values are verified server-side.

## 8. Test data and determinism
- Use isolated fixtures; do not depend on production data.
- Use a fixed Clock for expiry, due-date and time-window tests.
- Mock external services in unit tests; use isolated containers/adapters for integration tests.
- Avoid arbitrary sleeps and order-dependent tests.

## 9. Completion checklist
- [ ] Positive and negative paths covered.
- [ ] Applicable boundary and security cases covered.
- [ ] Persistence and side effects verified.
- [ ] Regression tests added for fixed defects.
- [ ] Relevant tests executed; exact command and result recorded.
- [ ] Unexecuted tests and remaining gaps reported.

## 10. References
- General policy: [AI_TESTING_GUIDELINES.md](../AI_TESTING_GUIDELINES.md)
- Business rules: [DOMAIN_BUSINESS_RULES.md](../DOMAIN_BUSINESS_RULES.md)

> This document is a scenario checklist, not a source of business requirements. Do not invent field limits, role permissions, status transitions, formulas, currencies or HTTP status codes. Where requirements are unclear, record the gap and request clarification.
