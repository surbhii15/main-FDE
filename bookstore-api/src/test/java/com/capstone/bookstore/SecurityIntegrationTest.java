package com.capstone.bookstore;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.bookstore.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void catalogueIsPublicButCartNeedsLogin() throws Exception {
        getJson("/api/products", null).andExpect(status().isOk());
        getJson("/api/cart", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        getJson("/api/cart", "not-a-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndProfile() throws Exception {
        var reg = postJson("/api/auth/register", Map.of("email", "New.Reader@Test.local", "password", "Secret@123",
                "fullName", "New Reader", "phone", "+91 98000 00000"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("new.reader@test.local"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
        String token = read(reg, "$.accessToken");
        getJson("/api/users/me", token).andExpect(jsonPath("$.fullName").value("New Reader"));

        postJson("/api/auth/register", Map.of("email", "new.reader@test.local", "password", "Secret@123",
                "fullName", "Dup"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        postJson("/api/auth/login", Map.of("email", "new.reader@test.local", "password", "wrong-pass"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        postJson("/api/auth/register", Map.of("email", "bad", "password", "short", "fullName", ""), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        String customer = registerCustomer();
        var body = Map.of("name", "Poetry", "slug", "poetry");
        postJson("/api/admin/categories", body, null).andExpect(status().isUnauthorized());
        postJson("/api/admin/categories", body, customer).andExpect(status().isForbidden());
        postJson("/api/admin/categories", body, adminToken()).andExpect(status().isCreated());
    }

    @Test
    void customersCannotSeeEachOthersOrders() throws Exception {
        String alice = registerCustomer();
        long book = createProduct("Privacy Author", 2, 1, "50.00");
        postJson("/api/cart/items", Map.of("productId", book, "quantity", 1), alice);
        long orderId = readLong(postJson("/api/orders", Map.of("addressId", addAddress(alice)), alice), "$.id");

        String bob = registerCustomer();
        getJson("/api/orders/" + orderId, bob).andExpect(status().isNotFound());
        postJson("/api/orders/" + orderId + "/cancel", null, bob).andExpect(status().isNotFound());
        postJson("/api/orders/" + orderId + "/payments", Map.of("method", "CASH_ON_DELIVERY"), bob)
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiDocsArePublished() throws Exception {
        getJson("/v3/api-docs", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("E-Bookstore API"))
                .andExpect(jsonPath("$.paths['/api/orders/{orderId}/cancel']").exists());
    }
}
