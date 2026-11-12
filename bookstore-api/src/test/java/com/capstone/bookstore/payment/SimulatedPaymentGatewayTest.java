package com.capstone.bookstore.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.capstone.bookstore.payment.PaymentDtos.CardDetails;
import com.capstone.bookstore.payment.PaymentDtos.PaymentRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway =
            new SimulatedPaymentGateway(Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC));

    private PaymentGateway.ChargeResult chargeCard(String number, int month, int year) {
        var card = new CardDetails(number, "Test User", month, year, "123");
        return gateway.charge(new PaymentRequest(PaymentMethod.CREDIT_CARD, card, null, null),
                new BigDecimal("100.00"), "BK-TEST");
    }

    @Test
    void approvesValidCardAndStoresOnlyLast4() {
        var r = chargeCard("4111 1111 1111 1111", 12, 2030);
        assertThat(r.approved()).isTrue();
        assertThat(r.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(r.cardBrand()).isEqualTo("VISA");
        assertThat(r.cardLast4()).isEqualTo("1111");
        assertThat(r.transactionRef()).startsWith("TXN-");
    }

    @Test
    void declinesTestCardEnding0002() {
        var r = chargeCard("4000000000000002", 12, 2030);
        assertThat(r.approved()).isFalse();
        assertThat(r.failureReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void declinesCardFailingLuhn() {
        var r = chargeCard("4111111111111112", 12, 2030);
        assertThat(r.approved()).isFalse();
        assertThat(r.failureReason()).isEqualTo("Invalid card number");
    }

    @Test
    void declinesExpiredCard() {
        var r = chargeCard("4111111111111111", 10, 2026);
        assertThat(r.approved()).isFalse();
        assertThat(r.failureReason()).isEqualTo("Card has expired");
    }

    @Test
    void cardExpiringThisMonthIsStillValid() {
        assertThat(chargeCard("4111111111111111", 11, 2026).approved()).isTrue();
    }

    @Test
    void cashOnDeliveryIsPending() {
        var r = gateway.charge(new PaymentRequest(PaymentMethod.CASH_ON_DELIVERY, null, null, null),
                BigDecimal.TEN, "BK-TEST");
        assertThat(r.approved()).isTrue();
        assertThat(r.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void upiFailHandleIsDeclined() {
        var r = gateway.charge(new PaymentRequest(PaymentMethod.UPI, null, "someone@fail", null),
                BigDecimal.TEN, "BK-TEST");
        assertThat(r.approved()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"4111111111111111,VISA", "5555555555554444,MASTERCARD", "378282246310005,AMEX",
            "6074000000000000,RUPAY"})
    void detectsBrand(String number, String brand) {
        assertThat(SimulatedPaymentGateway.brandOf(number)).isEqualTo(brand);
    }

    @ParameterizedTest
    @CsvSource({"4111111111111111,true", "5555555555554444,true", "4000000000000002,true",
            "4111111111111112,false", "1234567812345678,false"})
    void luhnCheck(String number, boolean valid) {
        assertThat(SimulatedPaymentGateway.luhnValid(number)).isEqualTo(valid);
    }
}
