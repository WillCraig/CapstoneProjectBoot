package com.northstar.crm.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.northstar.crm.domain.CustomerEntity;
import com.northstar.crm.domain.InteractionEntity;
import com.northstar.crm.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * JPA proof against real PostgreSQL: Flyway V1 seeded the fixture customers, and the
 * timeline query returns newest first. Runs against the shared Testcontainers database.
 */
@SpringBootTest
class InteractionPersistenceIT extends AbstractIntegrationTest {

  @Autowired CustomerRepository customerRepository;

  @Autowired InteractionRepository interactionRepository;

  @BeforeEach
  void cleanInteractions() {
    interactionRepository.deleteAll();
  }

  @Test
  void flywayV1_seedsFixtureCustomers() {
    CustomerEntity amina = customerRepository.findByPublicId("CUS-1001").orElseThrow();
    assertEquals("Amina Khan", amina.getFullName());
    assertEquals("ACTIVE", amina.getStatus());

    CustomerEntity ravi = customerRepository.findByPublicId("CUS-1002").orElseThrow();
    assertEquals("Ravi Singh", ravi.getFullName());
    assertEquals("PROSPECT", ravi.getStatus());

    assertTrue(customerRepository.findByPublicId("CUS-9999").isEmpty());
  }

  @Test
  void saveAndTimelineQuery_newestFirstPerCustomer() {
    CustomerEntity amina = customerRepository.findByPublicId("CUS-1001").orElseThrow();
    CustomerEntity ravi = customerRepository.findByPublicId("CUS-1002").orElseThrow();

    InteractionEntity older = save(amina, "NOTE", Instant.parse("2026-10-19T10:00:00Z"));
    InteractionEntity newer = save(amina, "EMAIL", Instant.parse("2026-10-20T10:00:00Z"));
    save(ravi, "CALL", Instant.parse("2026-10-20T11:00:00Z"));

    List<InteractionEntity> timeline =
        interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc("CUS-1001");

    // Ravi's later interaction must not leak into Amina's timeline.
    assertEquals(List.of(newer.getId(), older.getId()), timeline.stream().map(InteractionEntity::getId).toList());
    assertEquals("lab-request-001", timeline.get(0).getCorrelationId());
  }

  @Test
  void softDeletedInteraction_hiddenFromTimelineAndFindById() {
    CustomerEntity amina = customerRepository.findByPublicId("CUS-1001").orElseThrow();
    InteractionEntity visible = save(amina, "NOTE", Instant.parse("2026-10-20T10:00:00Z"));
    InteractionEntity deleted = save(amina, "EMAIL", Instant.parse("2026-10-20T11:00:00Z"));

    deleted.setDeletedAt(Instant.parse("2026-10-20T12:00:00Z"));
    interactionRepository.save(deleted);

    assertFalse(interactionRepository.findById(deleted.getId()).isPresent());
    assertEquals(
        List.of(visible.getId()),
        interactionRepository.findByCustomer_PublicIdOrderByCreatedAtDesc("CUS-1001").stream()
            .map(InteractionEntity::getId)
            .toList());
  }

  private InteractionEntity save(CustomerEntity customer, String type, Instant createdAt) {
    InteractionEntity entity = new InteractionEntity();
    entity.setId(UUID.randomUUID());
    entity.setCustomer(customer);
    entity.setInteractionType(type);
    entity.setSummary("Persistence IT " + type);
    entity.setCorrelationId("lab-request-001");
    entity.setCreatedAt(createdAt);
    return interactionRepository.save(entity);
  }
}
