# Security Guidelines

- Inspect the actual Spring Security configuration, JWT implementation, filters and authorization rules before changes.
- Enforce authentication and authorization on the backend for every protected endpoint.
- Apply least privilege; do not broaden permissions to make a test pass.
- Never trust client-supplied user IDs, roles, tenant, partner or resource ownership without server-side verification.
- Prevent horizontal and vertical privilege escalation.
- Hash passwords with the project's approved encoder; never store or log plaintext passwords.
- Validate JWT signature, expiration and relevant claims using the existing implementation.
- Follow the implemented token refresh/revocation design; do not invent a parallel mechanism.
- Keep secrets in environment/configuration secret stores, not source files.
- Do not expose tokens, passwords, personal data or internal exception details in responses/logs.
- Validate uploaded files and protect storage paths against traversal and unauthorized access.
- Use safe query practices; never construct SQL from untrusted input.
- Add tests for anonymous access, insufficient authority, allowed access and cross-user/resource access boundaries.
- Flag security-sensitive assumptions rather than silently choosing permissive behavior.
