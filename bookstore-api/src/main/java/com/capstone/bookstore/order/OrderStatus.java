package com.capstone.bookstore.order;

public enum OrderStatus {
    /** Placed from the cart; stock and points are reserved, awaiting a successful payment. */
    PENDING_PAYMENT,
    /** Paid (or cash on delivery accepted); purchase confirmed. */
    CONFIRMED,
    /** Cancelled within the cancellation window; stock, points and payment were reversed. */
    CANCELLED
}
