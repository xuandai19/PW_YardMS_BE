# Architecture Guidelines

## Baseline
The backend is organized around business domains. Preserve existing domain boundaries and package conventions in yms-service, with genuinely shared code in common.

Observed domains: user, confirmation, partner, contract, service, vehicle, yard, goods, tracking and finance.

## Responsibilities
- Keep HTTP/controller concerns at the API boundary.
- Keep use-case orchestration and business rules in services/domain components.
- Keep persistence access behind repositories.
- Keep external systems behind explicit clients/adapters.
- Do not use common as a dumping ground for domain-specific classes.
- Avoid circular dependencies between domains.
- Do not reach into another domain's repositories to bypass its service/application boundary.
- Use explicit DTOs or domain events for cross-domain communication where appropriate.
- Make transaction ownership and failure behavior explicit for cross-domain workflows.

## Change rules
1. Identify the owning domain before adding a class.
2. Search for existing services, DTOs, mappers and repositories.
3. Place new code alongside the closest existing feature.
4. Keep changes within the affected domain unless a documented dependency requires otherwise.
5. Do not restructure packages or introduce a new pattern as part of an unrelated feature.
6. Update tests and documentation when a boundary changes.

Before moving code into shared modules, verify that multiple independent domains need the behavior and that sharing will not create tighter coupling.
