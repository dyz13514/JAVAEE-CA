# Project workflow

For every task in this project:

1. Before making changes, check the working tree and synchronize with the GitHub remote:
   - Run `git status --short --branch`.
   - Run `git fetch origin`.
   - If the current branch has an upstream and the working tree can be updated safely, run `git pull --ff-only`.
   - If local changes or diverged history make updating unsafe, do not overwrite anything; report the situation before proceeding.
2. Preserve unrelated user changes and never use destructive Git commands to force synchronization.
3. After completing and verifying the requested work, review the diff, commit the task's changes with a clear commit message, and push the current branch to GitHub promptly.
4. If fetching, pulling, committing, or pushing is blocked by authentication, conflicts, permissions, or failing verification, clearly report the blocker instead of claiming the repository is up to date.

Canonical remote: `https://github.com/dyz13514/JAVAEE-CA.git`
