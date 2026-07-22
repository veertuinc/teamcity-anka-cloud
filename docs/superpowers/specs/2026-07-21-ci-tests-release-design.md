# CI, Unit Tests, Lint, and Release — Design

**Date:** 2026-07-21  
**Status:** Approved

## Goal

Add offline unit tests and GitHub Actions for continuous testing/linting, plus a separate manual release workflow that bumps version, tags, and publishes a GitHub Release with the plugin zip — without requiring a live TeamCity server for CI.

## Decisions

| Topic | Choice |
|-------|--------|
| Release trigger | `workflow_dispatch` with `version` input |
| Version bump in release | Commit bump to `master`, tag `vX.Y.Z`, build, publish Release |
| Lint | Checkstyle **and** SpotBugs (minimal configs) |
| CI layout | One `ci.yml` (parallel test/checkstyle/spotbugs) + `release.yml` |
| Default branch | `master` |
| Initial tests | JUnit 5 on `RoundRobin`/`WeighedURL`, `AnkaVmInstance`, `AnkaCloudPropertiesProcesser` |
| Out of scope (v1) | WireMock Anka API tests, live TeamCity E2E |

## CI workflow (`ci.yml`)

- Triggers: pull requests and pushes to `master`
- Java 17, Maven 4.0.0-rc-5 (matches local/Jenkins Docker image)
- Parallel jobs: `test` (`mvn test`), `checkstyle` (`mvn checkstyle:check`), `spotbugs` (`mvn spotbugs:check`)

## Release workflow (`release.yml`)

1. Checkout `master` with write permissions
2. Validate version `MAJOR.MINOR.PATCH`
3. Run `./scripts/bump-version.sh <version>`
4. Commit and push to `master`
5. Create annotated tag `v<version>` (fail if exists)
6. `mvn package`
7. Create GitHub Release attaching `target/anka-build-cloud-teamcity-plugin-<version>.zip`

## Maven / tooling

- Parent POM: Surefire, Checkstyle plugin, SpotBugs plugin; JUnit 5 test dependency available to modules
- `config/checkstyle.xml` — minimal ruleset + suppressions as needed for green CI
- `config/spotbugs-exclude.xml` — exclude known noise; fail on remaining bugs

## Unit tests

Located in `server/src/test/java`. No live TeamCity or Anka required (TeamCity API jars on test classpath only for `AnkaCloudPropertiesProcesser`).

## Docs

- Update `RELEASING.md` for Actions-based release (keep local bump script docs)
- Brief CI/test notes in `README.md`
