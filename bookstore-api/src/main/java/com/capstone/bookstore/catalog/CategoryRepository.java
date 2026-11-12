package com.capstone.bookstore.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCaseOrSlugIgnoreCase(String name, String slug);
}
