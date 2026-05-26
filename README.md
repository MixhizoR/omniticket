# 🎫 OmniTicket: High-Concurrency Ticket Reservation System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen)
![Java](https://img.shields.io/badge/Java-21-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-darkblue)
![Redis](https://img.shields.io/badge/Redis-7.4-red)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4.1-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

**OmniTicket** is a state-of-the-art, high-concurrency ticket reservation microservice built with **Spring Boot 3.5.x**. Designed for massive traffic events (like concerts or major sporting events), it leverages advanced locking mechanisms and messaging queues to ensure data integrity and system resilience under heavy Load.

---

## 🔥 Key Features

-   **High Concurrency Support**: Utilizes **Redisson** for distributed locking to prevent overbooking and race conditions.
-   **Layered Architecture**: Clean separation of concerns following the **Controller-Service-Repository** pattern.
-   **Modern Java**: Built using **Java 21** with full support for **Records**, **Virtual Threads** (where applicable), and **Spring Boot 3.5.x** features.
-   **Real-time Notifications**: Integrated with **RabbitMQ** for asynchronous ticket issuance and status updates.
-   **Interactive API Documentation**: Full **Scalar** integration for a modern, high-performance API testing interface.
-   **Dockerized Deployment**: Includes a multi-stage Dockerfile and a robust `compose.yaml` for instant environment setup.
-   **Global Exception Handling**: Centralized error management for meaningful HTTP status codes and responses.
-   **Observability & Monitoring**: Fully integrated with **Spring Boot Actuator** and **Micrometer Prometheus** for real-time system health and performance tracking.

---

## 🏗️ Architectural Decisions (The 'Why?')

As a Senior Architect, I've implemented several best practices into this project:

1.  **Distributed Locking (Redisson)**: To handle high-concurrency reservations, we use Redis-based locking rather than simple database locks. This prevents database bottlenecking and ensures that ticket counts are never inconsistent.
2.  **Spring Boot 3.5.x & Java 21**: We utilize the latest stable features, focusing on performance optimizations and modern coding standards (Lombok, Jakarta EE).
3.  **Asynchronous Messaging**: RabbitMQ decoupling ensures that the reservation experience remains fast for the user while slower processes (like sending confirmation emails) happen in the background.
4.  **Database Strategy**: PostgreSQL 16 is used for ACID-compliant persistence, with optimized indexing for ticket availability checks.
5.  **Observability (Actuator & Prometheus)**: Architecture includes built-in readiness/liveness probes and custom metrics to ensure the system is monitorable in production environments.

---

## 🛠️ Technology Stack

| Component            | Technology                     |
| :------------------- | :----------------------------- |
| **Framework**        | Spring Boot 3.5.x              |
| **Language**         | Java 21                        |
| **Database**         | PostgreSQL 16                  |
| **Caching/Locking**  | Redis & Redisson               |
| **Messaging Queue**  | RabbitMQ                       |
| **API Documentation**| Scalar & SpringDoc             |
| **Containerization** | Docker & Docker Compose        |
| **Monitoring**      | Actuator & Prometheus          |
| **Build Tool**       | Maven                          |

---

## 🚀 Getting Started

### Prerequisites

-   [Docker & Docker Compose](https://docs.docker.com/get-docker/)
-   [JDK 21+](https://adoptium.net/) (if running locally)
-   [Maven](https://maven.apache.org/download.cgi) (if running locally)

### Quick Start with Docker

The easiest way to get OmniTicket up and running is using Docker:

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/omniticket.git
    cd omniticket
    ```

2.  **Environment Setup**:
    The service reads from a `.env` file. A default one is provided.
    ```bash
    cp .env.example .env
    ```
    *Note: The application is configured to automatically import `.env` as a property source even during local development.*

3.  **Launch the stack**:
    ```bash
    docker compose up -d --build
    ```

The system will automatically initialize:
-   **PostgreSQL**: `localhost:5432`
-   **Redis**: `localhost:6379`
-   **RabbitMQ Management**: `localhost:15672` (User/Pass from `.env`)
-   **OmniTicket API**: `localhost:8080`

---

## 📑 API Documentation (Scalar)

Once the application is running, access the premium Scalar API documentation at:

👉 [**http://localhost:8080/scalar/index.html**](http://localhost:8080/scalar/index.html)

*Note: Ensure `scalar.enabled=true` is set in your `application.properties`.*

---

## 🔧 Configuration

All critical configurations are managed via environment variables in the `.env` file:

```properties
# Database host (postgres for Docker, localhost for local)
DB_HOST=postgres
POSTGRES_DB=omniticket
POSTGRES_USER=myuser
POSTGRES_PASSWORD=secret

# RabbitMQ
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=password

# Mail (Mailtrap etc.)
MAIL_USER=your_user
MAIL_PASS=your_pass
```

---

## 🧪 Running Tests

```bash
./mvnw test
```

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.

---

**Developed with ❤️ by Oğuz Selman Çetin.**
