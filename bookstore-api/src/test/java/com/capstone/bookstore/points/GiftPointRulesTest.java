package com.capstone.bookstore.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.capstone.bookstore.config.AppProperties;
import com.capstone.bookstore.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class GiftPointRulesTest {

    private final GiftPointService service = new GiftPointService(mock(UserRepository.class),
            mock(GiftPointTransactionRepository.class),
            new AppProperties(null, null, new AppProperties.Points(50, 10), null), Clock.systemUTC());

    @Test
    void canRedeemAtMostHalfTheSubtotalRoundedDown() {
        assertThat(service.maxRedeemableFor(new BigDecimal("1497.00"))).isEqualTo(748);
        assertThat(service.maxRedeemableFor(new BigDecimal("1.99"))).isZero();
    }

    @Test
    void earnsOnePointPerTenSpentRoundedDown() {
        assertThat(service.pointsEarnedFor(new BigDecimal("1347.00"))).isEqualTo(134);
        assertThat(service.pointsEarnedFor(new BigDecimal("9.99"))).isZero();
    }
}
