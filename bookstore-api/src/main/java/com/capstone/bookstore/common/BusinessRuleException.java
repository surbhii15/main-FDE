package com.capstone.bookstore.common;

import org.springframework.http.HttpStatus;

/** A well-formed request that violates a business rule: 422 by default, 409 for state conflicts. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    private BusinessRuleException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static BusinessRuleException conflict(String code, String message) {
        return new BusinessRuleException(HttpStatus.CONFLICT, code, message);
    }
}
