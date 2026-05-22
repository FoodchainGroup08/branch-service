package com.microservices.branch.service;

import com.microservices.branch.dto.TableDtos;
import com.microservices.branch.entity.BranchTable;

import java.util.List;

public interface TableService {

    List<TableDtos.TableResponse> getTables(String branchId);

    TableDtos.TableResponse createTable(String branchId, TableDtos.CreateTableRequest request);

    void deleteTable(String tableId);

    TableDtos.TableResponse updateTableStatus(String tableId, BranchTable.TableStatus status);

    TableDtos.TableResponse lockTable(String branchId, Integer tableNumber);

    TableDtos.TableResponse freeTable(String tableId);
}
