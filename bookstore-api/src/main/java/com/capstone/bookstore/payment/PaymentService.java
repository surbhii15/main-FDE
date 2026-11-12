package com.capstone.bookstore.payment;

import com.capstone.bookstore.common.ApiException;
import com.capstone.bookstore.common.BusinessRuleException;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.order.Order;
import com.capstone.bookstore.order.OrderDtos.PaymentResultResponse;
import com.capstone.bookstore.order.OrderMapper;
import com.capstone.bookstore.order.OrderRepository;
import com.capstone.bookstore.order.OrderStatus;
import com.capstone.bookstore.payment.PaymentDtos.PaymentMethodOption;
import com.capstone.bookstore.payment.PaymentDtos.PaymentRequest;
import com.capstone.bookstore.payment.PaymentDtos.PaymentResponse;
import com.capstone.bookstore.points.GiftPointService;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

    private final OrderRepository orders;
    private final PaymentGateway gateway;
    private final GiftPointService points;
    private final OrderMapper mapper;
    private final Clock clock;

    public PaymentService(OrderRepository orders, PaymentGateway gateway, GiftPointService points,
                          OrderMapper mapper, Clock clock) {
        this.orders = orders;
        this.gateway = gateway;
        this.points = points;
        this.mapper = mapper;
        this.clock = clock;
    }

    public static List<PaymentMethodOption> availableMethods() {
        return List.of(
                new PaymentMethodOption(PaymentMethod.CREDIT_CARD, PaymentMethod.CREDIT_CARD.label(), "card"),
                new PaymentMethodOption(PaymentMethod.DEBIT_CARD, PaymentMethod.DEBIT_CARD.label(), "card"),
                new PaymentMethodOption(PaymentMethod.UPI, PaymentMethod.UPI.label(), "upiId"),
                new PaymentMethodOption(PaymentMethod.NET_BANKING, PaymentMethod.NET_BANKING.label(), "bankCode"),
                new PaymentMethodOption(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CASH_ON_DELIVERY.label(),
                        "nothing"));
    }

    /**
     * Pays for a pending order. A declined payment is recorded and the order stays PENDING_PAYMENT,
     * so the customer can retry with another method.
     */
    public PaymentResultResponse pay(Long userId, Long orderId, PaymentRequest req) {
        validateDetails(req);
        Order order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessRuleException.conflict("ORDER_NOT_PAYABLE",
                    "Order " + order.getOrderNumber() + " is " + order.getStatus() + " and cannot be paid");
        }

        PaymentGateway.ChargeResult result = gateway.charge(req, order.getTotal(), order.getOrderNumber());
        Payment payment = new Payment();
        payment.setMethod(req.method());
        payment.setStatus(result.status());
        payment.setAmount(order.getTotal());
        payment.setCardBrand(result.cardBrand());
        payment.setCardLast4(result.cardLast4());
        payment.setTransactionRef(result.transactionRef());
        payment.setFailureReason(result.failureReason());
        payment.setCreatedAt(clock.instant());
        order.addPayment(payment);

        String message;
        if (result.approved()) {
            order.setStatus(OrderStatus.CONFIRMED);
            int earned = points.pointsEarnedFor(order.getTotal());
            order.setPointsEarned(earned);
            points.earn(userId, order.getId(), earned);
            message = mapper.message(order) + (earned > 0 ? " You earned " + earned + " gift points." : "");
        } else {
            message = "Payment failed: " + result.failureReason() + ". Your order is still reserved; "
                    + "please try again or choose another payment method.";
        }
        orders.flush(); // assign the payment id before building the response
        return new PaymentResultResponse(PaymentResponse.from(payment), mapper.toResponse(order), message);
    }

    private static void validateDetails(PaymentRequest req) {
        String missing = switch (req.method()) {
            case CREDIT_CARD, DEBIT_CARD -> req.card() == null ? "card" : null;
            case UPI -> req.upiId() == null ? "upiId" : null;
            case NET_BANKING -> req.bankCode() == null ? "bankCode" : null;
            case CASH_ON_DELIVERY -> null;
        };
        if (missing != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_DETAILS_MISSING",
                    "'" + missing + "' is required for payment method " + req.method());
        }
    }
}
