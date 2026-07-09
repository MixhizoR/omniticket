package com.omniticket.reservation_service;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * True JVM-singleton for Testcontainers containers.
 * Ensures containers start ONLY ONCE per JVM and survive
 * across different Spring ApplicationContexts.
 */
@SuppressWarnings("resource")
public final class TestContainerSingleton {

    private static final PostgreSQLContainer<?> postgres;
    private static final GenericContainer<?> redis;
    private static final RabbitMQContainer rabbitmq;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();

        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();

        rabbitmq = new RabbitMQContainer("rabbitmq:4-alpine");
        rabbitmq.start();
    }

    public static PostgreSQLContainer<?> getPostgres() {
        return postgres;
    }

    public static GenericContainer<?> getRedis() {
        return redis;
    }

    public static RabbitMQContainer getRabbitmq() {
        return rabbitmq;
    }

    private TestContainerSingleton() {
    }
}