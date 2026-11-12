package com.capstone.bookstore.catalog;

import com.capstone.bookstore.catalog.CatalogDtos.BrandRequest;
import com.capstone.bookstore.catalog.CatalogDtos.BrandResponse;
import com.capstone.bookstore.catalog.CatalogDtos.CategoryRequest;
import com.capstone.bookstore.catalog.CatalogDtos.CategoryResponse;
import com.capstone.bookstore.catalog.CatalogDtos.ProductRequest;
import com.capstone.bookstore.catalog.CatalogDtos.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin — Catalogue", description = "Catalogue management (ADMIN role)")
public class AdminCatalogController {

    private final CatalogService catalog;

    public AdminCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return catalog.createProduct(req);
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update a product")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return catalog.updateProduct(id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a product from the catalogue (soft delete)")
    public void delete(@PathVariable Long id) {
        catalog.deactivateProduct(id);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return catalog.createCategory(req);
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a brand")
    public BrandResponse createBrand(@Valid @RequestBody BrandRequest req) {
        return catalog.createBrand(req);
    }
}
