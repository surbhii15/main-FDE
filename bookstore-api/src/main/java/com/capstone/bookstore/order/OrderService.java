package com.capstone.bookstore.order;

import com.capstone.bookstore.address.Address;
import com.capstone.bookstore.address.AddressService;
import com.capstone.bookstore.cart.Cart;
import com.capstone.bookstore.cart.CartDtos.BuyAgainResponse;
import com.capstone.bookstore.cart.CartDtos.CartResponse;
import com.capstone.bookstore.cart.CartItem;
import com.capstone.bookstore.cart.CartService;
import com.capstone.bookstore.catalog.Product;
import com.capstone.bookstore.catalog.ProductRepository;
import com.capstone.bookstore.common.BusinessRuleException;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.common.PageResponse;
import com.capstone.bookstore.order.OrderDtos.CheckoutPreviewResponse;
import com.capstone.bookstore.order.OrderDtos.CheckoutRequest;
import com.capstone.bookstore.order.OrderDtos.OrderResponse;
import com.capstone.bookstore.order.OrderDtos.OrderSummaryResponse;
import com.capstone.bookstore.payment.Payment;
import com.capstone.bookstore.payment.PaymentGateway;
import com.capstone.bookstore.payment.PaymentService;
import com.capstone.bookstore.payment.PaymentStatus;
import com.capstone.bookstore.points.GiftPointService;
import com.capstone.bookstore.user.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final OrderRepository orders;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductRepository products;
    private final UserRepository users;
    private final GiftPointService points;
    private final PaymentGateway gateway;
    private final OrderMapper mapper;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orders, CartService cartService, AddressService addressService,
                        ProductRepository products, UserRepository users, GiftPointService points,
                        PaymentGateway gateway, OrderMapper mapper, Clock clock) {
        this.orders = orders;
        this.cartService = cartService;
        this.addressService = addressService;
        this.products = products;
        this.users = users;
        this.points = points;
        this.gateway = gateway;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Payment page: totals for the current cart with the requested gift points applied. */
    public CheckoutPreviewResponse preview(Long userId, int pointsToRedeem) {
        CartResponse cart = cartService.getCart(userId);
        int balance = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId)).getGiftPoints();
        int max = Math.min(balance, points.maxRedeemableFor(cart.subtotal()));
        validatePoints(pointsToRedeem, balance, cart.subtotal());
        BigDecimal discount = BigDecimal.valueOf(pointsToRedeem);
        BigDecimal total = cart.subtotal().subtract(discount);
        return new CheckoutPreviewResponse(cart, cart.subtotal(), balance, max, pointsToRedeem, discount, total,
                points.pointsEarnedFor(total), PaymentService.availableMethods());
    }

    /**
     * Places an order from the cart: reserves stock, redeems gift points, snapshots the address and
     * prices, and empties the cart. The order then waits for payment.
     */
    public OrderResponse checkout(Long userId, CheckoutRequest req) {
        Cart cart = cartService.loadForCheckout(userId);
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException("CART_EMPTY", "Your cart is empty");
        }
        Address address = addressService.getOwned(userId, req.addressId());
        int pointsToRedeem = req.pointsToRedeem() == null ? 0 : req.pointsToRedeem();

        LocalDate today = LocalDate.now(clock);
        Order order = new Order();
        order.setUser(users.getReferenceById(userId));
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPlacedAt(clock.instant());
        order.setOrderNumber(newOrderNumber());
        snapshotAddress(order, address);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate eta = today;
        // Reserve in product-id order so concurrent checkouts lock rows consistently.
        List<CartItem> items = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId())).toList();
        for (CartItem ci : items) {
            Product p = ci.getProduct();
            if (products.reserveStock(p.getId(), ci.getQuantity()) == 0) {
                throw new BusinessRuleException("INSUFFICIENT_STOCK",
                        p.getTitle() + " is unavailable in the requested quantity (" + ci.getQuantity() + ")");
            }
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setTitle(p.getTitle());
            oi.setUnitPrice(p.getPrice());
            oi.setQuantity(ci.getQuantity());
            oi.setLineTotal(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            order.addItem(oi);
            subtotal = subtotal.add(oi.getLineTotal());
            LocalDate itemEta = p.estimatedDeliveryDate(today);
            if (itemEta.isAfter(eta)) {
                eta = itemEta;
            }
        }

        BigDecimal discount = BigDecimal.valueOf(pointsToRedeem);
        order.setSubtotal(subtotal);
        order.setPointsRedeemed(pointsToRedeem);
        order.setDiscount(discount);
        order.setTotal(subtotal.subtract(discount));
        order.setEstimatedDeliveryDate(eta);
        orders.save(order);

        points.redeem(userId, order.getId(), pointsToRedeem, subtotal); // validates balance and limit under lock
        cart.getItems().clear();
        return mapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> history(Long userId, OrderStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt", "id"));
        Page<Order> result = status == null ? orders.findByUserId(userId, pr)
                : orders.findByUserIdAndStatus(userId, status, pr);
        // Load items for the whole page in one query, then map in page order.
        Map<Long, Order> withItems = result.getContent().isEmpty() ? Map.of()
                : orders.findWithItemsByIdIn(result.map(Order::getId).getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        return PageResponse.of(result, o -> mapper.toSummary(withItems.getOrDefault(o.getId(), o)));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return mapper.toResponse(loadOwned(userId, orderId));
    }

    /**
     * Cancels an order within the cancellation window: releases stock, refunds or voids payments,
     * returns redeemed points and takes back points earned on it.
     */
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = loadOwned(userId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw BusinessRuleException.conflict("ORDER_ALREADY_CANCELLED", "Order is already cancelled");
        }
        if (!clock.instant().isBefore(mapper.cancellableUntil(order))) {
            throw new BusinessRuleException("CANCELLATION_WINDOW_EXPIRED",
                    "Orders can only be cancelled within 48 hours of purchase (until "
                            + mapper.cancellableUntil(order) + ")");
        }
        order.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .forEach(i -> products.releaseStock(i.getProduct().getId(), i.getQuantity()));

        for (Payment p : order.getPayments()) {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                gateway.refund(p.getTransactionRef(), p.getAmount());
                p.setStatus(PaymentStatus.REFUNDED);
            } else if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.CANCELLED);
            }
        }
        points.refundRedeemed(userId, order.getId(), order.getPointsRedeemed());
        points.reverseEarned(userId, order.getId(), order.getPointsEarned());

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(clock.instant());
        return mapper.toResponse(order);
    }

    /** "Buy it again": copies an order's still-available items into the cart. */
    public BuyAgainResponse buyAgain(Long userId, Long orderId) {
        Order order = loadOwned(userId, orderId);
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            int qty = cartService.addCapped(userId, p, item.getQuantity());
            if (qty == 0) {
                skipped.add(item.getTitle() + " (currently unavailable)");
            } else if (qty < item.getQuantity()) {
                added.add(item.getTitle() + " x" + qty + " (only " + qty + " available)");
            } else {
                added.add(item.getTitle() + " x" + qty);
            }
        }
        return new BuyAgainResponse(cartService.getCart(userId), added, skipped);
    }

    Order loadOwned(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> new NotFoundException("Order", orderId));
    }

    private void validatePoints(int requested, int balance, BigDecimal subtotal) {
        if (requested < 0) {
            throw new BusinessRuleException("INVALID_POINTS", "pointsToRedeem must not be negative");
        }
        if (requested > balance) {
            throw new BusinessRuleException("INSUFFICIENT_POINTS",
                    "You have " + balance + " points; cannot redeem " + requested);
        }
        int max = points.maxRedeemableFor(subtotal);
        if (requested > max) {
            throw new BusinessRuleException("POINTS_LIMIT_EXCEEDED", "At most " + max + " points can be redeemed");
        }
    }

    private void snapshotAddress(Order o, Address a) {
        o.setShipRecipientName(a.getRecipientName());
        o.setShipLine1(a.getLine1());
        o.setShipLine2(a.getLine2());
        o.setShipCity(a.getCity());
        o.setShipState(a.getState());
        o.setShipPostalCode(a.getPostalCode());
        o.setShipCountry(a.getCountry());
        o.setShipPhone(a.getPhone());
    }

    private String newOrderNumber() {
        String date = LocalDate.now(clock.withZone(ZoneOffset.UTC)).format(ORDER_DATE);
        String candidate;
        do {
            StringBuilder sb = new StringBuilder("BK").append(date).append('-');
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
            }
            candidate = sb.toString();
        } while (orders.existsByOrderNumber(candidate));
        return candidate;
    }
}
