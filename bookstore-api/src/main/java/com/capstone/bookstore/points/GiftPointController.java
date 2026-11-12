package com.capstone.bookstore.points;

import com.capstone.bookstore.points.GiftPointService.PointsSummaryResponse;
import com.capstone.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Gift points", description = "Gift-point balance and history (journey step 10)")
public class GiftPointController {

    private final GiftPointService points;

    public GiftPointController(GiftPointService points) {
        this.points = points;
    }

    @GetMapping("/api/users/me/gift-points")
    @Operation(summary = "My gift-point balance, redemption rules and recent transactions")
    public PointsSummaryResponse summary(@AuthenticationPrincipal AuthUser user) {
        return points.summary(user.id());
    }
}
