package com.healthbox.hms_backend.security.tenant;

public final class TenantContext {
    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPER_ADMIN = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long tenantId, boolean superAdmin) {
        TENANT.set(tenantId);
        SUPER_ADMIN.set(superAdmin);
    }

    public static Long tenantId() {
        return TENANT.get();
    }

    public static boolean isSuperAdmin() {
        return Boolean.TRUE.equals(SUPER_ADMIN.get());
    }

    public static void clear() {
        TENANT.remove();
        SUPER_ADMIN.remove();
    }
}
