package com.capstone.bookstore.order;

import com.capstone.bookstore.payment.Payment;
import com.capstone.bookstore.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "points_redeemed", nullable = false)
    private int pointsRedeemed;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // Shipping address snapshot
    @Column(name = "ship_recipient_name", nullable = false)
    private String shipRecipientName;
    @Column(name = "ship_line1", nullable = false)
    private String shipLine1;
    @Column(name = "ship_line2")
    private String shipLine2;
    @Column(name = "ship_city", nullable = false)
    private String shipCity;
    @Column(name = "ship_state")
    private String shipState;
    @Column(name = "ship_postal_code", nullable = false)
    private String shipPostalCode;
    @Column(name = "ship_country", nullable = false)
    private String shipCountry;
    @Column(name = "ship_phone")
    private String shipPhone;

    @Column(name = "estimated_delivery_date", nullable = false)
    private LocalDate estimatedDeliveryDate;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Both collections are Sets: when items and payments are fetched together the SQL join repeats rows,
    // and a List would then contain duplicates (e.g. stock released twice on cancel).
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<OrderItem> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<Payment> payments = new LinkedHashSet<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addPayment(Payment payment) {
        payment.setOrder(this);
        payments.add(payment);
    }
}
