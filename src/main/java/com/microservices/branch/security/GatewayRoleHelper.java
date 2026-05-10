package com.microservices.branch.security;

/**
 * Headers forwarded by the API gateway after JWT validation. Matches branch/menu/user conventions.
 */
public final class GatewayRoleHelper {

    private GatewayRoleHelper() {}

    /** Head-office admin roles (display name {@code Admin} or enum-style names). */
    public static boolean isHeadOfficeAdminRole(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return false;
        }
        String r = userRole.trim();
        return "HEAD_OFFICE_ADMIN".equalsIgnoreCase(r)
                || "OFFICE_ADMIN".equalsIgnoreCase(r)
                || "Admin".equalsIgnoreCase(r);
    }
}
