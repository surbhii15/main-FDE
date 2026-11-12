package com.capstone.bookstore.catalog;

import com.capstone.bookstore.catalog.CatalogDtos.BrandResponse;
import com.capstone.bookstore.catalog.CatalogDtos.CategoryResponse;
import com.capstone.bookstore.catalog.CatalogDtos.ProductDetailResponse;
import com.capstone.bookstore.catalog.CatalogDtos.ProductResponse;
import com.capstone.bookstore.catalog.CatalogService.ProductFilter;
import com.capstone.bookstore.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
@SecurityRequirements
@Tag(name = "Catalogue", description = "Categories, brands, product search, product page and related products "
        + "(journey steps 3, 5, 6, 7) — public")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    @Operation(summary = "List product categories")
    public List<CategoryResponse> categories() {
        return catalog.listCategories();
    }

    @GetMapping("/brands")
    @Operation(summary = "List brands (publishers)")
    public List<BrandResponse> brands() {
        return catalog.listBrands();
    }

    @GetMapping("/products")
    @Operation(summary = "Browse / search the catalogue",
            description = "Filter by category, brand, author, free text (title/author/ISBN), price range and stock.")
    public PageResponse<ProductResponse> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String author,
            @Parameter(description = "Free text matched against title, author and ISBN")
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "TITLE") ProductSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return catalog.search(new ProductFilter(categoryId, brandId, author, q, minPrice, maxPrice, inStock),
                sort, page, size);
    }

    @GetMapping("/categories/{categoryId}/products")
    @Operation(summary = "Product catalogue for one category")
    public PageResponse<ProductResponse> productsInCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "TITLE") ProductSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return catalog.search(new ProductFilter(categoryId, brandId, null, null, null, null, null), sort, page, size);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Product page: details, tentative delivery date and related products")
    public ProductDetailResponse product(@PathVariable Long id) {
        return catalog.getProduct(id);
    }

    @GetMapping("/products/{id}/related")
    @Operation(summary = "Related products (same author first, then same category)")
    public List<ProductResponse> related(@PathVariable Long id) {
        return catalog.getRelated(id);
    }
}
