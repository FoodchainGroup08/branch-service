package com.microservices.branch.service;

import com.microservices.branch.dto.TableDtos;
import com.microservices.branch.entity.BranchTable;
import com.microservices.branch.repository.BranchRepository;
import com.microservices.branch.repository.BranchTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    @Autowired
    private BranchTableRepository tableRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TableDtos.TableResponse> getTables(String branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        }
        return tableRepository.findByBranchIdOrderByTableNumberAsc(branchId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TableDtos.TableResponse createTable(String branchId, TableDtos.CreateTableRequest request) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found");
        }
        if (tableRepository.findByBranchIdAndTableNumber(branchId, request.tableNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Table " + request.tableNumber() + " already exists for this branch");
        }
        BranchTable table = BranchTable.builder()
                .branchId(branchId)
                .tableNumber(request.tableNumber())
                .capacity(request.capacity())
                .build();
        log.info("Created table {} for branch {}", request.tableNumber(), branchId);
        return toResponse(tableRepository.save(table));
    }

    @Override
    @Transactional
    public void deleteTable(String tableId) {
        BranchTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        if (table.getStatus() != BranchTable.TableStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete table while it is " + table.getStatus().name().toLowerCase());
        }
        tableRepository.delete(table);
        log.info("Deleted table {}", tableId);
    }

    @Override
    @Transactional
    public TableDtos.TableResponse updateTableStatus(String tableId, BranchTable.TableStatus status) {
        BranchTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        table.setStatus(status);
        log.info("Table {} status set to {} by manager", tableId, status);
        return toResponse(tableRepository.save(table));
    }

    @Override
    @Transactional
    public TableDtos.TableResponse lockTable(String branchId, Integer tableNumber) {
        BranchTable table = tableRepository.findByBranchIdAndTableNumber(branchId, tableNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Table " + tableNumber + " not found in branch"));
        if (table.getStatus() != BranchTable.TableStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Table " + tableNumber + " is not available");
        }
        table.setStatus(BranchTable.TableStatus.OCCUPIED);
        log.info("Table {} in branch {} locked (order placed)", tableNumber, branchId);
        return toResponse(tableRepository.save(table));
    }

    @Override
    @Transactional
    public TableDtos.TableResponse freeTable(String tableId) {
        BranchTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        table.setStatus(BranchTable.TableStatus.AVAILABLE);
        log.info("Table {} freed (order completed/cancelled)", tableId);
        return toResponse(tableRepository.save(table));
    }

    // ── private ───────────────────────────────────────────────────────────────

    private TableDtos.TableResponse toResponse(BranchTable t) {
        return new TableDtos.TableResponse(
                t.getId(),
                t.getTableNumber(),
                t.getCapacity(),
                t.getStatus() == BranchTable.TableStatus.AVAILABLE,
                t.getStatus().name()
        );
    }
}
