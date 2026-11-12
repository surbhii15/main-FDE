package com.capstone.bookstore.payment;

public enum PaymentStatus {
    /** Charged successfully. */
    SUCCESS,
    /** Cash on delivery: collected when the parcel arrives. */
    PENDING,
    /** Declined by the gateway; the order stays PENDING_PAYMENT so the customer can retry. */
    FAILED,
    /** A successful charge returned after cancellation. */
    REFUNDED,
    /** A pending (cash on delivery) payment voided by cancellation. */
    CANCELLED
}
