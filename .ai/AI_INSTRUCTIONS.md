# AI Instructions — PW_YardMS_BE
## Quy tắc chung AI phải tuân thủ

Before changing code, read this file and the relevant documents under .ai/.

## Mandatory workflow
1. Inspect the current branch, working tree, repository structure and existing implementation.
2. Identify affected domains, APIs, database objects, integrations and tests.
3. Read applicable business, architecture, coding, security and testing rules.
4. Trace existing behavior before proposing changes.
5. Make the smallest coherent change and preserve existing conventions.
6. Add or update tests in the same change.
7. Run relevant tests and inspect the results.
8. Review the diff for unrelated changes, security issues and regressions.
9. Report changed files, behavior, test commands, results and remaining risks.

## Mandatory AI workflow and test design
- Follow `.ai/AI_CODE_GENERATION_WORKFLOW.md` for every code-generation or code-change task.
- Use `.ai/TEST_CASE_CATALOG.md` to select and extend test cases; map every changed acceptance criterion to test-case IDs.
- Follow `.ai/AI_TESTING_GUIDELINES.md` for test levels, domain coverage, security, boundaries and test execution.
- Keep production changes and their automated tests in the same change.
- Report PASS, FAIL, BLOCKED and NOT RUN separately, with executed commands and evidence.

## Rules
- Do not invent business requirements, permissions, status transitions, field limits or default values.
- If approved requirements and implementation disagree, report the discrepancy instead of guessing.
- Do not mix unrelated domains or introduce unnecessary cross-domain dependencies.
- Do not bypass validation, authorization, transactions or audit requirements.
- Do not duplicate existing functionality or add dependencies without justification.
- Never delete or weaken tests just to make a build pass.
- Never claim a test/build/migration passed unless it was executed.
- Never commit secrets, credentials, production data or generated build artifacts.
- Keep changes focused and preserve API compatibility unless a change is approved.

## Related guides
- AI workflow: AI_CODE_GENERATION_WORKFLOW.md
- Test case catalog: TEST_CASE_CATALOG.md
- Testing: AI_TESTING_GUIDELINES.md
- Business rules: DOMAIN_BUSINESS_RULES.md
- Coding: CODING_STANDARDS.md
- Architecture: ARCHITECTURE_GUIDELINES.md
- API: API_DESIGN_GUIDELINES.md
- Database: DATABASE_GUIDELINES.md
- Security: SECURITY_GUIDELINES.md
- Git: GIT_WORKFLOW.md
