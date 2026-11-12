package com.capstone.bookstore.payment;

import com.capstone.bookstore.payment.PaymentDtos.PaymentRequest;
import java.math.BigDecimal;

/** Port to a payment provider. The default implementation is {@link SimulatedPaymentGateway}. */
public interface PaymentGateway {

    record ChargeResult(boolean approved, PaymentStatus status, String transactionRef, String cardBrand,
                        String cardLast4, String failureReason) {}

    ChargeResult charge(PaymentRequest request, BigDecimal amount, String orderNumber);

    /** Returns the refund reference. */
    String refund(String transactionRef, BigDecimal amount);
}
