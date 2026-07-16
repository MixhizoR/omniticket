# OmniTicket

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-darkblue)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.4-red)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4.1-orange)](https://www.rabbitmq.com/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-1.21-blueviolet)](https://testcontainers.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

OmniTicket is a high-concurrency monolithic backend application built with Spring Boot 3.5.x and Java 21. It is designed for events with massive traffic spikes -- concerts, festivals, sporting events -- where thousands of users compete for limited tickets simultaneously.

The system uses Redisson distributed locks to prevent overselling, an outbox pattern with RabbitMQ for reliable asynchronous messaging, JPA optimistic locking for data integrity, and Testcontainers for fully isolated integration testing.

---

## Key Features

- **Distributed Locking with Redisson.** Redis-backed locks prevent race conditions during concurrent reservation and purchase operations. Lock keys are scoped per ticket, so unrelated reservations never contend on the same lock.

- **Outbox Pattern for Reliable Messaging.** Domain events are persisted to an outbox table within the same database transaction that updates the ticket. A scheduled poller reads pending events and publishes them to RabbitMQ, guaranteeing at-least-once delivery without two-phase commits.

- **Asynchronous Email Notifications.** After purchase, a confirmation email is sent via RabbitMQ asynchronously. The API returns immediately to the client while the email service processes the message in the background.

- **Three Layers of Concurrency Control.** Redisson locks at the application layer, JPA `@Version` optimistic locking at the ORM layer, and database constraints at the persistence layer. This defence-in-depth approach ensures overselling is impossible even under extreme load.

- **Fully Containerized Environment.** Docker Compose orchestrates PostgreSQL, Redis, RabbitMQ, and the application. A single command provisions the entire stack with health checks and network configuration.

- **Isolated Integration Testing with Testcontainers.** Integration tests spin up disposable PostgreSQL and RabbitMQ containers, eliminating the need for pre-configured infrastructure. Tests are deterministic and self-contained.

- **Interactive API Documentation via Scalar.** The API is documented with OpenAPI 3.0 and served through Scalar UI for exploring and testing endpoints.

- **Observability with Actuator and Prometheus.** Spring Boot Actuator exposes health, metrics, and environment endpoints. Micrometer publishes custom metrics in Prometheus format for real-time monitoring.

---

## Technology Stack

| Component              | Technology                                   |
|------------------------|----------------------------------------------|
| Framework              | Spring Boot 3.5.16                           |
| Language               | Java 21                                      |
| Database               | PostgreSQL 16                                |
| Caching and Locking    | Redis 7.4 with Redisson 3.52.0               |
| Message Broker         | RabbitMQ 4.1                                 |
| API Documentation      | SpringDoc OpenAPI 2.8.15 + Scalar            |
| Containerisation       | Docker and Docker Compose                    |
| Monitoring             | Spring Boot Actuator + Micrometer Prometheus |
| Build Tool             | Apache Maven (Maven Wrapper)                 |
| Testing                | JUnit 5, Testcontainers 1.21, Awaitility     |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose (V2 recommended)
- JDK 21 and Maven 3.9+ (only for local development without Docker)

### Quick Start with Docker Compose

```bash
git clone https://github.com/MixhizoR/omniticket.git
cd omniticket

cp .env.example .env

docker compose up -d --build
```

This starts four services:

| Service     | Port(s)                         | Default Credentials                         |
|-------------|---------------------------------|---------------------------------------------|
| PostgreSQL  | 5432                            | `omniticket` / `myuser` / `secret`          |
| Redis       | 6379                            | None                                        |
| RabbitMQ    | 5672 (AMQP), 15672 (Management) | `admin` / `password`                        |
| OmniTicket  | 8080                            | None                                        |

### Local Development Without Docker

Start PostgreSQL, Redis, and RabbitMQ separately (or via Docker), then update `.env` to point `DB_HOST`, `REDIS_HOST`, and `RABBITMQ_HOST` to `localhost`.

```bash
./mvnw spring-boot:run
```

The `.env` file is automatically loaded as a property source, so no manual property overrides are needed.

---

## API Documentation

When the application is running, visit:

```website
http://localhost:8080/scalar/index.html
```

### Available Endpoints

| Method   | Path                          | Description                  |
|----------|-------------------------------|------------------------------|
| `GET`    | `/api/tickets`                | List all tickets             |
| `GET`    | `/api/tickets/{id}`           | Get a ticket by ID           |
| `POST`   | `/api/tickets`                | Create a new ticket          |
| `PUT`    | `/api/tickets/{id}`           | Update a ticket              |
| `DELETE` | `/api/tickets/{id}`           | Delete a ticket              |
| `POST`   | `/api/tickets/{id}/reserve`   | Reserve an available ticket  |
| `POST`   | `/api/tickets/{id}/purchase`  | Purchase a reserved ticket   |

---

## Configuration

Environment-specific configuration is externalised through the `.env` file.

| Variable           | Default     | Description                                          |
|--------------------|-------------|------------------------------------------------------|
| `DB_HOST`          | `postgres`  | PostgreSQL host                                      |
| `POSTGRES_DB`      | `omniticket`| Database name                                        |
| `POSTGRES_USER`    | `myuser`    | Database user                                        |
| `POSTGRES_PASSWORD`| `secret`    | Database password                                    |
| `RABBITMQ_USER`    | `admin`     | RabbitMQ management user                             |
| `RABBITMQ_PASSWORD`| `password`  | RabbitMQ management password                         |
| `MAIL_USER`        |             | SMTP username (Mailtrap or any SMTP provider)        |
| `MAIL_PASS`        |             | SMTP password                                        |

---

## Testing

Tests use Testcontainers to provision real PostgreSQL and RabbitMQ instances. Docker must be running.

```bash
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TicketServiceConcurrencyTest
```

The test suite covers unit tests with mocked dependencies, integration tests against real containers, and concurrency tests that simulate thousands of concurrent reservation attempts to validate distributed lock behaviour.

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.
