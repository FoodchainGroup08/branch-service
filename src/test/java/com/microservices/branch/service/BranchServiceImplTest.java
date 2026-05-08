package com.microservices.branch.service;

import com.microservices.branch.dto.BranchDtos;
import com.microservices.branch.entity.Branch;
import com.microservices.branch.entity.BranchHours;
import com.microservices.branch.repository.BranchHoursRepository;
import com.microservices.branch.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock BranchRepository branchRepository;
    @Mock BranchHoursRepository branchHoursRepository;
    @InjectMocks BranchServiceImpl service;

    private Branch branch(String id, boolean active) {
        return Branch.builder()
                .id(id)
                .name("Test Branch")
                .address("123 Main St")
                .phone("555-1234")
                .description("A test branch")
                .active(active)
                .managerId("mgr-1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createBranch ──────────────────────────────────────────────────────────

    @Test
    void createBranch_savesAndReturnsResponse() {
        BranchDtos.CreateBranchRequest req = new BranchDtos.CreateBranchRequest(
                "Branch A", "1 Street", "555", "desc", "mgr-1", null, null);
        Branch saved = branch("id-1", false);
        when(branchRepository.save(any(Branch.class))).thenReturn(saved);

        BranchDtos.BranchResponse result = service.createBranch(req);

        assertThat(result.id()).isEqualTo("id-1");
        assertThat(result.name()).isEqualTo("Test Branch");
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void createBranch_withCoords_persistsLatLng() {
        BranchDtos.CreateBranchRequest req = new BranchDtos.CreateBranchRequest(
                "Branch A", "1 St", "555", "desc", "mgr-1", 51.5, -0.12);
        Branch saved = Branch.builder().id("id-1").name("Branch A").address("1 St")
                .latitude(51.5).longitude(-0.12).build();
        when(branchRepository.save(any(Branch.class))).thenReturn(saved);

        BranchDtos.BranchResponse result = service.createBranch(req);

        assertThat(result.latitude()).isEqualTo(51.5);
        assertThat(result.longitude()).isEqualTo(-0.12);
    }

    // ── listBranches ──────────────────────────────────────────────────────────

    @Test
    void listBranches_withActiveFilter_callsFindByActive() {
        Page<Branch> page = new PageImpl<>(List.of(branch("id-1", true)));
        when(branchRepository.findByActive(true, PageRequest.of(0, 10))).thenReturn(page);

        Page<BranchDtos.BranchSummary> result = service.listBranches(true, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(branchRepository).findByActive(true, PageRequest.of(0, 10));
    }

    @Test
    void listBranches_withoutFilter_callsFindAll() {
        when(branchRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of()));

        service.listBranches(null, PageRequest.of(0, 10));

        verify(branchRepository).findAll(PageRequest.of(0, 10));
    }

    // ── getBranch ─────────────────────────────────────────────────────────────

    @Test
    void getBranch_found_returnsResponse() {
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(branch("id-1", true)));

        BranchDtos.BranchResponse result = service.getBranch("id-1");

        assertThat(result.id()).isEqualTo("id-1");
    }

    @Test
    void getBranch_notFound_throws404() {
        when(branchRepository.findById("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBranch("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── updateBranch ──────────────────────────────────────────────────────────

    @Test
    void updateBranch_byAdmin_succeeds() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        when(branchRepository.save(any())).thenReturn(b);

        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                "New Name", null, null, null, null, null, null);
        service.updateBranch("id-1", req, "admin-1", "OFFICE_ADMIN");

        assertThat(b.getName()).isEqualTo("New Name");
        verify(branchRepository).save(b);
    }

    @Test
    void updateBranch_byOwnManager_succeeds() {
        Branch b = branch("id-1", true); // managerId = "mgr-1"
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        when(branchRepository.save(any())).thenReturn(b);

        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                null, "New Address", null, null, null, null, null);
        service.updateBranch("id-1", req, "mgr-1", "BRANCH_MANAGER");

        assertThat(b.getAddress()).isEqualTo("New Address");
    }

    @Test
    void updateBranch_byOtherManager_throwsForbidden() {
        Branch b = branch("id-1", true); // managerId = "mgr-1"
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));

        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateBranch("id-1", req, "other-mgr", "BRANCH_MANAGER"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void updateBranch_updatesCoords() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        when(branchRepository.save(any())).thenReturn(b);

        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                null, null, null, null, null, 40.7128, -74.0060);
        service.updateBranch("id-1", req, "admin-1", "OFFICE_ADMIN");

        assertThat(b.getLatitude()).isEqualTo(40.7128);
        assertThat(b.getLongitude()).isEqualTo(-74.0060);
    }

    // ── setActive ─────────────────────────────────────────────────────────────

    @Test
    void setActive_activatesBranch() {
        Branch b = branch("id-1", false);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        when(branchRepository.save(any())).thenReturn(b);

        service.setActive("id-1", true);

        assertThat(b.isActive()).isTrue();
    }

    @Test
    void setActive_deactivatesBranch() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        when(branchRepository.save(any())).thenReturn(b);

        service.setActive("id-1", false);

        assertThat(b.isActive()).isFalse();
    }

    // ── getHours ──────────────────────────────────────────────────────────────

    @Test
    void getHours_returnsListForBranch() {
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(branch("id-1", true)));
        BranchHours h = BranchHours.builder().id("h-1").dayOfWeek(0).closed(true).build();
        when(branchHoursRepository.findByBranch_IdOrderByDayOfWeekAsc("id-1")).thenReturn(List.of(h));

        List<BranchDtos.BranchHoursResponse> result = service.getHours("id-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).dayOfWeek()).isEqualTo(0);
        assertThat(result.get(0).dayName()).isEqualTo("Monday");
        assertThat(result.get(0).closed()).isTrue();
    }

    @Test
    void getHours_branchNotFound_throws404() {
        when(branchRepository.findById("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHours("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── setHours ──────────────────────────────────────────────────────────────

    @Test
    void setHours_replacesExistingAndReturns() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        BranchHours saved = BranchHours.builder().id("h-1").branch(b).dayOfWeek(1)
                .openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(17, 0)).closed(false).build();
        when(branchHoursRepository.saveAll(anyList())).thenReturn(List.of(saved));

        List<BranchDtos.BranchHoursRequest> req = List.of(
                new BranchDtos.BranchHoursRequest(1, "09:00", "17:00", false));
        List<BranchDtos.BranchHoursResponse> result = service.setHours("id-1", req, "admin-1", "OFFICE_ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).openTime()).isEqualTo("09:00");
        verify(branchHoursRepository).deleteByBranch_Id("id-1");
        verify(branchHoursRepository).saveAll(anyList());
    }

    @Test
    void setHours_closedDay_nullTimesAccepted() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));
        BranchHours saved = BranchHours.builder().id("h-1").branch(b).dayOfWeek(6)
                .closed(true).build();
        when(branchHoursRepository.saveAll(anyList())).thenReturn(List.of(saved));

        List<BranchDtos.BranchHoursRequest> req = List.of(
                new BranchDtos.BranchHoursRequest(6, null, null, true));
        List<BranchDtos.BranchHoursResponse> result = service.setHours("id-1", req, "admin-1", "OFFICE_ADMIN");

        assertThat(result.get(0).closed()).isTrue();
        assertThat(result.get(0).openTime()).isNull();
    }

    @Test
    void setHours_invalidTimeFormat_throws400() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));

        List<BranchDtos.BranchHoursRequest> req = List.of(
                new BranchDtos.BranchHoursRequest(0, "not-a-time", "bad", false));

        assertThatThrownBy(() -> service.setHours("id-1", req, "admin-1", "OFFICE_ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(400));
    }

    // ── deleteBranch ──────────────────────────────────────────────────────────

    @Test
    void deleteBranch_byAdmin_deletesSuccessfully() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));

        service.deleteBranch("id-1", "admin-1", "OFFICE_ADMIN");

        verify(branchRepository).delete(b);
    }

    @Test
    void deleteBranch_notAdmin_throwsForbidden() {
        Branch b = branch("id-1", true);
        when(branchRepository.findById("id-1")).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.deleteBranch("id-1", "mgr-1", "BRANCH_MANAGER"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void deleteBranch_notFound_throws404() {
        when(branchRepository.findById("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteBranch("bad", "admin-1", "OFFICE_ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── findNearby ────────────────────────────────────────────────────────────

    @Test
    void findNearby_returnsWithinRadius_sortedByDistance() {
        // London ~0.9 km from query point, Manchester ~262 km away
        Branch london = Branch.builder().id("london").name("London").address("A")
                .active(true).latitude(51.5074).longitude(-0.1278).build();
        Branch manchester = Branch.builder().id("manchester").name("Manchester").address("B")
                .active(true).latitude(53.4808).longitude(-2.2426).build();
        when(branchRepository.findByActiveTrueAndLatitudeNotNullAndLongitudeNotNull())
                .thenReturn(List.of(manchester, london));

        // 51.5, -0.12 with 20 km radius — only London qualifies
        List<BranchDtos.NearbyBranchResponse> result = service.findNearby(51.5, -0.12, 20.0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("london");
    }

    @Test
    void findNearby_multipleWithinRadius_orderedByDistance() {
        Branch b1 = Branch.builder().id("b1").name("B1").address("A")
                .active(true).latitude(51.51).longitude(-0.12).build();
        Branch b2 = Branch.builder().id("b2").name("B2").address("B")
                .active(true).latitude(51.52).longitude(-0.13).build();
        when(branchRepository.findByActiveTrueAndLatitudeNotNullAndLongitudeNotNull())
                .thenReturn(List.of(b2, b1));

        List<BranchDtos.NearbyBranchResponse> result = service.findNearby(51.50, -0.12, 50.0);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).distanceKm()).isLessThanOrEqualTo(result.get(1).distanceKm());
    }

    @Test
    void findNearby_noBranchesWithCoords_returnsEmpty() {
        when(branchRepository.findByActiveTrueAndLatitudeNotNullAndLongitudeNotNull())
                .thenReturn(List.of());

        List<BranchDtos.NearbyBranchResponse> result = service.findNearby(0.0, 0.0, 10.0);

        assertThat(result).isEmpty();
    }
}
