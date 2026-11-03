# Git Workflow

## Before changes
- Check the current branch and working tree.
- Do not overwrite or discard existing user changes.
- Keep commits focused on one logical change.
- Exclude build output, IDE files, secrets, local environment files and generated data unless explicitly required.

## Branches and commits
- Follow branch naming and commit conventions already used in the repository.
- Use short, imperative commit messages.
- Conventional prefixes may include feat, fix, test, refactor, docs and chore.
- Do not force-push or rewrite shared history unless explicitly requested.
- Respect branch protection and pull-request requirements.

## Before commit / pull request
- Review git status and the complete diff.
- Run relevant tests and report exact results.
- Check that no unrelated files or secrets are included.
- Summarize purpose, implementation, tests and migration/configuration impact.
- Do not claim a change is ready to merge if required checks were not run.
