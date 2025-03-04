package my.project.qri3a.enums;

/**
 * Enum representing the various states a product can be in throughout its lifecycle.
 */
public enum ProductStatus {
    /**
     * Initial state when a product is submitted, awaiting review.
     */
    MODERATION,

    /**
     * Approved by the administrator and live on the platform.
     */
    ACTIVE,

    /**
     * Denied by the administrator.
     */
    REJECTED,

    /**
     * Turned off by the user but still stored in the system.
     */
    DEACTIVATED
}