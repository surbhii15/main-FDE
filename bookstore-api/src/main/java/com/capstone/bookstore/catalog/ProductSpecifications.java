package com.capstone.bookstore.catalog;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the product catalogue search. */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> activeOnly() {
        return (root, q, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Product> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> ofBrand(Long brandId) {
        return brandId == null ? null : (root, q, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> byAuthor(String author) {
        return isBlank(author) ? null
                : (root, q, cb) -> cb.equal(cb.lower(root.get("author")), author.trim().toLowerCase());
    }

    /** Free-text match on title, author or ISBN. */
    public static Specification<Product> matching(String text) {
        if (isBlank(text)) {
            return null;
        }
        String like = "%" + text.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like, '\\'),
                cb.like(cb.lower(root.get("author")), like, '\\'),
                cb.like(cb.lower(root.get("isbn")), like, '\\'));
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, q, cb) -> {
            if (min == null && max == null) {
                return null;
            }
            if (min == null) {
                return cb.lessThanOrEqualTo(root.get("price"), max);
            }
            if (max == null) {
                return cb.greaterThanOrEqualTo(root.get("price"), min);
            }
            return cb.between(root.get("price"), min, max);
        };
    }

    public static Specification<Product> inStockOnly(Boolean inStock) {
        return Boolean.TRUE.equals(inStock) ? (root, q, cb) -> cb.greaterThan(root.get("stockQuantity"), 0) : null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
