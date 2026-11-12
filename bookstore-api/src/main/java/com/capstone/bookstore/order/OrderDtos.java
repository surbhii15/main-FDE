package com.capstone.bookstore.order;

import com.capstone.bookstore.cart.CartDtos.CartResponse;
import com.capstone.bookstore.payment.PaymentDtos.PaymentMethodOption;
import com.capstone.bookstore.payment.PaymentDtos.PaymentResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public record CheckoutRequest(@NotNull Long addressId, @Min(0) Integer pointsToRedeem) {}

    /** Payment page data: what the order would cost and how many points can be used. */
    public record CheckoutPreviewResponse(CartResponse cart, BigDecimal subtotal, int giftPointBalance,
                                          int maxRedeemablePoints, int pointsToRedeem, BigDecimal discount,
                                          BigDecimal total, int pointsToEarn,
                                          List<PaymentMethodOption> paymentMethods) {}

    public record ShippingAddress(String recipientName, String line1, String line2, String city, String state,
                                  String postalCode, String country, String phone) {}

    public record OrderItemResponse(Long productId, String title, BigDecimal unitPrice, int quantity,
                                    BigDecimal lineTotal) {}

    public record OrderResponse(Long id, String orderNumber, OrderStatus status, Instant placedAt,
                                List<OrderItemResponse> items, BigDecimal subtotal, int pointsRedeemed,
                                BigDecimal discount, BigDecimal total, int pointsEarned,
                                ShippingAddress shippingAddress, LocalDate estimatedDeliveryDate,
                                List<PaymentResponse> payments, boolean cancellable, Instant cancellableUntil,
                                Instant cancelledAt, String message) {}

    public record OrderSummaryResponse(Long id, String orderNumber, OrderStatus status, Instant placedAt,
                                       int itemCount, List<String> itemTitles, BigDecimal total,
                                       LocalDate estimatedDeliveryDate, boolean cancellable) {}

    public record PaymentResultResponse(PaymentResponse payment, OrderResponse order, String message) {}
}
