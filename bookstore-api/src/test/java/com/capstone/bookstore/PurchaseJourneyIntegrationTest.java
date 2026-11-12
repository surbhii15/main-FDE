package com.capstone.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.bookstore.support.IntegrationTestBase;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** End-to-end customer journey from the capstone wireframes (slides 3-10). */
class PurchaseJourneyIntegrationTest extends IntegrationTestBase {

    @Test
    void customerBrowsesBuysEarnsAndRedeemsPointsThenCancels() throws Exception {
        String token = registerCustomer();
        String author = "Journey Author";
        long bookA = createProduct(author, 5, 3, "200.00");
        long bookB = createProduct(author, 10, 5, "500.00");

        // Product page: tentative delivery date and related products (same author)
        getJson("/api/products/" + bookA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.estimatedDeliveryDate").value(LocalDate.of(2026, 11, 13).toString()))
                .andExpect(jsonPath("$.relatedProducts[0].id").value(bookB));

        // Cart
        postJson("/api/cart/items", Map.of("productId", bookA, "quantity", 2), token).andExpect(status().isOk());
        postJson("/api/cart/items", Map.of("productId", bookB, "quantity", 1), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(3))
                .andExpect(jsonPath("$.subtotal").value(900.00))
                .andExpect(jsonPath("$.estimatedDeliveryDate").value("2026-11-15"))
                .andExpect(jsonPath("$.readyForCheckout").value(true));

        // Checkout (new customer has no points yet)
        long addressId = addAddress(token);
        var order = postJson("/api/orders", Map.of("addressId", addressId), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.orderNumber", startsWith("BK261110-")))
                .andExpect(jsonPath("$.total").value(900.00))
                .andExpect(jsonPath("$.estimatedDeliveryDate").value("2026-11-15"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Pune"));
        long orderId = readLong(order, "$.id");
        getJson("/api/cart", token).andExpect(jsonPath("$.itemCount").value(0));
        getJson("/api/products/" + bookA, null).andExpect(jsonPath("$.product.stockQuantity").value(3));

        // Declined payment keeps the order payable
        postJson("/api/orders/" + orderId + "/payments",
                Map.of("method", "DEBIT_CARD", "card", card("4000 0000 0000 0002")), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.status").value("FAILED"))
                .andExpect(jsonPath("$.payment.failureReason").value("Insufficient funds"))
                .andExpect(jsonPath("$.order.status").value("PENDING_PAYMENT"));

        // Successful payment confirms the purchase and earns 90 points (1 per 10)
        postJson("/api/orders/" + orderId + "/payments",
                Map.of("method", "CREDIT_CARD", "card", card("4111 1111 1111 1111")), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.status").value("SUCCESS"))
                .andExpect(jsonPath("$.payment.cardLast4").value("1111"))
                .andExpect(jsonPath("$.order.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.order.pointsEarned").value(90))
                .andExpect(jsonPath("$.message", containsString("purchase is complete")));
        getJson("/api/users/me/gift-points", token).andExpect(jsonPath("$.balance").value(90));

        // Cannot pay twice
        postJson("/api/orders/" + orderId + "/payments", Map.of("method", "CASH_ON_DELIVERY"), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_PAYABLE"));

        // Order history
        getJson("/api/orders", token)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].itemCount").value(3))
                .andExpect(jsonPath("$.content[0].cancellable").value(true));

        // Recommendations are driven by order history and exclude purchased books
        getJson("/api/recommendations", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].product.id", not(hasItem((int) bookA))))
                .andExpect(jsonPath("$[*].product.id", not(hasItem((int) bookB))));

        // Buy it again: both lines come back exactly once (regression: items duplicated by fetch join)
        postJson("/api/orders/" + orderId + "/buy-again", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added", hasSize(2)))
                .andExpect(jsonPath("$.cart.itemCount").value(3));

        // Second order redeems points: max 50% of subtotal and within the balance
        postJson("/api/orders", Map.of("addressId", addressId, "pointsToRedeem", 91), token)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"));
        getJson("/api/checkout/preview?pointsToRedeem=90", token)
                .andExpect(jsonPath("$.maxRedeemablePoints").value(90))
                .andExpect(jsonPath("$.total").value(810.00))
                .andExpect(jsonPath("$.paymentMethods", hasSize(5)));
        var second = postJson("/api/orders", Map.of("addressId", addressId, "pointsToRedeem", 90), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discount").value(90))
                .andExpect(jsonPath("$.total").value(810.00));
        long secondId = readLong(second, "$.id");
        getJson("/api/users/me/gift-points", token).andExpect(jsonPath("$.balance").value(0));

        // Cancel the first order: stock released once, earned points reversed (balance can't go negative)
        postJson("/api/orders/" + orderId + "/cancel", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payments[?(@.status == 'SUCCESS')]", hasSize(0)))
                .andExpect(jsonPath("$.payments[?(@.status == 'REFUNDED')]", hasSize(1)))
                .andExpect(jsonPath("$.cancellable").value(false));
        getJson("/api/products/" + bookA, null).andExpect(jsonPath("$.product.stockQuantity").value(3));
        postJson("/api/orders/" + orderId + "/cancel", null, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_ALREADY_CANCELLED"));

        // Cancelling the unpaid second order returns the 90 redeemed points and its stock
        postJson("/api/orders/" + secondId + "/cancel", null, token).andExpect(status().isOk());
        getJson("/api/users/me/gift-points", token)
                .andExpect(jsonPath("$.balance").value(90))
                .andExpect(jsonPath("$.recentTransactions[*].type",
                        hasItem("REFUNDED")));
        getJson("/api/products/" + bookA, null).andExpect(jsonPath("$.product.stockQuantity").value(5));
    }

    @Test
    void orderCannotBeCancelledAfter48Hours() throws Exception {
        String token = registerCustomer();
        long book = createProduct("Window Author", 3, 2, "150.00");
        postJson("/api/cart/items", Map.of("productId", book, "quantity", 1), token);
        long orderId = readLong(postJson("/api/orders", Map.of("addressId", addAddress(token)), token), "$.id");
        postJson("/api/orders/" + orderId + "/payments", Map.of("method", "UPI", "upiId", "test@okbank"), token)
                .andExpect(jsonPath("$.order.status").value("CONFIRMED"));
        String email = read(getJson("/api/users/me", token), "$.email");

        clock.advance(Duration.ofHours(47).plusMinutes(59));
        String fresh = login(email, "Secret@123"); // the original 2-hour token has expired
        getJson("/api/orders/" + orderId, fresh).andExpect(jsonPath("$.cancellable").value(true));

        clock.advance(Duration.ofMinutes(2));
        postJson("/api/orders/" + orderId + "/cancel", null, fresh)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CANCELLATION_WINDOW_EXPIRED"));
        getJson("/api/orders/" + orderId, fresh).andExpect(jsonPath("$.cancellable").value(false));
    }

    @Test
    void checkoutRejectsEmptyCartOversoldStockAndForeignAddress() throws Exception {
        String token = registerCustomer();
        long addressId = addAddress(token);
        postJson("/api/orders", Map.of("addressId", addressId), token)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CART_EMPTY"));

        long book = createProduct("Stock Author", 4, 2, "100.00");
        postJson("/api/cart/items", Map.of("productId", book, "quantity", 5), token)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        postJson("/api/cart/items", Map.of("productId", book, "quantity", 4), token).andExpect(status().isOk());

        // Someone else buys 3 of the 4 copies first
        String other = registerCustomer();
        postJson("/api/cart/items", Map.of("productId", book, "quantity", 3), other);
        postJson("/api/orders", Map.of("addressId", addAddress(other)), other).andExpect(status().isCreated());

        getJson("/api/cart", token).andExpect(jsonPath("$.readyForCheckout").value(false));
        postJson("/api/orders", Map.of("addressId", addressId), token)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        getJson("/api/products/" + book, null).andExpect(jsonPath("$.product.stockQuantity").value(1));

        // Another customer's address is not visible
        long otherAddress = addAddress(other);
        putJson("/api/cart/items/" + book, Map.of("quantity", 1), token).andExpect(status().isOk());
        postJson("/api/orders", Map.of("addressId", otherAddress), token).andExpect(status().isNotFound());
    }

    @Test
    void catalogueSearchFiltersAndSorts() throws Exception {
        getJson("/api/categories", null).andExpect(jsonPath("$[*].slug", hasItem("fiction")));
        getJson("/api/brands", null).andExpect(jsonPath("$[*].name", hasItem("Pebble Kids")));

        var byCategory = getJson("/api/categories/1/products?sort=PRICE_DESC", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].category.name", everyItem(org.hamcrest.Matchers.is("Fiction"))));
        List<Double> prices = read(byCategory, "$.content[*].price");
        assertThat(prices).isSortedAccordingTo((a, b) -> Double.compare(b, a));

        getJson("/api/products?q=austen", null).andExpect(jsonPath("$.totalElements").value(2));
        getJson("/api/products?brandId=5&inStock=true", null)
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Peter Pan"))));
        getJson("/api/products?minPrice=500&maxPrice=100", null)
                .andExpect(status().isUnprocessableEntity());
        getJson("/api/products?sort=BOGUS", null).andExpect(status().isBadRequest());
        getJson("/api/products/99999", null).andExpect(status().isNotFound());
    }

    @Test
    void addressBookKeepsExactlyOneDefault() throws Exception {
        String token = registerCustomer();
        long first = addAddress(token);
        long second = readLong(postJson("/api/addresses", Map.of("recipientName", "Office", "line1", "2 Work Rd",
                "city", "Pune", "postalCode", "411002", "country", "India", "isDefault", true), token), "$.id");
        getJson("/api/addresses", token)
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[?(@.isDefault == true)]", hasSize(1)));
        deleteJson("/api/addresses/" + second, token).andExpect(status().isNoContent());
        getJson("/api/addresses", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }
}
