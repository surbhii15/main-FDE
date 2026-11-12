package com.capstone.bookstore.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "payments"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /** Loads items (for summaries) of a page of orders in one query. */
    @EntityGraph(attributePaths = {"items"})
    @Query("select o from Order o where o.id in :ids")
    List<Order> findWithItemsByIdIn(@Param("ids") List<Long> ids);

    boolean existsByOrderNumber(String orderNumber);
}
