package com.capstone.bookstore.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findWithDetailsById(@Param("id") Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByIsbn(String isbn);

    /** Related products: same author ranked first, then same category; best rated first. */
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("""
            select p from Product p
            where p.active = true and p.id <> :productId
              and (p.author = :author or p.category.id = :categoryId)
            order by case when p.author = :author then 0 else 1 end, p.rating desc nulls last, p.id
            """)
    List<Product> findRelated(@Param("productId") Long productId, @Param("author") String author,
                              @Param("categoryId") Long categoryId, Pageable pageable);

    /** Recommendation candidates: active, in stock, matching the user's favourite categories or authors. */
    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("""
            select p from Product p
            where p.active = true and p.stockQuantity > 0
              and p.id not in :excludeIds
              and (p.category.id in :categoryIds or p.author in :authors)
            order by p.rating desc nulls last, p.id
            """)
    List<Product> findRecommendationCandidates(@Param("categoryIds") Collection<Long> categoryIds,
                                               @Param("authors") Collection<String> authors,
                                               @Param("excludeIds") Collection<Long> excludeIds,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("""
            select p from Product p
            where p.active = true and p.stockQuantity > 0 and p.id not in :excludeIds
            order by p.rating desc nulls last, p.id
            """)
    List<Product> findTopRated(@Param("excludeIds") Collection<Long> excludeIds, Pageable pageable);

    /**
     * Atomically reserves stock. Returns 1 if reserved, 0 if the product is inactive or has too little stock,
     * so concurrent checkouts can never oversell.
     */
    @Modifying
    @Query("""
            update Product p set p.stockQuantity = p.stockQuantity - :qty
            where p.id = :id and p.active = true and p.stockQuantity >= :qty
            """)
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :qty where p.id = :id")
    int releaseStock(@Param("id") Long id, @Param("qty") int qty);
}
