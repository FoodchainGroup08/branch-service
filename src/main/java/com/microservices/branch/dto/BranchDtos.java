package com.microservices.branch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public class BranchDtos {

    public record CreateBranchRequest(
            @NotBlank(message = "Branch name must not be blank")
            String name,
            @NotBlank(message = "Branch address must not be blank")
            String address,
            String phone,
            String description,
            String managerId,
            @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
            @DecimalMax(value =  "90.0",  message = "Latitude must be <= 90")
            Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
            @DecimalMax(value =  "180.0", message = "Longitude must be <= 180")
            Double longitude
    ) {}

    public record UpdateBranchRequest(
            String name,
            String address,
            String phone,
            String description,
            String managerId,
            @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
            @DecimalMax(value =  "90.0",  message = "Latitude must be <= 90")
            Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
            @DecimalMax(value =  "180.0", message = "Longitude must be <= 180")
            Double longitude
    ) {}

    public record UpdateBranchStatusRequest(boolean isActive) {}

    public record BranchHoursRequest(
            DayOfWeek dayOfWeek,
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "openTime must be in HH:mm format")
            String openTime,
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "closeTime must be in HH:mm format")
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
