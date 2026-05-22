package com.microservices.branch.controller;

import com.microservices.branch.dto.TableDtos;
import com.microservices.branch.entity.BranchTable;
import com.microservices.branch.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/branches")
@Tag(name = "Tables", description = "Manage dine-in tables per branch. Customers read table availability; managers create and delete tables.")
public class TableController {

    @Autowired
    private TableService tableService;

    // ── GET /v1/branches/{branchId}/tables ────────────────────────────────────

    @Operation(
        summary = "List tables for a branch",
        description = "Returns all tables for the branch with current availability status. Used by the frontend checkout to display available tables.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of tables with availability"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/{branchId}/tables")
    public ResponseEntity<List<TableDtos.TableResponse>> getTables(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String branchId) {
        return ResponseEntity.ok(tableService.getTables(branchId));
    }

    // ── POST /v1/branches/{branchId}/tables ───────────────────────────────────

    @Operation(
        summary = "Add a table to a branch",
        description = "Creates a new table for the specified branch. Requires BRANCH_MANAGER or HEAD_OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Table created"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Branch not found"),
        @ApiResponse(responseCode = "409", description = "Table number already exists for this branch")
    })
    @PostMapping("/{branchId}/tables")
    public ResponseEntity<TableDtos.TableResponse> createTable(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String branchId,
            @RequestBody TableDtos.CreateTableRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id")   String userId,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String userRole) {
        log.info("POST /branches/{}/tables userId={} role={}", branchId, userId, userRole);
        assertManagerOrAdmin(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.createTable(branchId, request));
    }

    // ── DELETE /v1/branches/{branchId}/tables/{tableId} ───────────────────────

    @Operation(
        summary = "Delete a table",
        description = "Permanently removes a table from the branch. Fails if the table is currently occupied or reserved. Requires BRANCH_MANAGER or HEAD_OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Table deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Table not found"),
        @ApiResponse(responseCode = "409", description = "Table is occupied or reserved")
    })
    @DeleteMapping("/{branchId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String branchId,
            @Parameter(description = "UUID of the table", required = true)
            @PathVariable String tableId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id")   String userId,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String userRole) {
        log.info("DELETE /branches/{}/tables/{} userId={} role={}", branchId, tableId, userId, userRole);
        assertManagerOrAdmin(userRole);
        tableService.deleteTable(tableId);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /v1/branches/{branchId}/tables/{tableId}/status ─────────────────

    @Operation(
        summary = "Manually set table status",
        description = "Allows a manager to override table status (e.g. mark as reserved for a phone booking, or set back to available after cleaning). Requires BRANCH_MANAGER or HEAD_OFFICE_ADMIN role.",
        security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Table not found")
    })
    @PatchMapping("/{branchId}/tables/{tableId}/status")
    public ResponseEntity<TableDtos.TableResponse> updateTableStatus(
            @Parameter(description = "UUID of the branch", required = true)
            @PathVariable String branchId,
            @Parameter(description = "UUID of the table", required = true)
            @PathVariable String tableId,
            @RequestBody TableDtos.UpdateTableStatusRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id")   String userId,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String userRole) {
        log.info("PATCH /branches/{}/tables/{}/status status={} userId={}", branchId, tableId, request.status(), userId);
        assertManagerOrAdmin(userRole);
        BranchTable.TableStatus status;
        try {
            status = BranchTable.TableStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Valid values: AVAILABLE, OCCUPIED, RESERVED");
        }
        return ResponseEntity.ok(tableService.updateTableStatus(tableId, status));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void assertManagerOrAdmin(String userRole) {
        if (!"HEAD_OFFICE_ADMIN".equals(userRole) && !"BRANCH_MANAGER".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "BRANCH_MANAGER or HEAD_OFFICE_ADMIN role required");
        }
    }
}
