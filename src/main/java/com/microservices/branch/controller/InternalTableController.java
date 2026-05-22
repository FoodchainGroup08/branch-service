package com.microservices.branch.controller;

import com.microservices.branch.dto.TableDtos;
import com.microservices.branch.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal-only endpoints called service-to-service by order-service.
 * Not exposed through the API gateway (no matching gateway route).
 * Secured by network isolation — only reachable on the internal Docker network.
 */
@Slf4j
@RestController
@RequestMapping("/v1/internal/tables")
public class InternalTableController {

    @Autowired
    private TableService tableService;

    @PostMapping("/lock")
    public ResponseEntity<TableDtos.TableResponse> lockTable(
            @RequestBody TableDtos.LockTableRequest request) {
        log.info("Internal: locking table {} in branch {}", request.tableNumber(), request.branchId());
        return ResponseEntity.ok(tableService.lockTable(request.branchId(), request.tableNumber()));
    }

    @PatchMapping("/{tableId}/free")
    public ResponseEntity<TableDtos.TableResponse> freeTable(
            @PathVariable String tableId) {
        log.info("Internal: freeing table {}", tableId);
        return ResponseEntity.ok(tableService.freeTable(tableId));
    }
}
