package com.microservices.branch.repository;

import com.microservices.branch.entity.BranchTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchTableRepository extends JpaRepository<BranchTable, String> {

    List<BranchTable> findByBranchIdOrderByTableNumberAsc(String branchId);

    Optional<BranchTable> findByBranchIdAndTableNumber(String branchId, Integer tableNumber);
}
