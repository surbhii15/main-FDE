package com.capstone.bookstore.recommendation;

import com.capstone.bookstore.cart.CartRepository;
import com.capstone.bookstore.catalog.CatalogDtos.ProductResponse;
import com.capstone.bookstore.catalog.Product;
import com.capstone.bookstore.catalog.ProductRepository;
import com.capstone.bookstore.order.OrderItemRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recommends books from the customer's order history: favourite authors and categories first,
 * excluding books already bought or already in the cart, topped up with top-rated titles.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    public record Recommendation(ProductResponse product, String reason) {}

    private static final int TOP_AUTHORS = 3;
    private static final int TOP_CATEGORIES = 3;

    private final OrderItemRepository orderItems;
    private final ProductRepository products;
    private final CartRepository carts;
    private final Clock clock;

    public RecommendationService(OrderItemRepository orderItems, ProductRepository products, CartRepository carts,
                                 Clock clock) {
        this.orderItems = orderItems;
        this.products = products;
        this.carts = carts;
        this.clock = clock;
    }

    public List<Recommendation> forUser(Long userId, int limit) {
        LocalDate today = LocalDate.now(clock);
        Set<Long> exclude = new HashSet<>(orderItems.findPurchasedProductIds(userId));
        carts.findByUserId(userId).ifPresent(c -> c.getItems().forEach(i -> exclude.add(i.getProduct().getId())));
        exclude.add(-1L); // keeps "not in" valid when nothing is excluded

        Map<Long, String> categories = new LinkedHashMap<>();
        orderItems.findTopCategories(userId).stream().limit(TOP_CATEGORIES)
                .forEach(r -> categories.put((Long) r[0], (String) r[1]));
        List<String> authors = orderItems.findTopAuthors(userId).stream().limit(TOP_AUTHORS)
                .map(r -> (String) r[0]).toList();

        List<Recommendation> result = new ArrayList<>();
        if (!categories.isEmpty() || !authors.isEmpty()) {
            List<Product> candidates = products.findRecommendationCandidates(
                    categories.isEmpty() ? List.of(-1L) : categories.keySet(),
                    authors.isEmpty() ? List.of("") : authors,
                    exclude, PageRequest.of(0, limit * 3));
            // Favourite authors first, then favourite categories.
            candidates.stream().filter(p -> authors.contains(p.getAuthor()))
                    .forEach(p -> add(result, exclude, p, "Because you bought books by " + p.getAuthor(), today));
            candidates.stream().filter(p -> !authors.contains(p.getAuthor()))
                    .forEach(p -> add(result, exclude, p,
                            "Popular in " + categories.getOrDefault(p.getCategory().getId(),
                                    p.getCategory().getName()) + ", a category you like", today));
        }
        if (result.size() < limit) {
            products.findTopRated(exclude, PageRequest.of(0, limit))
                    .forEach(p -> add(result, exclude, p, "Top rated in our store", today));
        }
        return result.stream().limit(limit).toList();
    }

    private static void add(List<Recommendation> out, Set<Long> exclude, Product p, String reason, LocalDate today) {
        if (exclude.add(p.getId())) {
            out.add(new Recommendation(ProductResponse.from(p, today), reason));
        }
    }
}
