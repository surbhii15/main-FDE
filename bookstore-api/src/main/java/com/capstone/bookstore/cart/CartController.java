package com.capstone.bookstore.cart;

import com.capstone.bookstore.cart.CartDtos.AddCartItemRequest;
import com.capstone.bookstore.cart.CartDtos.CartResponse;
import com.capstone.bookstore.cart.CartDtos.UpdateCartItemRequest;
import com.capstone.bookstore.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping basket (journey step 8)")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    @Operation(summary = "View my cart with subtotal and tentative delivery date")
    public CartResponse get(@AuthenticationPrincipal AuthUser user) {
        return cart.getCart(user.id());
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart (adds to the existing quantity)")
    public CartResponse add(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddCartItemRequest req) {
        return cart.addItem(user.id(), req.productId(), req.quantity());
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set the quantity of a cart item (0 removes it)")
    public CartResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long productId,
                               @Valid @RequestBody UpdateCartItemRequest req) {
        return cart.updateQuantity(user.id(), productId, req.quantity());
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the cart")
    public CartResponse remove(@AuthenticationPrincipal AuthUser user, @PathVariable Long productId) {
        return cart.removeItem(user.id(), productId);
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public CartResponse clear(@AuthenticationPrincipal AuthUser user) {
        return cart.clear(user.id());
    }
}
