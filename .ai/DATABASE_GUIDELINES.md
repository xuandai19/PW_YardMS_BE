# Database Guidelines
## Quy chuẩn Entity, Repository, Migration, Transaction

- Inspect entities, repositories, Flyway migrations and database conventions before persistence changes.
- Follow the configured database and declared dependency versions.
- Treat applied Flyway migrations as immutable; add a new migration for schema changes unless an explicit migration strategy says otherwise.
- Consider nullability, uniqueness, foreign keys, indexes, column length, precision/scale and delete behavior.
- Do not add cascade delete without understanding dependent data and retention rules.
- Avoid N+1 queries and use pagination for potentially large result sets.
- Keep multi-step writes within a clear transaction boundary.
- Consider locking/versioning for concurrent updates where lost updates or over-allocation are possible.
- Never use floating-point types for money.
- Test PostgreSQL-specific behavior against PostgreSQL (prefer Testcontainers); H2 is not automatically equivalent.
- Verify migrations against a clean database and the supported upgrade path.
- Never commit production data, credentials or sensitive database dumps.
