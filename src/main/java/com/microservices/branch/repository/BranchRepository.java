package com.microservices.branch.repository;

import com.microservices.branch.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, String> {
    Page<Branch> findByActive(boolean active, Pageable pageable);
    Optional<Branch> findByManagerId(String managerId);
    List<Branch> findByActiveTrueAndLatitudeNotNullAndLongitudeNotNull();
}
