package com.microservices.branch.repository;

import com.microservices.branch.entity.BranchHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchHoursRepository extends JpaRepository<BranchHours, String> {
    List<BranchHours> findByBranch_IdOrderByDayOfWeekAsc(String branchId);
    void deleteByBranch_Id(String branchId);
}
