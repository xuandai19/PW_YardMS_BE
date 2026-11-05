# Backend test specification

## Scope

For every changed domain (user, partner, contract, service, vehicle, yard, goods, tracking, confirmation, finance), add tests beside the affected package under `yms-service/src/test/java`.

## Required cases

- Service/unit: successful rule, rejected rule, and meaningful side effect; use JUnit 5 and Mockito.
- HTTP/API: valid response, invalid/missing/wrong-type request, not found/conflict where implemented, and the existing `ApiResponse`/error envelope.
- Security: anonymous, insufficient authority, and authorized access for protected endpoints.
- Persistence/integration: constraints, rollback, and PostgreSQL-specific behaviour when the changed code depends on it. Testcontainers tests may be skipped only when Docker is unavailable.
- Integrations: mock mail, storage, MQTT, Kafka and external finance providers. Do not use real services.

## Coverage

`mvn -Pcoverage -pl yms-service -am verify` generates the JaCoCo HTML report at `yms-service/target/site/jacoco/index.html`. No percentage gate is configured yet: establish a reviewed baseline before introducing a gate, so existing code is not silently excluded or weakened.
