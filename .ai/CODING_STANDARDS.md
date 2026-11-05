# Coding Standards
## Quy chuẩn Java/Spring Boot

## General
- Follow conventions already established in the affected module and neighboring classes.
- Prefer readable, cohesive code and meaningful domain names.
- Keep methods focused; avoid unnecessary nesting and speculative abstractions.
- Search for existing functionality before duplicating it.
- Remove dead code and unused imports introduced by the change.
- Avoid unrelated formatting or refactoring.

## Java and Spring
- Use the Java and dependency versions declared by the project.
- Prefer constructor injection; avoid field injection.
- Keep controllers focused on HTTP mapping and delegation.
- Put business rules in the appropriate service/domain layer, not controllers or repositories.
- Keep persistence concerns in repositories and mappings.
- Use request/response DTOs at API boundaries; do not expose entities without an explicit existing contract.
- Validate requests at boundaries and enforce important invariants in the service/domain layer.
- Use the project's common exception handling; do not swallow exceptions.
- Define transaction boundaries for multi-step state changes.
- Use BigDecimal for money and precise decimal calculations.
- Use consistent time types and make timezone assumptions explicit.
- Externalize configuration; never hard-code secrets or environment-specific endpoints.
- Never log passwords, tokens or sensitive personal/financial data.

## Maintainability
- Keep responsibilities clear and classes cohesive.
- Document non-obvious decisions and integration contracts.
- Add regression tests for bug fixes.
- Extract shared code only when behavior is genuinely shared.
