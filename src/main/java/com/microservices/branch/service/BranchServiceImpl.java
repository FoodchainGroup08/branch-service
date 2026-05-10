package com.microservices.branch.service;

import com.microservices.branch.dto.BranchDtos;
import com.microservices.branch.entity.Branch;
import com.microservices.branch.entity.BranchHours;
import com.microservices.branch.exception.ResourceNotFoundException;
import com.microservices.branch.repository.BranchHoursRepository;
import com.microservices.branch.repository.BranchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BranchServiceImpl implements BranchService {

    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchHoursRepository branchHoursRepository;

    @Override
    @Transactional
    public BranchDtos.BranchResponse createBranch(BranchDtos.CreateBranchRequest request) {
        log.info("Creating branch: {}", request.name());
        Branch branch = Branch.builder()
                .name(request.name())
                .address(request.address())
                .phone(request.phone())
                .description(request.description())
                .managerId(request.managerId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
        Branch saved = branchRepository.save(branch);
        log.info("Branch created id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchDtos.BranchSummary> listBranches(Boolean activeOnly, Pageable pageable) {
        Page<Branch> page = (activeOnly != null)
                ? branchRepository.findByActive(activeOnly, pageable)
                : branchRepository.findAll(pageable);
        return page.map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDtos.BranchResponse getBranch(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public BranchDtos.BranchResponse updateBranch(String id, BranchDtos.UpdateBranchRequest request,
                                                    String requesterId, String requesterRole) {
        Branch branch = findOrThrow(id);
        assertCanManage(branch, requesterId, requesterRole);

        if (request.name()        != null) branch.setName(request.name());
        if (request.address()     != null) branch.setAddress(request.address());
        if (request.phone()       != null) branch.setPhone(request.phone());
        if (request.description() != null) branch.setDescription(request.description());
        if (request.managerId()   != null) branch.setManagerId(request.managerId());
        if (request.latitude()    != null) branch.setLatitude(request.latitude());
        if (request.longitude()   != null) branch.setLongitude(request.longitude());

        log.info("Branch {} updated by {}", id, requesterId);
        return toResponse(branchRepository.save(branch));
    }

    @Override
    @Transactional
    public BranchDtos.BranchResponse setActive(String id, boolean active) {
        Branch branch = findOrThrow(id);
        branch.setActive(active);
        log.info("Branch {} active={}", id, active);
        return toResponse(branchRepository.save(branch));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDtos.BranchHoursResponse> getHours(String id) {
        findOrThrow(id);
        return branchHoursRepository.findByBranch_IdOrderByDayOfWeekAsc(id)
                .stream().map(this::toHoursResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<BranchDtos.BranchHoursResponse> setHours(String id,
                                                          List<BranchDtos.BranchHoursRequest> hoursRequests,
                                                          String requesterId, String requesterRole) {
        Branch branch = findOrThrow(id);
        assertCanManage(branch, requesterId, requesterRole);

        branchHoursRepository.deleteByBranch_Id(id);
        branchHoursRepository.flush();

        List<BranchHours> hours = hoursRequests.stream().map(req -> {
            LocalTime openTime  = null;
            LocalTime closeTime = null;
            if (!req.closed()) {
                try {
                    openTime  = LocalTime.parse(req.openTime());
                    closeTime = LocalTime.parse(req.closeTime());
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid time format for day " + req.dayOfWeek() + ". Use HH:mm");
                }
            }
            return BranchHours.builder()
                    .branch(branch)
                    .dayOfWeek(req.dayOfWeek().getValue() - 1) // MONDAY=1 → 0, SUNDAY=7 → 6
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .closed(req.closed())
                    .build();
        }).collect(Collectors.toList());

        List<BranchHours> saved = branchHoursRepository.saveAll(hours);
        log.info("Hours set for branch {} by {}", id, requesterId);
        return saved.stream().map(this::toHoursResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBranch(String id, String requesterId, String requesterRole) {
        Branch branch = findOrThrow(id);
        if (!"OFFICE_ADMIN".equals(requesterRole) && !"HEAD_OFFICE_ADMIN".equals(requesterRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can delete branches");
        }
        branchRepository.delete(branch);
        log.info("Branch {} deleted by {}", id, requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDtos.NearbyBranchResponse> findNearby(double lat, double lng, double radiusKm) {
        return branchRepository.findByActiveTrueAndLatitudeNotNullAndLongitudeNotNull()
                .stream()
                .map(b -> {
                    double dist = haversineKm(lat, lng, b.getLatitude(), b.getLongitude());
                    double rating = b.getRating() != null ? b.getRating() : 0.0;
                    String distFormatted = String.format("%.1f km", dist);
                    return new BranchDtos.NearbyBranchResponse(
                            b.getId(), b.getName(), b.getAddress(), b.getPhone(),
                            b.isActive(), b.isActive(),
                            b.getLatitude(), b.getLongitude(),
                            dist, distFormatted,
                            rating, computeIsOpen(b), computeHoursDisplay(b));
                })
                .filter(r -> r.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(BranchDtos.NearbyBranchResponse::distanceKm))
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Branch findOrThrow(String id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
    }

    private void assertCanManage(Branch branch, String requesterId, String requesterRole) {
        boolean isAdmin = "OFFICE_ADMIN".equals(requesterRole) || "HEAD_OFFICE_ADMIN".equals(requesterRole);
        boolean isOwnManager = "BRANCH_MANAGER".equals(requesterRole)
                && requesterId != null
                && requesterId.equals(branch.getManagerId());
        if (!isAdmin && !isOwnManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private BranchDtos.BranchResponse toResponse(Branch b) {
        List<BranchDtos.BranchHoursResponse> hours = b.getHours() == null ? List.of()
                : b.getHours().stream().map(this::toHoursResponse).collect(Collectors.toList());
        double rating = b.getRating() != null ? b.getRating() : 0.0;
        return new BranchDtos.BranchResponse(
                b.getId(), b.getName(), b.getAddress(), b.getPhone(),
                b.getDescription(), b.isActive(), b.isActive(), b.getManagerId(), b.getCreatedAt(),
                b.getLatitude(), b.getLongitude(), hours,
                computeHoursDisplay(b), rating, computeIsOpen(b));
    }

    private BranchDtos.BranchSummary toSummary(Branch b) {
        double rating = b.getRating() != null ? b.getRating() : 0.0;
        return new BranchDtos.BranchSummary(
                b.getId(), b.getName(), b.getAddress(), b.isActive(), b.isActive(),
                b.getManagerId(), b.getLatitude(), b.getLongitude(),
                rating, computeIsOpen(b), computeHoursDisplay(b));
    }

    private BranchDtos.BranchHoursResponse toHoursResponse(BranchHours h) {
        String dayName = (h.getDayOfWeek() >= 0 && h.getDayOfWeek() < 7)
                ? DAY_NAMES[h.getDayOfWeek()] : "Unknown";
        return new BranchDtos.BranchHoursResponse(
                h.getId(), h.getDayOfWeek(), dayName,
                h.getOpenTime()  != null ? h.getOpenTime().toString()  : null,
                h.getCloseTime() != null ? h.getCloseTime().toString() : null,
                h.isClosed());
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // BranchHours.dayOfWeek: 0 = Monday … 6 = Sunday
    // java.time.DayOfWeek:   MONDAY=1 … SUNDAY=7  →  getValue() % 7 gives 1..6,0
    // To map MONDAY→0 … SUNDAY→6: (getValue() - 1)
    private int todayDayOfWeek() {
        return java.time.LocalDate.now().getDayOfWeek().getValue() - 1;
    }

    private boolean computeIsOpen(Branch branch) {
        List<BranchHours> hoursList = branch.getHours();
        if (hoursList == null || hoursList.isEmpty()) return false;
        int dayOfWeek = todayDayOfWeek();
        java.time.LocalTime now = java.time.LocalTime.now();
        return hoursList.stream()
                .filter(h -> !h.isClosed() && h.getDayOfWeek() == dayOfWeek)
                .anyMatch(h -> h.getOpenTime() != null && h.getCloseTime() != null
                        && !now.isBefore(h.getOpenTime()) && !now.isAfter(h.getCloseTime()));
    }

    private String computeHoursDisplay(Branch branch) {
        List<BranchHours> hoursList = branch.getHours();
        if (hoursList == null || hoursList.isEmpty()) return "Hours not set";
        int dayOfWeek = todayDayOfWeek();
        return hoursList.stream()
                .filter(h -> h.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .map(h -> h.isClosed() ? "Closed today"
                        : formatTime(h.getOpenTime()) + " - " + formatTime(h.getCloseTime()))
                .orElse("Hours not available");
    }

    private String formatTime(java.time.LocalTime t) {
        if (t == null) return "";
        return t.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
    }
}
