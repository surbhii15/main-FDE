package com.capstone.bookstore.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.ClockTestConfig.class)
public abstract class IntegrationTestBase {

    public static final Instant START = Instant.parse("2026-11-10T06:00:00Z");

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START);
        }
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.set(START);
    }

    // ---------- HTTP helpers ----------

    protected ResultActions getJson(String url, String token) throws Exception {
        return mvc.perform(auth(get(url), token));
    }

    protected ResultActions postJson(String url, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder b = post(url);
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(auth(b, token));
    }

    protected ResultActions putJson(String url, Object body, String token) throws Exception {
        return mvc.perform(auth(put(url).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), token));
    }

    protected ResultActions deleteJson(String url, String token) throws Exception {
        return mvc.perform(auth(delete(url), token));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static <T> T read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    protected static long readLong(ResultActions result, String path) throws Exception {
        return ((Number) read(result, path)).longValue();
    }

    // ---------- Domain helpers ----------

    protected String registerCustomer() throws Exception {
        String email = "user-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        var res = postJson("/api/auth/register",
                Map.of("email", email, "password", "Secret@123", "fullName", "Test Customer"), null);
        return read(res, "$.accessToken");
    }

    protected String login(String email, String password) throws Exception {
        return read(postJson("/api/auth/login", Map.of("email", email, "password", password), null),
                "$.accessToken");
    }

    protected String adminToken() throws Exception {
        return login("admin@bookstore.local", "Admin@123");
    }

    /** Creates a fresh product so stock assertions are not affected by other tests. */
    protected long createProduct(String author, int stock, int deliveryDays, String price) throws Exception {
        String sku = "T-" + UUID.randomUUID().toString().substring(0, 8);
        var res = postJson("/api/admin/products", Map.of("sku", sku, "title", "Book " + sku, "author", author,
                "price", price, "stockQuantity", stock, "deliveryDays", deliveryDays,
                "categoryId", 3, "brandId", 3), adminToken());
        return readLong(res, "$.id");
    }

    protected long addAddress(String token) throws Exception {
        var res = postJson("/api/addresses", Map.of("recipientName", "Test Customer", "line1", "1 Test Street",
                "city", "Pune", "postalCode", "411001", "country", "India"), token);
        return readLong(res, "$.id");
    }

    protected Map<String, Object> card(String number) {
        return Map.of("number", number, "holderName", "Test Customer", "expiryMonth", 12, "expiryYear", 2030,
                "cvv", "123");
    }
}
