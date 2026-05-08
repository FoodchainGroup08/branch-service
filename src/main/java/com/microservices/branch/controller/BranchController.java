package com.microservices.branch.controller;

import com.microservices.branch.dto.BranchDtos;
import com.microservices.branch.service.BranchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/branches")
public class BranchController {

    @Autowired
    private BranchService branchService;

    // ── GET /branches/nearby ──────────────────────────────────────────────────

    @GetMapping("/nearby")
    public ResponseEntity<List<BranchDtos.NearbyBranchResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        return ResponseEntity.ok(branchService.findNearby(lat, lng, radiusKm));
    }

    // ── POST /branches ────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<BranchDtos.BranchResponse> createBranch(
            @RequestBody BranchDtos.CreateBranchRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("POST /branches userId={} role={}", userId, userRole);
        assertAdmin(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(request));
    }

    // ── GET /branches ─────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<BranchDtos.BranchSummary>> listBranches(
            @RequestParam(required = false)          Boolean active,
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "20")       int size) {
        return ResponseEntity.ok(branchService.listBranches(active,
                PageRequest.of(page, size, Sort.by("name").ascending())));
    }

    // ── GET /branches/{id} ────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<BranchDtos.BranchResponse> getBranch(@PathVariable String id) {
        return ResponseEntity.ok(branchService.getBranch(id));
    }

    // ── PUT /branches/{id} ───────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<BranchDtos.BranchResponse> updateBranch(
            @PathVariable String id,
            @RequestBody  BranchDtos.UpdateBranchRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("PUT /branches/{} userId={} role={}", id, userId, userRole);
        return ResponseEntity.ok(branchService.updateBranch(id, request, userId, userRole));
    }

    // ── PATCH /branches/{id}/activate ────────────────────────────────────────

    @PatchMapping("/{id}/activate")
    public ResponseEntity<BranchDtos.BranchResponse> activate(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String userRole) {
        assertAdmin(userRole);
        return ResponseEntity.ok(branchService.setActive(id, true));
    }

    // ── PATCH /branches/{id}/deactivate ──────────────────────────────────────

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BranchDtos.BranchResponse> deactivate(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String userRole) {
        assertAdmin(userRole);
        return ResponseEntity.ok(branchService.setActive(id, false));
    }

    // ── GET /branches/{id}/hours ──────────────────────────────────────────────

    @GetMapping("/{id}/hours")
    public ResponseEntity<List<BranchDtos.BranchHoursResponse>> getHours(@PathVariable String id) {
        return ResponseEntity.ok(branchService.getHours(id));
    }

    // ── PUT /branches/{id}/hours ──────────────────────────────────────────────

    @PutMapping("/{id}/hours")
    public ResponseEntity<List<BranchDtos.BranchHoursResponse>> setHours(
            @PathVariable String id,
            @RequestBody  List<BranchDtos.BranchHoursRequest> hours,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("PUT /branches/{}/hours userId={} role={}", id, userId, userRole);
        return ResponseEntity.ok(branchService.setHours(id, hours, userId, userRole));
    }

    // ── DELETE /branches/{id} ─────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable String id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("DELETE /branches/{} userId={} role={}", id, userId, userRole);
        assertAdmin(userRole);
        branchService.deleteBranch(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void assertAdmin(String userRole) {
        if (!"OFFICE_ADMIN".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OFFICE_ADMIN can perform this action");
        }
    }
}
