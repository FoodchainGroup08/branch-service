package com.microservices.branch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.branch.dto.BranchDtos;
import com.microservices.branch.entity.Branch;
import com.microservices.branch.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BranchControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BranchRepository branchRepository;

    @BeforeEach
    void setUp() {
        branchRepository.deleteAll();
    }

    private Branch persistBranch() {
        return branchRepository.save(Branch.builder()
                .name("Test Branch")
                .address("1 Test Street")
                .phone("555-0001")
                .description("Integration test branch")
                .managerId("mgr-1")
                .build());
    }

    // ── POST /branches ────────────────────────────────────────────────────────

    @Test
    void createBranch_admin_returns201WithBody() throws Exception {
        BranchDtos.CreateBranchRequest req = new BranchDtos.CreateBranchRequest(
                "New Branch", "10 New St", "555-0002", "desc", "mgr-2", 51.5, -0.12);

        mockMvc.perform(post("/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("New Branch"))
                .andExpect(jsonPath("$.latitude").value(51.5))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void createBranch_nonAdmin_returns403() throws Exception {
        BranchDtos.CreateBranchRequest req = new BranchDtos.CreateBranchRequest(
                "Branch", "St", "555", "desc", "mgr", null, null);

        mockMvc.perform(post("/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    // ── GET /branches ─────────────────────────────────────────────────────────

    @Test
    void listBranches_returns200WithPagedContent() throws Exception {
        persistBranch();

        mockMvc.perform(get("/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Test Branch"));
    }

    @Test
    void listBranches_activeFilter_returnsOnlyActive() throws Exception {
        branchRepository.save(Branch.builder().name("Active").address("A").active(true).build());
        branchRepository.save(Branch.builder().name("Inactive").address("B").active(false).build());

        mockMvc.perform(get("/branches").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Active"));
    }

    @Test
    void listBranches_inactiveFilter_returnsOnlyInactive() throws Exception {
        branchRepository.save(Branch.builder().name("Active").address("A").active(true).build());
        branchRepository.save(Branch.builder().name("Inactive").address("B").active(false).build());

        mockMvc.perform(get("/branches").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Inactive"));
    }

    // ── GET /branches/{id} ────────────────────────────────────────────────────

    @Test
    void getBranch_found_returns200() throws Exception {
        Branch b = persistBranch();

        mockMvc.perform(get("/branches/" + b.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(b.getId()))
                .andExpect(jsonPath("$.name").value("Test Branch"))
                .andExpect(jsonPath("$.hours").isArray());
    }

    @Test
    void getBranch_notFound_returns404() throws Exception {
        mockMvc.perform(get("/branches/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /branches/{id} ───────────────────────────────────────────────────

    @Test
    void updateBranch_byAdmin_returns200WithUpdatedFields() throws Exception {
        Branch b = persistBranch();
        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                "Updated Name", null, null, null, null, 40.71, -74.00);

        mockMvc.perform(put("/branches/" + b.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.latitude").value(40.71));
    }

    @Test
    void updateBranch_byOwnManager_returns200() throws Exception {
        Branch b = persistBranch(); // managerId = "mgr-1"
        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                null, "Updated Address", null, null, null, null, null);

        mockMvc.perform(put("/branches/" + b.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "mgr-1")
                        .header("X-User-Role", "BRANCH_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Updated Address"));
    }

    @Test
    void updateBranch_byOtherUser_returns403() throws Exception {
        Branch b = persistBranch(); // managerId = "mgr-1"
        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                "Hacked", null, null, null, null, null, null);

        mockMvc.perform(put("/branches/" + b.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "other-user")
                        .header("X-User-Role", "BRANCH_MANAGER"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /branches/{id} ─────────────────────────────────────────────────

    @Test
    void deleteBranch_admin_returns204AndRemovesBranch() throws Exception {
        Branch b = persistBranch();

        mockMvc.perform(delete("/branches/" + b.getId())
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isNoContent());

        assertThat(branchRepository.findById(b.getId())).isEmpty();
    }

    @Test
    void deleteBranch_nonAdmin_returns403() throws Exception {
        Branch b = persistBranch();

        mockMvc.perform(delete("/branches/" + b.getId())
                        .header("X-User-Id", "mgr-1")
                        .header("X-User-Role", "BRANCH_MANAGER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteBranch_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/branches/nonexistent")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /branches/{id}/activate ─────────────────────────────────────────

    @Test
    void activate_admin_returns200AndActivates() throws Exception {
        Branch b = persistBranch(); // active = false by default

        mockMvc.perform(patch("/branches/" + b.getId() + "/activate")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activate_nonAdmin_returns403() throws Exception {
        Branch b = persistBranch();

        mockMvc.perform(patch("/branches/" + b.getId() + "/activate")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /branches/{id}/deactivate ──────────────────────────────────────

    @Test
    void deactivate_admin_returns200AndDeactivates() throws Exception {
        Branch b = branchRepository.save(Branch.builder()
                .name("Active Branch").address("A").active(true).managerId("mgr-1").build());

        mockMvc.perform(patch("/branches/" + b.getId() + "/deactivate")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ── GET /branches/{id}/hours ──────────────────────────────────────────────

    @Test
    void getHours_returnsEmptyListByDefault() throws Exception {
        Branch b = persistBranch();

        mockMvc.perform(get("/branches/" + b.getId() + "/hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getHours_branchNotFound_returns404() throws Exception {
        mockMvc.perform(get("/branches/nonexistent/hours"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /branches/{id}/hours ──────────────────────────────────────────────

    @Test
    void setHours_admin_returns200WithHoursList() throws Exception {
        Branch b = persistBranch();
        List<BranchDtos.BranchHoursRequest> hours = List.of(
                new BranchDtos.BranchHoursRequest(0, "09:00", "17:00", false),
                new BranchDtos.BranchHoursRequest(1, "09:00", "17:00", false),
                new BranchDtos.BranchHoursRequest(6, null, null, true));

        mockMvc.perform(put("/branches/" + b.getId() + "/hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hours))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].dayName").value("Monday"))
                .andExpect(jsonPath("$[0].openTime").value("09:00"))
                .andExpect(jsonPath("$[2].closed").value(true));
    }

    @Test
    void setHours_invalidTime_returns400() throws Exception {
        Branch b = persistBranch();
        List<BranchDtos.BranchHoursRequest> hours = List.of(
                new BranchDtos.BranchHoursRequest(0, "not-a-time", "bad", false));

        mockMvc.perform(put("/branches/" + b.getId() + "/hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hours))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setHours_replacesPreviousHours() throws Exception {
        Branch b = persistBranch();
        List<BranchDtos.BranchHoursRequest> initial = List.of(
                new BranchDtos.BranchHoursRequest(0, "08:00", "16:00", false));
        List<BranchDtos.BranchHoursRequest> updated = List.of(
                new BranchDtos.BranchHoursRequest(0, "10:00", "18:00", false));

        mockMvc.perform(put("/branches/" + b.getId() + "/hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/branches/" + b.getId() + "/hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].openTime").value("10:00"));
    }

    // ── GET /branches/nearby ──────────────────────────────────────────────────

    @Test
    void findNearby_returns200WithBranchesWithinRadius() throws Exception {
        branchRepository.save(Branch.builder()
                .name("London Branch").address("London").active(true)
                .latitude(51.5074).longitude(-0.1278).build());
        branchRepository.save(Branch.builder()
                .name("Manchester Branch").address("Manchester").active(true)
                .latitude(53.4808).longitude(-2.2426).build());

        mockMvc.perform(get("/branches/nearby")
                        .param("lat", "51.5")
                        .param("lng", "-0.12")
                        .param("radiusKm", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("London Branch"))
                .andExpect(jsonPath("$[0].distanceKm").isNumber());
    }

    @Test
    void findNearby_branchesWithoutCoords_excluded() throws Exception {
        branchRepository.save(Branch.builder()
                .name("No Coords Branch").address("Somewhere").active(true).build());

        mockMvc.perform(get("/branches/nearby")
                        .param("lat", "51.5")
                        .param("lng", "-0.12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findNearby_usesDefaultRadius_whenNotProvided() throws Exception {
        // Branch within default 10 km radius
        branchRepository.save(Branch.builder()
                .name("Nearby").address("A").active(true)
                .latitude(51.505).longitude(-0.125).build());
        // Branch outside 10 km but within 50 km
        branchRepository.save(Branch.builder()
                .name("Far").address("B").active(true)
                .latitude(51.6).longitude(0.2).build());

        mockMvc.perform(get("/branches/nearby")
                        .param("lat", "51.5")
                        .param("lng", "-0.12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Nearby"));
    }

    // ── Validation: POST /branches ─────────────────────────────────────────────

    @Test
    void createBranch_returns400_whenNameIsBlank() throws Exception {
        BranchDtos.CreateBranchRequest req = new BranchDtos.CreateBranchRequest(
                "", "10 New St", "555-0002", "desc", "mgr-2", null, null);

        mockMvc.perform(post("/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBranch_returns400_whenAddressIsBlank() throws Exception {
        BranchDtos.CreateBranchRequest req = new BranchDtos.CreateBranchRequest(
                "Valid Name", "   ", "555-0002", "desc", "mgr-2", null, null);

        mockMvc.perform(post("/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ── Error shape: GET /branches/{id} ───────────────────────────────────────

    @Test
    void getBranch_returns404_whenBranchNotFound() throws Exception {
        mockMvc.perform(get("/branches/no-such-branch"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").isString())
                .andExpect(jsonPath("$.message").isString());
    }

    // ── Error shape: PUT /branches/{id} ───────────────────────────────────────

    @Test
    void updateBranch_returns404_whenBranchNotFound() throws Exception {
        BranchDtos.UpdateBranchRequest req = new BranchDtos.UpdateBranchRequest(
                "New Name", null, null, null, null, null, null);

        mockMvc.perform(put("/branches/no-such-branch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").isString())
                .andExpect(jsonPath("$.message").isString());
    }

    // ── Validation: PUT /branches/{id}/hours ──────────────────────────────────

    @Test
    void setHours_returns400_forInvalidTimeFormat() throws Exception {
        Branch b = persistBranch();
        List<BranchDtos.BranchHoursRequest> hours = List.of(
                new BranchDtos.BranchHoursRequest(0, "99:99", "bad-time", false));

        mockMvc.perform(put("/branches/" + b.getId() + "/hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hours))
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "OFFICE_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    // ── Validation: GET /branches/nearby ──────────────────────────────────────

    @Test
    void nearbySearch_returns400_forNegativeRadius() throws Exception {
        mockMvc.perform(get("/branches/nearby")
                        .param("lat", "51.5")
                        .param("lng", "-0.12")
                        .param("radiusKm", "-5.0"))
                .andExpect(status().isBadRequest());
    }
}
