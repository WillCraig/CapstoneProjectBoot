package com.northstar.crm.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared real infrastructure for the Lab 49 ITs: one PostgreSQL 16 and one Kafka container
 * for the whole test JVM (singleton pattern — containers start once, Ryuk removes them at
 * exit). Tests therefore prove the Flyway migration, JPA mappings, and Kafka publishing
 * without depending on whatever happens to be running on local ports 5432/9092.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("crm")
          .withUsername("crm")
          .withPassword("crm-test");

  protected static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

  static {
    POSTGRES.start();
    KAFKA.start();
  }

  @DynamicPropertySource
  static void infrastructureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
  }

  protected static String kafkaBootstrapServers() {
    return KAFKA.getBootstrapServers();
  }
}
