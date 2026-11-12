package com.capstone.bookstore.order;

import com.capstone.bookstore.config.AppProperties;
import com.capstone.bookstore.order.OrderDtos.OrderItemResponse;
import com.capstone.bookstore.order.OrderDtos.OrderResponse;
import com.capstone.bookstore.order.OrderDtos.OrderSummaryResponse;
import com.capstone.bookstore.order.OrderDtos.ShippingAddress;
import com.capstone.bookstore.payment.PaymentDtos.PaymentResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final Clock clock;
    private final Duration cancellationWindow;

    public OrderMapper(Clock clock, AppProperties props) {
        this.clock = clock;
        this.cancellationWindow = Duration.ofHours(props.orders().cancellationWindowHours());
    }

    public Instant cancellableUntil(Order o) {
        return o.getPlacedAt().plus(cancellationWindow);
    }

    public boolean isCancellable(Order o) {
        return o.getStatus() != OrderStatus.CANCELLED && clock.instant().isBefore(cancellableUntil(o));
    }

    public OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus(), o.getPlacedAt(),
                o.getItems().stream().map(i -> new OrderItemResponse(i.getProduct().getId(), i.getTitle(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList(),
                o.getSubtotal(), o.getPointsRedeemed(), o.getDiscount(), o.getTotal(), o.getPointsEarned(),
                new ShippingAddress(o.getShipRecipientName(), o.getShipLine1(), o.getShipLine2(), o.getShipCity(),
                        o.getShipState(), o.getShipPostalCode(), o.getShipCountry(), o.getShipPhone()),
                o.getEstimatedDeliveryDate(),
                o.getPayments().stream().map(PaymentResponse::from).toList(),
                isCancellable(o), o.getStatus() == OrderStatus.CANCELLED ? null : cancellableUntil(o),
                o.getCancelledAt(), message(o));
    }

    public OrderSummaryResponse toSummary(Order o) {
        return new OrderSummaryResponse(o.getId(), o.getOrderNumber(), o.getStatus(), o.getPlacedAt(),
                o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                o.getItems().stream().map(OrderItem::getTitle).limit(3).toList(),
                o.getTotal(), o.getEstimatedDeliveryDate(), isCancellable(o));
    }

    /** Customer-facing confirmation text (journey step 12). */
    public String message(Order o) {
        return switch (o.getStatus()) {
            case PENDING_PAYMENT -> "Order " + o.getOrderNumber() + " is reserved and awaiting payment of "
                    + o.getTotal() + ".";
            case CONFIRMED -> "Thank you! Your purchase is complete. Order " + o.getOrderNumber()
                    + " is confirmed and should arrive by " + o.getEstimatedDeliveryDate() + ".";
            case CANCELLED -> "Order " + o.getOrderNumber() + " has been cancelled. Any payment has been refunded"
                    + " and redeemed gift points returned.";
        };
    }
}
