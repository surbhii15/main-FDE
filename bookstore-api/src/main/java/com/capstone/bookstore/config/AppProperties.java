package com.capstone.bookstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Orders orders, Points points, Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Orders(int cancellationWindowHours) {}

    /** maxRedeemPercent: share of the subtotal points may cover; earnPerAmount: currency spent per point earned. */
    public record Points(int maxRedeemPercent, int earnPerAmount) {}

    public record Seed(boolean enabled, String adminEmail, String adminPassword,
                       String customerEmail, String customerPassword) {}
}
