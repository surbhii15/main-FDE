package com.capstone.bookstore.payment;

public enum PaymentMethod {
    CREDIT_CARD("Credit card"),
    DEBIT_CARD("Debit card"),
    UPI("UPI"),
    NET_BANKING("Net banking"),
    CASH_ON_DELIVERY("Cash on delivery");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isCard() {
        return this == CREDIT_CARD || this == DEBIT_CARD;
    }
}
