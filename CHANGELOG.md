# Changelog

## 1.0.0 - 2026-11-16

- Spring Boot 3 / Java 21 order service on PostgreSQL (Flyway, JPA, RFC 7807 errors, Actuator).
- Ten production incidents, each with the shipped code, the fix, a test that reproduces the bug and proves the fix, and a write-up.
- Debugging playbook: stack traces, thread dumps, heap dumps, SQL statistics, remote debugging.
- Docker image with heap dump on OOM and GC logging; docker compose stack with JDWP on port 5005.
- CI: tests against PostgreSQL in Testcontainers, then image build and smoke test.
