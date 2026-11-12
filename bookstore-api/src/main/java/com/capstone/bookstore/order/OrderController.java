package com.capstone.bookstore.order;

import com.capstone.bookstore.cart.CartDtos.BuyAgainResponse;
import com.capstone.bookstore.common.PageResponse;
import com.capstone.bookstore.order.OrderDtos.CheckoutPreviewResponse;
import com.capstone.bookstore.order.OrderDtos.CheckoutRequest;
import com.capstone.bookstore.order.OrderDtos.OrderResponse;
import com.capstone.bookstore.order.OrderDtos.OrderSummaryResponse;
import com.capstone.bookstore.order.OrderDtos.PaymentResultResponse;
import com.capstone.bookstore.payment.PaymentDtos.PaymentMethodOption;
import com.capstone.bookstore.payment.PaymentDtos.PaymentRequest;
import com.capstone.bookstore.payment.PaymentService;
import com.capstone.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Checkout & Orders", description = "Checkout, payment, confirmation, order history, buy again and "
        + "cancellation (journey steps 4, 9-12)")
public class OrderController {

    private final OrderService orders;
    private final PaymentService payments;

    public OrderController(OrderService orders, PaymentService payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @GetMapping("/api/checkout/preview")
    @Operation(summary = "Payment page summary: totals with gift points applied, and payment options")
    public CheckoutPreviewResponse preview(@AuthenticationPrincipal AuthUser user,
                                           @RequestParam(defaultValue = "0") @Min(0) int pointsToRedeem) {
        return orders.preview(user.id(), pointsToRedeem);
    }

    @GetMapping("/api/payments/methods")
    @Operation(summary = "Available payment options")
    public List<PaymentMethodOption> paymentMethods() {
        return PaymentService.availableMethods();
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from the cart (choose delivery address, redeem gift points)",
            description = "Reserves stock and points; the order is PENDING_PAYMENT until paid.")
    public OrderResponse checkout(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CheckoutRequest req) {
        return orders.checkout(user.id(), req);
    }

    @PostMapping("/api/orders/{orderId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pay for an order (credit/debit card, UPI, net banking or cash on delivery)",
            description = "Returns the payment record and the purchase confirmation. A declined payment is "
                    + "recorded with status FAILED and the order can be paid again. Test hooks: a card ending "
                    + "in 0002 or a UPI id ending in @fail is declined.")
    public PaymentResultResponse pay(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId,
                                     @Valid @RequestBody PaymentRequest req) {
        return payments.pay(user.id(), orderId, req);
    }

    @GetMapping("/api/orders")
    @Operation(summary = "My order history, newest first")
    public PageResponse<OrderSummaryResponse> history(@AuthenticationPrincipal AuthUser user,
                                                      @RequestParam(required = false) OrderStatus status,
                                                      @RequestParam(defaultValue = "0") @Min(0) int page,
                                                      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orders.history(user.id(), status, page, size);
    }

    @GetMapping("/api/orders/{orderId}")
    @Operation(summary = "Order details and purchase confirmation")
    public OrderResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId) {
        return orders.get(user.id(), orderId);
    }

    @PostMapping("/api/orders/{orderId}/cancel")
    @Operation(summary = "Cancel an order within 48 hours of purchase",
            description = "Releases stock, refunds the payment, returns redeemed points and reverses earned points.")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId) {
        return orders.cancel(user.id(), orderId);
    }

    @PostMapping("/api/orders/{orderId}/buy-again")
    @Operation(summary = "Buy it again: add this order's available items back to the cart")
    public BuyAgainResponse buyAgain(@AuthenticationPrincipal AuthUser user, @PathVariable Long orderId) {
        return orders.buyAgain(user.id(), orderId);
    }
}
