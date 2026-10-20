package com.northstar.crm.repo;

import com.northstar.crm.domain.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
  Optional<CustomerEntity> findByPublicId(String publicId);

  boolean existsByPublicId(String publicId);

  List<CustomerEntity> findAllByOrderByIdAsc();
}
