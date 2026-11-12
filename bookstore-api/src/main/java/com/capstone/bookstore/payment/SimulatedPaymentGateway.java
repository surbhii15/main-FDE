package com.capstone.bookstore.payment;

import com.capstone.bookstore.payment.PaymentDtos.CardDetails;
import com.capstone.bookstore.payment.PaymentDtos.PaymentRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real provider so the purchase flow can be demonstrated end to end.
 * <ul>
 *   <li>Cards must pass the Luhn check and must not be expired.</li>
 *   <li>A card number ending in {@value #DECLINE_SUFFIX} is declined (insufficient funds).</li>
 *   <li>A UPI id with the handle {@code @fail} is declined.</li>
 * </ul>
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    static final String DECLINE_SUFFIX = "0002";

    private final Clock clock;

    public SimulatedPaymentGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ChargeResult charge(PaymentRequest req, BigDecimal amount, String orderNumber) {
        return switch (req.method()) {
            case CREDIT_CARD, DEBIT_CARD -> chargeCard(req.card());
            case UPI -> req.upiId().toLowerCase().endsWith("@fail")
                    ? declined(null, null, "UPI request declined by the payer's bank")
                    : approved(null, null);
            case NET_BANKING -> approved(null, null);
            case CASH_ON_DELIVERY -> new ChargeResult(true, PaymentStatus.PENDING, ref("COD"), null, null, null);
        };
    }

    @Override
    public String refund(String transactionRef, BigDecimal amount) {
        return ref("RFND");
    }

    private ChargeResult chargeCard(CardDetails card) {
        String digits = card.number().replace(" ", "");
        String last4 = digits.substring(digits.length() - 4);
        String brand = brandOf(digits);
        if (digits.length() < 12 || digits.length() > 19 || !luhnValid(digits)) {
            return declined(brand, last4, "Invalid card number");
        }
        if (YearMonth.of(card.expiryYear(), card.expiryMonth()).isBefore(YearMonth.now(clock))) {
            return declined(brand, last4, "Card has expired");
        }
        if (digits.endsWith(DECLINE_SUFFIX)) {
            return declined(brand, last4, "Insufficient funds");
        }
        return approved(brand, last4);
    }

    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            if (dbl) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    static String brandOf(String digits) {
        if (digits.startsWith("4")) {
            return "VISA";
        }
        if (digits.matches("^(5[1-5]|2[2-7]).*")) {
            return "MASTERCARD";
        }
        if (digits.matches("^3[47].*")) {
            return "AMEX";
        }
        if (digits.matches("^(60|65|81|82|508).*")) {
            return "RUPAY";
        }
        return "CARD";
    }

    private ChargeResult approved(String brand, String last4) {
        return new ChargeResult(true, PaymentStatus.SUCCESS, ref("TXN"), brand, last4, null);
    }

    private ChargeResult declined(String brand, String last4, String reason) {
        return new ChargeResult(false, PaymentStatus.FAILED, ref("TXN"), brand, last4, reason);
    }

    private static String ref(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
