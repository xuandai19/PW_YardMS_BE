# API Design Guidelines
## 	Quy chuẩn thiết kế REST API

- Follow existing REST conventions, route naming, response envelopes and exception formats.
- Inspect neighboring controllers and API tests before changing an endpoint.
- Use resource-oriented paths and appropriate HTTP methods while preserving repository conventions.
- Use request/response DTOs and validate incoming data at the boundary.
- Return appropriate status codes for success, validation failure, unauthenticated, forbidden, not found and conflict cases.
- Keep errors consistent; never expose stack traces, SQL, internal class names or secrets.
- Define pagination, filtering and sorting behavior; validate page size and sort properties.
- Do not silently change request/response fields. Assess compatibility and update consumers/tests for approved contract changes.
- Enforce authorization on the server for every protected operation.
- Do not expose sensitive fields or persistence internals.
- Define duplicate-request/idempotency behavior for operations where it matters.
- API examples must match actual DTOs and implementation; never invent fields or response codes.
