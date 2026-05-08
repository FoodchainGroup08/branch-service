package com.microservices.branch.service;

import com.microservices.branch.dto.BranchDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BranchService {
    BranchDtos.BranchResponse createBranch(BranchDtos.CreateBranchRequest request);
    Page<BranchDtos.BranchSummary> listBranches(Boolean activeOnly, Pageable pageable);
    BranchDtos.BranchResponse getBranch(String id);
    BranchDtos.BranchResponse updateBranch(String id, BranchDtos.UpdateBranchRequest request, String requesterId, String requesterRole);
    BranchDtos.BranchResponse setActive(String id, boolean active);
    List<BranchDtos.BranchHoursResponse> getHours(String id);
    List<BranchDtos.BranchHoursResponse> setHours(String id, List<BranchDtos.BranchHoursRequest> hoursRequests, String requesterId, String requesterRole);
    void deleteBranch(String id, String requesterId, String requesterRole);
    List<BranchDtos.NearbyBranchResponse> findNearby(double lat, double lng, double radiusKm);
}
