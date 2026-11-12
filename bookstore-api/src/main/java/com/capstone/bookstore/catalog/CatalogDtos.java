package com.capstone.bookstore.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class CatalogDtos {

    private CatalogDtos() {}

    public record CategoryResponse(Long id, String name, String slug, String description) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription());
        }
    }

    public record BrandResponse(Long id, String name, String description) {
        public static BrandResponse from(Brand b) {
            return new BrandResponse(b.getId(), b.getName(), b.getDescription());
        }
    }

    public record Ref(Long id, String name) {}

    public record ProductResponse(Long id, String sku, String title, String author, String isbn, String description,
                                  BigDecimal price, int stockQuantity, boolean inStock, int deliveryDays,
                                  LocalDate estimatedDeliveryDate, BigDecimal rating, String imageUrl,
                                  boolean active, Ref category, Ref brand) {

        public static ProductResponse from(Product p, LocalDate today) {
            return new ProductResponse(p.getId(), p.getSku(), p.getTitle(), p.getAuthor(), p.getIsbn(),
                    p.getDescription(), p.getPrice(), p.getStockQuantity(), p.isPurchasable(), p.getDeliveryDays(),
                    p.estimatedDeliveryDate(today), p.getRating(), p.getImageUrl(), p.isActive(),
                    new Ref(p.getCategory().getId(), p.getCategory().getName()),
                    new Ref(p.getBrand().getId(), p.getBrand().getName()));
        }
    }

    /** Product page: the product plus related items to show alongside it. */
    public record ProductDetailResponse(ProductResponse product, java.util.List<ProductResponse> relatedProducts) {}

    public record ProductRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 150) String author,
            @Pattern(regexp = "^[0-9X-]{10,17}$", message = "must be a valid ISBN") String isbn,
            @Size(max = 4000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) Integer stockQuantity,
            @NotNull @Min(0) @Max(60) Integer deliveryDays,
            @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
            @Size(max = 500) String imageUrl,
            Boolean active,
            @NotNull Long categoryId,
            @NotNull Long brandId) {}

    public record CategoryRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,100}$") String slug,
                                  @Size(max = 500) String description) {}

    public record BrandRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
}
