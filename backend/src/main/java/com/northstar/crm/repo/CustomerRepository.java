package com.northstar.crm.repo;

import com.northstar.crm.domain.CustomerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
  Optional<CustomerEntity> findByPublicId(String publicId);

  List<CustomerEntity> findAllByOrderByFullNameAsc();

  /** Agent search: one free-text term matched against both the public ID and the full name. */
  List<CustomerEntity>
      findByPublicIdContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrderByFullNameAsc(
          String publicIdTerm, String fullNameTerm);
}
