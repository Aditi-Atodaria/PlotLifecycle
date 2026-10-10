package com.realestate.model;

/**
 * The lifecycle states a plot/site moves through.
 * AVAILABLE -> ON_HOLD -> LOAN_PENDING -> BOOKED -> SOLD
 * with CANCELLED as an escape hatch back toward AVAILABLE.
 */
public enum SiteStatus {
    AVAILABLE,
    ON_HOLD,
    LOAN_PENDING,
    BOOKED,
    SOLD,
    CANCELLED
}
