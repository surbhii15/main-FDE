package com.capstone.bookstore.catalog;

import static com.capstone.bookstore.catalog.ProductSpecifications.activeOnly;
import static com.capstone.bookstore.catalog.ProductSpecifications.byAuthor;
import static com.capstone.bookstore.catalog.ProductSpecifications.inCategory;
import static com.capstone.bookstore.catalog.ProductSpecifications.inStockOnly;
import static com.capstone.bookstore.catalog.ProductSpecifications.matching;
import static com.capstone.bookstore.catalog.ProductSpecifications.ofBrand;
import static com.capstone.bookstore.catalog.ProductSpecifications.priceBetween;

import com.capstone.bookstore.catalog.CatalogDtos.BrandRequest;
import com.capstone.bookstore.catalog.CatalogDtos.BrandResponse;
import com.capstone.bookstore.catalog.CatalogDtos.CategoryRequest;
import com.capstone.bookstore.catalog.CatalogDtos.CategoryResponse;
import com.capstone.bookstore.catalog.CatalogDtos.ProductDetailResponse;
import com.capstone.bookstore.catalog.CatalogDtos.ProductRequest;
import com.capstone.bookstore.catalog.CatalogDtos.ProductResponse;
import com.capstone.bookstore.common.BusinessRuleException;
import com.capstone.bookstore.common.NotFoundException;
import com.capstone.bookstore.common.PageResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    public record ProductFilter(Long categoryId, Long brandId, String author, String q,
                                BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {}

    private static final int RELATED_LIMIT = 6;

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;
    private final Clock clock;

    public CatalogService(CategoryRepository categories, BrandRepository brands, ProductRepository products,
                          Clock clock) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
        this.clock = clock;
    }

    public List<CategoryResponse> listCategories() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    public List<BrandResponse> listBrands() {
        return brands.findAll(Sort.by("name")).stream().map(BrandResponse::from).toList();
    }

    public PageResponse<ProductResponse> search(ProductFilter f, ProductSort sort, int page, int size) {
        if (f.minPrice() != null && f.maxPrice() != null && f.minPrice().compareTo(f.maxPrice()) > 0) {
            throw new BusinessRuleException("INVALID_PRICE_RANGE", "minPrice must not exceed maxPrice");
        }
        Specification<Product> spec = Specification.allOf(activeOnly(), inCategory(f.categoryId()),
                ofBrand(f.brandId()), byAuthor(f.author()), matching(f.q()),
                priceBetween(f.minPrice(), f.maxPrice()), inStockOnly(f.inStock()));
        LocalDate today = today();
        var result = products.findAll(spec, PageRequest.of(page, size, sort.toSort()));
        return PageResponse.of(result, p -> ProductResponse.from(p, today));
    }

    public ProductDetailResponse getProduct(Long id) {
        Product p = products.findWithDetailsById(id).filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Product", id));
        return new ProductDetailResponse(ProductResponse.from(p, today()), related(p));
    }

    public List<ProductResponse> getRelated(Long id) {
        Product p = products.findWithDetailsById(id).filter(Product::isActive)
                .orElseThrow(() -> new NotFoundException("Product", id));
        return related(p);
    }

    private List<ProductResponse> related(Product p) {
        LocalDate today = today();
        return products.findRelated(p.getId(), p.getAuthor(), p.getCategory().getId(),
                        PageRequest.of(0, RELATED_LIMIT))
                .stream().map(r -> ProductResponse.from(r, today)).toList();
    }

    // ---------- Admin ----------

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        if (products.existsBySkuIgnoreCase(req.sku())) {
            throw BusinessRuleException.conflict("SKU_TAKEN", "SKU " + req.sku() + " already exists");
        }
        if (req.isbn() != null && products.existsByIsbn(req.isbn())) {
            throw BusinessRuleException.conflict("ISBN_TAKEN", "ISBN " + req.isbn() + " already exists");
        }
        Product p = new Product();
        p.setCreatedAt(clock.instant());
        apply(p, req);
        return ProductResponse.from(products.save(p), today());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product p = products.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Product", id));
        if (!p.getSku().equalsIgnoreCase(req.sku()) && products.existsBySkuIgnoreCase(req.sku())) {
            throw BusinessRuleException.conflict("SKU_TAKEN", "SKU " + req.sku() + " already exists");
        }
        if (req.isbn() != null && !Objects.equals(p.getIsbn(), req.isbn()) && products.existsByIsbn(req.isbn())) {
            throw BusinessRuleException.conflict("ISBN_TAKEN", "ISBN " + req.isbn() + " already exists");
        }
        apply(p, req);
        return ProductResponse.from(p, today());
    }

    /** Soft delete: the product disappears from the catalogue but order history keeps referencing it. */
    @Transactional
    public void deactivateProduct(Long id) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
        p.setActive(false);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categories.existsByNameIgnoreCaseOrSlugIgnoreCase(req.name(), req.slug())) {
            throw BusinessRuleException.conflict("CATEGORY_EXISTS", "Category name or slug already exists");
        }
        Category c = new Category();
        c.setName(req.name().trim());
        c.setSlug(req.slug());
        c.setDescription(req.description());
        return CategoryResponse.from(categories.save(c));
    }

    @Transactional
    public BrandResponse createBrand(BrandRequest req) {
        if (brands.existsByNameIgnoreCase(req.name())) {
            throw BusinessRuleException.conflict("BRAND_EXISTS", "Brand already exists");
        }
        Brand b = new Brand();
        b.setName(req.name().trim());
        b.setDescription(req.description());
        return BrandResponse.from(brands.save(b));
    }

    private void apply(Product p, ProductRequest req) {
        p.setSku(req.sku().trim());
        p.setTitle(req.title().trim());
        p.setAuthor(req.author().trim());
        p.setIsbn(req.isbn());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setStockQuantity(req.stockQuantity());
        p.setDeliveryDays(req.deliveryDays());
        p.setRating(req.rating());
        p.setImageUrl(req.imageUrl());
        p.setActive(req.active() == null || req.active());
        p.setCategory(categories.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Category", req.categoryId())));
        p.setBrand(brands.findById(req.brandId())
                .orElseThrow(() -> new NotFoundException("Brand", req.brandId())));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
