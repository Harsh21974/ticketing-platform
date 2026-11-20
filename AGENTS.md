# Project: Ticketing Platform

## Stack
Java 21, Spring Boot 4.x (Spring Framework 7), Maven, PostgreSQL 16, Flyway, Testcontainers, Docker.

## Rules
- Do not assume Spring Boot 3 APIs. Spring Boot 4 changed some packages and starters. If unsure, say so instead of guessing.
- Modular monolith. No microservices.
- Layers: controller, service, repository. No business logic in controllers.
- Use DTOs (Java records) for API input and output. Never expose JPA entities.
- All schema changes go through Flyway migrations. Never edit a migration that was already merged. Never use ddl-auto=update.
- Money is stored as integer paise. Never float or double.
- No secrets in code. Read config from environment variables.
- Every new feature needs at least one test.
- Make small changes. Do not touch files unrelated to the task.
- After finishing a task, explain what you changed and why.

## Lessons learned
(Add one line here every time the AI makes a mistake.)
