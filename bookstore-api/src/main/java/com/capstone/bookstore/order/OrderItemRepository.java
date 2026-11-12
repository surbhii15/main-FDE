package com.capstone.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Order-history aggregates used for recommendations. Cancelled orders are ignored. */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select distinct oi.product.id from OrderItem oi
            where oi.order.user.id = :userId
              and oi.order.status <> com.capstone.bookstore.order.OrderStatus.CANCELLED
            """)
    List<Long> findPurchasedProductIds(@Param("userId") Long userId);

    @Query("""
            select oi.product.category.id, oi.product.category.name, sum(oi.quantity) from OrderItem oi
            where oi.order.user.id = :userId
              and oi.order.status <> com.capstone.bookstore.order.OrderStatus.CANCELLED
            group by oi.product.category.id, oi.product.category.name
            order by sum(oi.quantity) desc
            """)
    List<Object[]> findTopCategories(@Param("userId") Long userId);

    @Query("""
            select oi.product.author, sum(oi.quantity) from OrderItem oi
            where oi.order.user.id = :userId
              and oi.order.status <> com.capstone.bookstore.order.OrderStatus.CANCELLED
            group by oi.product.author
            order by sum(oi.quantity) desc
            """)
    List<Object[]> findTopAuthors(@Param("userId") Long userId);
}
