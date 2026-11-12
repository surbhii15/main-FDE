package com.capstone.bookstore.points;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ledger entry for every change to a user's gift-point balance. */
@Entity
@Table(name = "gift_point_transactions")
@Getter
@Setter
@NoArgsConstructor
public class GiftPointTransaction {

    public enum Type { EARNED, REDEEMED, REFUNDED, REVERSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    /** Positive when credited, negative when debited. */
    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
