# CI Tests Lint Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add JUnit 5 unit tests, Checkstyle/SpotBugs, `ci.yml`, and `release.yml` for the TeamCity Anka plugin.

**Architecture:** Parent POM owns shared test/lint plugin config; server module holds unit tests; GitHub Actions runs Maven 4 + Java 17 offline (no live TeamCity).

**Tech Stack:** JUnit 5, Maven Surefire, Checkstyle, SpotBugs, GitHub Actions

## Global Constraints

- Java 17; TeamCity API version from parent `teamcity-version`
- Maven 4.0.0-rc-5 in CI (match Dockerfile)
- Release commits to `master` and tags `vMAJOR.MINOR.PATCH`
- No WireMock / live TC in v1

---

## Task 1: Maven test + lint plugins

- [ ] Add JUnit 5, Surefire, Checkstyle, SpotBugs to parent `pom.xml`
- [ ] Add `config/checkstyle.xml` and `config/spotbugs-exclude.xml`
- [ ] Ensure `server` inherits and can run `mvn test` / lint goals

## Task 2: Unit tests

- [ ] `RoundRobinTest` / `WeighedURL` coverage
- [ ] `AnkaVmInstanceTest` JSON + state helpers
- [ ] `AnkaCloudPropertiesProcesserTest` validation
- [ ] Run `mvn test` and fix until green

## Task 3: GitHub Actions

- [ ] `.github/workflows/ci.yml` — parallel test/checkstyle/spotbugs
- [ ] `.github/workflows/release.yml` — dispatch bump/commit/tag/package/release

## Task 4: Docs

- [ ] Update `RELEASING.md` and `README.md`
