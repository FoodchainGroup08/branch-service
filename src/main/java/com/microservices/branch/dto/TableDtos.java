package com.microservices.branch.dto;

public class TableDtos {

    public record CreateTableRequest(
            Integer tableNumber,
            Integer capacity
    ) {}

    public record LockTableRequest(
            String branchId,
            Integer tableNumber
    ) {}

    public record UpdateTableStatusRequest(
            String status
    ) {}

    public record TableResponse(
            String id,
            int tableNumber,
            int capacity,
            boolean isAvailable,
            String status
    ) {}
}
