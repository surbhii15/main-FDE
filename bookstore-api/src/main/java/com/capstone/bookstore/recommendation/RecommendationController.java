package com.capstone.bookstore.recommendation;

import com.capstone.bookstore.recommendation.RecommendationService.Recommendation;
import com.capstone.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Recommendations", description = "Items recommended from order history (journey step 8)")
public class RecommendationController {

    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @GetMapping("/api/recommendations")
    @Operation(summary = "Books recommended for me, with the reason for each")
    public List<Recommendation> forMe(@AuthenticationPrincipal AuthUser user,
                                      @RequestParam(defaultValue = "6") @Min(1) @Max(20) int limit) {
        return recommendations.forUser(user.id(), limit);
    }
}
