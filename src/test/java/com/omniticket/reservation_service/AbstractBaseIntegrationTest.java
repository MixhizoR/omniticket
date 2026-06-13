package com.omniticket.reservation_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractBaseIntegrationTest {

    static {
        // Trigger singleton container startup BEFORE any Spring context initialization
        TestContainerSingleton.getPostgres();
        TestContainerSingleton.getRedis();
        TestContainerSingleton.getRabbitmq();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add(
            "spring.datasource.url",
            () ->
                "jdbc:postgresql://" +
                TestContainerSingleton.getPostgres().getHost() +
                ":" +
                TestContainerSingleton.getPostgres().getFirstMappedPort() +
                "/" +
                TestContainerSingleton.getPostgres().getDatabaseName()
        );
        registry.add(
            "spring.datasource.username",
            TestContainerSingleton.getPostgres()::getUsername
        );
        registry.add(
            "spring.datasource.password",
            TestContainerSingleton.getPostgres()::getPassword
        );

        // Redis
        registry.add(
            "spring.data.redis.host",
            TestContainerSingleton.getRedis()::getHost
        );
        registry.add(
            "spring.data.redis.port",
            TestContainerSingleton.getRedis()::getFirstMappedPort
        );

        // RabbitMQ
        registry.add(
            "spring.rabbitmq.host",
            TestContainerSingleton.getRabbitmq()::getHost
        );
        registry.add(
            "spring.rabbitmq.port",
            TestContainerSingleton.getRabbitmq()::getAmqpPort
        );
        registry.add(
            "spring.rabbitmq.username",
            TestContainerSingleton.getRabbitmq()::getAdminUsername
        );
        registry.add(
            "spring.rabbitmq.password",
            TestContainerSingleton.getRabbitmq()::getAdminPassword
        );
    }
}
