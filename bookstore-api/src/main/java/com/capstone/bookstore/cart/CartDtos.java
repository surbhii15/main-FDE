package com.capstone.bookstore.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CartDtos {

    private CartDtos() {}

    public record AddCartItemRequest(@NotNull Long productId, @NotNull @Min(1) @Max(99) Integer quantity) {}

    /** quantity 0 removes the item. */
    public record UpdateCartItemRequest(@NotNull @Min(0) @Max(99) Integer quantity) {}

    public record CartItemResponse(Long productId, String sku, String title, String author, String imageUrl,
                                   BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                                   boolean available, int stockQuantity, LocalDate estimatedDeliveryDate) {}

    public record CartResponse(List<CartItemResponse> items, int itemCount, BigDecimal subtotal,
                               LocalDate estimatedDeliveryDate, boolean readyForCheckout) {}

    /** Result of "buy it again": what was added and what was skipped (and why). */
    public record BuyAgainResponse(CartResponse cart, List<String> added, List<String> skipped) {}
}
