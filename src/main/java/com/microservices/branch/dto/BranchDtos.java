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

    public record UpdateBranchStatusRequest(boolean isActive) {}

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
            boolean isActive,
            String managerId,
            LocalDateTime createdAt,
            Double latitude,
            Double longitude,
            List<BranchHoursResponse> hours,
            String hoursDisplay,
            Double rating,
            boolean isOpen
    ) {}

    public record BranchSummary(
            String id,
            String name,
            String address,
            boolean active,
            boolean isActive,
            String managerId,
            Double latitude,
            Double longitude,
            Double rating,
            boolean isOpen,
            String hoursDisplay
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
            boolean isActive,
            Double latitude,
            Double longitude,
            double distanceKm,
            String distance,
            Double rating,
            boolean isOpen,
            String hoursDisplay
    ) {}
}
