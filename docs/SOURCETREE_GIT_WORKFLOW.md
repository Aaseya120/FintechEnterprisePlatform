# Senior Developer Git & SourceTree Workflow Guide

## 1. Atlassian SourceTree Setup

As a Senior Java Developer, standardizing team Git workflows ensures zero regression and seamless Jira / Confluence traceability. This project is configured for **GitFlow** and **Trunk-Based / GitHub Flow** branching strategies via **Atlassian SourceTree**.

### 1.1 SourceTree GitFlow Initialization
1. In SourceTree, click **GitFlow** icon in the toolbar.
2. Accept standard enterprise branch prefixes:
   - **Production Branch**: `main`
   - **Development Branch**: `develop`
   - **Feature Branch Prefix**: `feature/`
   - **Release Branch Prefix**: `release/`
   - **Hotfix Branch Prefix**: `hotfix/`
   - **Version Tag Prefix**: `v`

---

## 2. Commit Message Convention (Jira & Sonar Integration)

Every commit must reference its active Jira User Story ID to enable automated release notes generation in Jira & Confluence:

```
[<JIRA-KEY>] <type>(<scope>): <concise subject in imperative mood>

[optional body explaining 'why' and architectural rationale]

[optional footer with breaking changes or co-authors]
```

### Examples:
- `[FIN-101] feat(order): implement transactional outbox entity and JPA repository`
- `[FIN-104] fix(gateway): correct redis reactive rate limiter key resolution for IPv6`
- `[FIN-108] perf(grpc): configure KeepAlive and thread pool tuning for payment service`

---

## 3. Pull Request Review Matrix & Definition of Done (DoD)

Before any PR can be merged to `develop` or `main`:
1. **At Least 2 Approvals**: 1 Senior Developer + 1 Security/Architecture Lead.
2. **SonarQube Quality Gate**: Must be `PASSED` (0 Blocker, 0 Critical, >80% line coverage via JaCoCo).
3. **CI/CD Pipeline**: GitHub Actions / GitLab CI must complete all stages green.
4. **Interactive Rebase & Squash**: Clean up messy WIP commits before final merge.
