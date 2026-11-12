package com.capstone.bookstore.cart;

import com.capstone.bookstore.cart.CartDtos.CartItemResponse;
import com.capstone.bookstore.cart.CartDtos.CartResponse;
import com.capstone.bookstore.catalog.Product;
import com.capstone.bookstore.catalog.ProductRepository;
import com.capstone.bookstore.common.BusinessRuleException;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;
    private final Clock clock;

    public CartService(CartRepository carts, ProductRepository products, UserRepository users, Clock clock) {
        this.carts = carts;
        this.products = products;
        this.users = users;
        this.clock = clock;
    }

    public CartResponse getCart(Long userId) {
        return toResponse(loadOrCreate(userId));
    }

    public CartResponse addItem(Long userId, Long productId, int quantity) {
        Cart cart = loadOrCreate(userId);
        addOrIncrement(cart, productId, quantity, true);
        touch(cart);
        return toResponse(cart);
    }

    public CartResponse updateQuantity(Long userId, Long productId, int quantity) {
        Cart cart = loadOrCreate(userId);
        CartItem item = cart.findItem(productId)
                .orElseThrow(() -> new NotFoundException("Cart item for product", productId));
        if (quantity == 0) {
            cart.getItems().remove(item);
        } else {
            ensureAvailable(item.getProduct(), quantity);
            item.setQuantity(quantity);
        }
        touch(cart);
        return toResponse(cart);
    }

    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = loadOrCreate(userId);
        CartItem item = cart.findItem(productId)
                .orElseThrow(() -> new NotFoundException("Cart item for product", productId));
        cart.getItems().remove(item);
        touch(cart);
        return toResponse(cart);
    }

    public CartResponse clear(Long userId) {
        Cart cart = loadOrCreate(userId);
        cart.getItems().clear();
        touch(cart);
        return toResponse(cart);
    }

    /**
     * Adds up to {@code quantity} of a product, capped by stock; used by "buy it again".
     * Returns the quantity actually added (0 if unavailable).
     */
    public int addCapped(Long userId, Product product, int quantity) {
        Cart cart = loadOrCreate(userId);
        if (!product.isPurchasable()) {
            return 0;
        }
        int already = cart.findItem(product.getId()).map(CartItem::getQuantity).orElse(0);
        int addable = Math.min(quantity, product.getStockQuantity() - already);
        if (addable <= 0) {
            return 0;
        }
        addOrIncrement(cart, product.getId(), addable, false);
        touch(cart);
        return addable;
    }

    /** Used by checkout: the cart entity with items and products loaded. */
    public Cart loadForCheckout(Long userId) {
        return loadOrCreate(userId);
    }

    private void addOrIncrement(Cart cart, Long productId, int quantity, boolean strict) {
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        CartItem item = cart.findItem(productId).orElse(null);
        int newQty = (item == null ? 0 : item.getQuantity()) + quantity;
        if (strict) {
            ensureAvailable(product, newQty);
        }
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            cart.getItems().add(item);
        }
        item.setQuantity(newQty);
    }

    private void ensureAvailable(Product product, int requestedQty) {
        if (!product.isActive()) {
            throw new BusinessRuleException("PRODUCT_UNAVAILABLE", product.getTitle() + " is no longer available");
        }
        if (requestedQty > product.getStockQuantity()) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK",
                    "Only " + product.getStockQuantity() + " left of " + product.getTitle());
        }
    }

    private Cart loadOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(users.getReferenceById(userId));
            c.setUpdatedAt(clock.instant());
            return carts.save(c);
        });
    }

    private void touch(Cart cart) {
        cart.setUpdatedAt(clock.instant());
    }

    CartResponse toResponse(Cart cart) {
        LocalDate today = LocalDate.now(clock);
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            boolean available = p.isActive() && p.getStockQuantity() >= i.getQuantity();
            return new CartItemResponse(p.getId(), p.getSku(), p.getTitle(), p.getAuthor(), p.getImageUrl(),
                    p.getPrice(), i.getQuantity(), line, available, p.getStockQuantity(),
                    p.estimatedDeliveryDate(today));
        }).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemResponse::quantity).sum();
        LocalDate eta = items.stream().map(CartItemResponse::estimatedDeliveryDate)
                .max(Comparator.naturalOrder()).orElse(null);
        boolean ready = !items.isEmpty() && items.stream().allMatch(CartItemResponse::available);
        return new CartResponse(items, count, subtotal, eta, ready);
    }
}
