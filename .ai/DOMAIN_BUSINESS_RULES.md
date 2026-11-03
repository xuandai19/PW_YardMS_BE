# Domain Business Rules

This document records verified, approved business rules. It is not permission to infer requirements.

## Instructions for AI
- Do not infer rules from class names, field names or UI labels alone.
- Inspect services, entities, DTOs, repositories, migrations, API contracts and tests.
- Prefer approved requirements and acceptance criteria over assumptions based on implementation.
- If the specification and implementation conflict, report the discrepancy.
- Record a source reference for every non-obvious rule.
- Do not transfer rules between domains unless explicitly required.

## Domain inventory
- User
- Confirmation
- Partner
- Contract
- Service
- Vehicle
- Yard
- Goods
- Tracking
- Finance

## Rule documentation template

### [Domain]

| ID | Rule | Preconditions | Expected behavior | Source/reference |
|---|---|---|---|---|
| DOM-XXX-001 | Approved rule | Required state/input | Observable result | Ticket/spec/code |

### State transitions

| Current state | Action | Allowed? | Next state | Source |
|---|---|---|---|---|

### Access rules

| Role/authority | Resource scope | Allowed actions | Source |
|---|---|---|---|

### Invariants
- Verified domain invariants:
- Database constraints enforcing them:
- Service-level validation enforcing them:
- Regression tests:

No business rules are prefilled because they must be confirmed against approved requirements.
