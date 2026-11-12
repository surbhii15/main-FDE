package com.capstone.bookstore.catalog;

import org.springframework.data.domain.Sort;

/** Whitelisted catalogue sort orders (keeps arbitrary column names out of the query string). */
public enum ProductSort {
    TITLE(Sort.by("title")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "price")),
    RATING(Sort.by(Sort.Order.desc("rating").nullsLast())),
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final Sort sort;

    ProductSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort.and(Sort.by("id"));
    }
}
