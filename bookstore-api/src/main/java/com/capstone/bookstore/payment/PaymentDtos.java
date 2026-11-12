package com.capstone.bookstore.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record CardDetails(
            @NotBlank @Pattern(regexp = "^[0-9 ]{12,23}$", message = "must contain 12-19 digits") String number,
            @NotBlank @Size(max = 100) String holderName,
            @NotNull @Min(1) @Max(12) Integer expiryMonth,
            @NotNull @Min(2000) @Max(2100) Integer expiryYear,
            @NotBlank @Pattern(regexp = "^[0-9]{3,4}$", message = "must be 3 or 4 digits") String cvv) {

        @Override
        public String toString() {
            return "CardDetails[****]"; // never log card data
        }
    }

    /**
     * card is required for CREDIT_CARD / DEBIT_CARD, upiId for UPI, bankCode for NET_BANKING;
     * CASH_ON_DELIVERY needs nothing else.
     */
    public record PaymentRequest(
            @NotNull PaymentMethod method,
            @Valid CardDetails card,
            @Pattern(regexp = "^[\\w.-]{2,64}@[A-Za-z]{2,64}$", message = "must look like name@bank") String upiId,
            @Pattern(regexp = "^[A-Z]{3,10}$", message = "must be a bank code such as HDFC") String bankCode) {}

    public record PaymentResponse(Long id, PaymentMethod method, PaymentStatus status, BigDecimal amount,
                                  String cardBrand, String cardLast4, String transactionRef, String failureReason,
                                  Instant createdAt) {

        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getMethod(), p.getStatus(), p.getAmount(), p.getCardBrand(),
                    p.getCardLast4(), p.getTransactionRef(), p.getFailureReason(), p.getCreatedAt());
        }
    }

    public record PaymentMethodOption(PaymentMethod method, String label, String requires) {}
}
