package com.omniticket.reservation_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractBaseIntegrationTest {

    static {
        TestContainerSingleton.getPostgres();
        TestContainerSingleton.getRedis();
        TestContainerSingleton.getRabbitmq();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://" +
                        TestContainerSingleton.getPostgres().getHost() +
                        ":" +
                        TestContainerSingleton.getPostgres().getFirstMappedPort() +
                        "/" +
                        TestContainerSingleton.getPostgres().getDatabaseName());
        registry.add(
                "spring.datasource.username",
                TestContainerSingleton.getPostgres()::getUsername);
        registry.add(
                "spring.datasource.password",
                TestContainerSingleton.getPostgres()::getPassword);

        registry.add(
                "spring.data.redis.host",
                TestContainerSingleton.getRedis()::getHost);
        registry.add(
                "spring.data.redis.port",
                TestContainerSingleton.getRedis()::getFirstMappedPort);

        registry.add(
                "spring.rabbitmq.host",
                TestContainerSingleton.getRabbitmq()::getHost);
        registry.add(
                "spring.rabbitmq.port",
                TestContainerSingleton.getRabbitmq()::getAmqpPort);
        registry.add(
                "spring.rabbitmq.username",
                TestContainerSingleton.getRabbitmq()::getAdminUsername);
        registry.add(
                "spring.rabbitmq.password",
                TestContainerSingleton.getRabbitmq()::getAdminPassword);
    }
}
