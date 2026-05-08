package com.microservices.branch.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BranchDtos {

    public record CreateBranchRequest(
            String name,
            String address,
            String phone,
            String description,
            String managerId,
            Double latitude,
            Double longitude
    ) {}

    public record UpdateBranchRequest(
            String name,
            String address,
            String phone,
            String description,
            String managerId,
            Double latitude,
            Double longitude
    ) {}

    public record BranchHoursRequest(
            int dayOfWeek,
            String openTime,
            String closeTime,
            boolean closed
    ) {}

    public record BranchResponse(
            String id,
            String name,
            String address,
            String phone,
            String description,
            boolean active,
            String managerId,
            LocalDateTime createdAt,
            Double latitude,
            Double longitude,
            List<BranchHoursResponse> hours
    ) {}

    public record BranchSummary(
            String id,
            String name,
            String address,
            boolean active,
            String managerId,
            Double latitude,
            Double longitude
    ) {}

    public record BranchHoursResponse(
            String id,
            int dayOfWeek,
            String dayName,
            String openTime,
            String closeTime,
            boolean closed
    ) {}

    public record NearbyBranchResponse(
            String id,
            String name,
            String address,
            String phone,
            boolean active,
            Double latitude,
            Double longitude,
            double distanceKm
    ) {}
}
