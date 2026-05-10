package com.microservices.branch.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRoleHelperTest {

    @Test
    void acceptsEnumCaseInsensitiveAndAdminDisplayName() {
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("HEAD_OFFICE_ADMIN")).isTrue();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("head_office_admin")).isTrue();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("OFFICE_ADMIN")).isTrue();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("Admin")).isTrue();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("  Admin  ")).isTrue();
    }

    @Test
    void rejectsOtherRoles() {
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole(null)).isFalse();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("")).isFalse();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("CUSTOMER")).isFalse();
        assertThat(GatewayRoleHelper.isHeadOfficeAdminRole("BRANCH_MANAGER")).isFalse();
    }
}
