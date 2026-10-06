package dev.forgeflow.api.organization;

/**
 * Ordered from lowest to highest privilege. Ordinal order is used by {@link #isAtLeast} to
 * implement simple RBAC checks ("must be at least ADMIN"), so the declaration order below is
 * load-bearing -- do not reorder without reviewing every isAtLeast call site.
 */
public enum MembershipRole {
    MEMBER,
    ADMIN,
    OWNER;

    public boolean isAtLeast(MembershipRole other) {
        return this.ordinal() >= other.ordinal();
    }
}
