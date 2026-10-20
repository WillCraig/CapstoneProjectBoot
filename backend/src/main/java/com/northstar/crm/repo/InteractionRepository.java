package com.northstar.crm.repo;

import com.northstar.crm.domain.InteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<InteractionEntity, UUID> {
  List<InteractionEntity> findByCustomer_PublicIdOrderByCreatedAtDesc(String publicId);
}
