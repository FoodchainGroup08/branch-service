package com.microservices.branch.controller;

import com.microservices.branch.dto.BranchDtos;
import com.microservices.branch.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/branches")
@Tag(name = "Branches", description = "Manage restaurant branch locations, operating hours, and active status. Read operations are public; write operations require OFFICE_ADMIN role.")
public class BranchController {

    @Autowired
    private BranchService branchService;

    // ── GET /branch/nearby ────────────────────────────────────────────────────

    @Operation(
        summary = "Find branches near a location",
        description = "Returns all active branches within the specified radius of the given coordinates, sorted by distance ascending. Distance is calculated using the Haversine formula.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of nearby branches with their distance from the query point")
    })
    @GetMapping("/nearby")
    public ResponseEntity<List<BranchDtos.NearbyBranchResponse>> nearby(
            @Parameter(description = "Latitude of the customer's location (decimal degrees)", required = true, example = "24.7136")
            @RequestParam double lat,
            @Parameter(description = "Longitude of the customer's location (decimal degrees)", required = true, example = "46.6753")
            @RequestParam double lng,
            @Parameter(description = "Search radius in kilometres (default 10 km)", example = "5.0")
            @Positive(message = "radiusKm must be positive")
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        return ResponseEntity.ok(branchService.findNearby(lat, lng, radiusKm));
    }

    // ── POST /branch ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Create a branch",
        description = "Registers a new restaurant branch with its name, address, phone number, and GPS coordinates. Requires OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Branch created successfully"),
        @ApiResponse(responseCode = "403", description = "Caller does not have OFFICE_ADMIN role")
    })
    @PostMapping
    public ResponseEntity<BranchDtos.BranchResponse> createBranch(
            @Valid @RequestBody BranchDtos.CreateBranchRequest request,
            @Parameter(description = "Caller's user UUID — injected by the API Gateway from the JWT. When testing directly on Swagger, paste your user UUID here.", example = "00000000-0000-0000-0000-000000000001") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("POST /branches userId={} role={}", userId, userRole);
        assertAdmin(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(request));
    }

    // ── GET /branch ───────────────────────────────────────────────────────────

    @Operation(
        summary = "List branches",
        description = "Returns a paginated list of branches sorted alphabetically by name. Optionally filter to active or inactive branches only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of branch summaries")
    })
    @GetMapping
    public ResponseEntity<Page<BranchDtos.BranchSummary>> listBranches(
            @Parameter(description = "Filter by active status — omit to return all branches")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Zero-based page number (default 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (default 20)")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(branchService.listBranches(active,
                PageRequest.of(page, size, Sort.by("name").ascending())));
    }

    // ── GET /branch/{id} ─────────────────────────────────────────────────────

    @Operation(
        summary = "Get branch by ID",
        description = "Returns full details for a single branch including address, coordinates, phone, and active status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Branch details"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BranchDtos.BranchResponse> getBranch(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(branchService.getBranch(id));
    }

    // ── PUT /branch/{id} ─────────────────────────────────────────────────────

    @Operation(
        summary = "Update a branch",
        description = "Performs a partial update on the specified branch — only fields present in the request body are changed. Requires OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Branch updated successfully"),
        @ApiResponse(responseCode = "403", description = "Caller does not have OFFICE_ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BranchDtos.BranchResponse> updateBranch(
            @Parameter(description = "UUID of the branch to update", required = true)
            @PathVariable String id,
            @Valid @RequestBody BranchDtos.UpdateBranchRequest request,
            @Parameter(description = "Caller's user UUID — injected by the API Gateway from the JWT. When testing directly on Swagger, paste your user UUID here.", example = "00000000-0000-0000-0000-000000000001") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("PUT /branches/{} userId={} role={}", id, userId, userRole);
        return ResponseEntity.ok(branchService.updateBranch(id, request, userId, userRole));
    }

    // ── PATCH /branch/{id}/activate ───────────────────────────────────────────

    @Operation(
        summary = "Activate a branch",
        description = "Sets the branch's active flag to true, making it available for customer orders and visible in nearby searches. Requires OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Branch activated"),
        @ApiResponse(responseCode = "403", description = "Caller does not have OFFICE_ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<BranchDtos.BranchResponse> activate(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String id,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        assertAdmin(userRole);
        return ResponseEntity.ok(branchService.setActive(id, true));
    }

    // ── PATCH /branch/{id}/deactivate ─────────────────────────────────────────

    @Operation(
        summary = "Deactivate a branch",
        description = "Sets the branch's active flag to false, temporarily removing it from customer-facing searches without deleting it. Requires OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Branch deactivated"),
        @ApiResponse(responseCode = "403", description = "Caller does not have OFFICE_ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BranchDtos.BranchResponse> deactivate(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String id,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        assertAdmin(userRole);
        return ResponseEntity.ok(branchService.setActive(id, false));
    }

    // ── GET /branch/{id}/hours ────────────────────────────────────────────────

    @Operation(
        summary = "Get branch operating hours",
        description = "Returns the weekly schedule for the specified branch — one entry per day of the week, each with opening and closing times.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of operating-hour entries (7 entries for a full week)"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/{id}/hours")
    public ResponseEntity<List<BranchDtos.BranchHoursResponse>> getHours(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(branchService.getHours(id));
    }

    // ── PUT /branch/{id}/hours ────────────────────────────────────────────────

    @Operation(
        summary = "Set branch operating hours",
        description = "Replaces the branch's entire weekly schedule with the supplied list. Send all 7 days to define a complete schedule, or a subset to update specific days. Requires OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated list of operating-hour entries"),
        @ApiResponse(responseCode = "403", description = "Caller does not have OFFICE_ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @PutMapping("/{id}/hours")
    public ResponseEntity<List<BranchDtos.BranchHoursResponse>> setHours(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String id,
            @Valid @RequestBody List<BranchDtos.BranchHoursRequest> hours,
            @Parameter(description = "Caller's user UUID — injected by the API Gateway from the JWT. When testing directly on Swagger, paste your user UUID here.", example = "00000000-0000-0000-0000-000000000001") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("PUT /branches/{}/hours userId={} role={}", id, userId, userRole);
        return ResponseEntity.ok(branchService.setHours(id, hours, userId, userRole));
    }

    // ── DELETE /branch/{id} ───────────────────────────────────────────────────

    @Operation(
        summary = "Delete a branch",
        description = "Permanently removes the branch and all its associated operating hours from the database. Requires OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Branch deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Caller does not have OFFICE_ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(
            @Parameter(description = "UUID of the branch to delete", required = true)
            @PathVariable String id,
            @Parameter(description = "Caller's user UUID — injected by the API Gateway from the JWT. When testing directly on Swagger, paste your user UUID here.", example = "00000000-0000-0000-0000-000000000001") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("DELETE /branches/{} userId={} role={}", id, userId, userRole);
        assertAdmin(userRole);
        branchService.deleteBranch(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /branches/{id}/status ───────────────────────────────────────────

    @Operation(
        summary = "Set branch active status",
        description = "Updates the active status of the specified branch using the isActive field in the request body. Requires admin role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Branch status updated"),
        @ApiResponse(responseCode = "403", description = "Caller does not have admin role"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<BranchDtos.BranchResponse> updateStatus(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String id,
            @RequestBody BranchDtos.UpdateBranchStatusRequest request,
            @Parameter(description = "Caller's role — injected by the API Gateway from the JWT. When testing directly on Swagger, enter HEAD_OFFICE_ADMIN for admin access.", example = "HEAD_OFFICE_ADMIN") @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        assertAdmin(userRole);
        return ResponseEntity.ok(branchService.setActive(id, request.isActive()));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void assertAdmin(String userRole) {
        if (userRole == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication required — add X-User-Role header (value: HEAD_OFFICE_ADMIN)");
        }
        if (!"HEAD_OFFICE_ADMIN".equals(userRole) && !"Admin".equals(userRole) && !"OFFICE_ADMIN".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
