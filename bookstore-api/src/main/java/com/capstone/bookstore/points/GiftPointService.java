package com.capstone.bookstore.points;

import com.capstone.bookstore.common.BusinessRuleException;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.config.AppProperties;
import com.capstone.bookstore.points.GiftPointTransaction.Type;
import com.capstone.bookstore.user.User;
import com.capstone.bookstore.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gift points: 1 point = 1.00 off, redeemable up to a configured share of the subtotal;
 * earned at 1 point per {@code earnPerAmount} of the paid total.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class GiftPointService {

    public record PointsTransactionResponse(Long id, Long orderId, int points, Type type, String description,
                                            Instant createdAt) {}

    public record PointsSummaryResponse(int balance, int maxRedeemPercent, int earnPerAmount,
                                        List<PointsTransactionResponse> recentTransactions) {}

    private final UserRepository users;
    private final GiftPointTransactionRepository ledger;
    private final AppProperties.Points rules;
    private final Clock clock;

    public GiftPointService(UserRepository users, GiftPointTransactionRepository ledger, AppProperties props,
                            Clock clock) {
        this.users = users;
        this.ledger = ledger;
        this.rules = props.points();
        this.clock = clock;
    }

    /** Most points that may be redeemed against this subtotal (ignoring the user's balance). */
    @Transactional(propagation = Propagation.SUPPORTS)
    public int maxRedeemableFor(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.valueOf(rules.maxRedeemPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR).intValue();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public int pointsEarnedFor(BigDecimal paidTotal) {
        return paidTotal.divide(BigDecimal.valueOf(rules.earnPerAmount()), 0, RoundingMode.FLOOR).intValue();
    }

    public void redeem(Long userId, Long orderId, int points, BigDecimal subtotal) {
        if (points <= 0) {
            return;
        }
        User u = lock(userId);
        if (points > u.getGiftPoints()) {
            throw new BusinessRuleException("INSUFFICIENT_POINTS",
                    "You have " + u.getGiftPoints() + " points; cannot redeem " + points);
        }
        int max = maxRedeemableFor(subtotal);
        if (points > max) {
            throw new BusinessRuleException("POINTS_LIMIT_EXCEEDED",
                    "At most " + max + " points (" + rules.maxRedeemPercent() + "% of the subtotal) can be redeemed");
        }
        apply(u, orderId, -points, Type.REDEEMED, "Redeemed on order");
    }

    public void earn(Long userId, Long orderId, int points) {
        if (points > 0) {
            apply(lock(userId), orderId, points, Type.EARNED, "Earned on order");
        }
    }

    public void refundRedeemed(Long userId, Long orderId, int points) {
        if (points > 0) {
            apply(lock(userId), orderId, points, Type.REFUNDED, "Refunded: order cancelled");
        }
    }

    /** Takes back points earned on a cancelled order, never pushing the balance below zero. */
    public int reverseEarned(Long userId, Long orderId, int points) {
        if (points <= 0) {
            return 0;
        }
        User u = lock(userId);
        int reversible = Math.min(points, u.getGiftPoints());
        if (reversible > 0) {
            apply(u, orderId, -reversible, Type.REVERSED, "Reversed: order cancelled");
        }
        return reversible;
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public PointsSummaryResponse summary(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
        List<PointsTransactionResponse> recent = ledger.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(t -> new PointsTransactionResponse(t.getId(), t.getOrderId(), t.getPoints(), t.getType(),
                        t.getDescription(), t.getCreatedAt()))
                .toList();
        return new PointsSummaryResponse(u.getGiftPoints(), rules.maxRedeemPercent(), rules.earnPerAmount(), recent);
    }

    private User lock(Long userId) {
        return users.findByIdForUpdate(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private void apply(User u, Long orderId, int delta, Type type, String description) {
        u.setGiftPoints(u.getGiftPoints() + delta);
        GiftPointTransaction t = new GiftPointTransaction();
        t.setUserId(u.getId());
        t.setOrderId(orderId);
        t.setPoints(delta);
        t.setType(type);
        t.setDescription(description);
        t.setCreatedAt(clock.instant());
        ledger.save(t);
    }
}
