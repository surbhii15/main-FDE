package com.capstone.bookstore.points;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiftPointTransactionRepository extends JpaRepository<GiftPointTransaction, Long> {

    List<GiftPointTransaction> findTop50ByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
