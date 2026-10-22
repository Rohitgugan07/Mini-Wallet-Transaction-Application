package com.example.wallet.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST),
    SAME_WALLET_TRANSFER(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) { this.status = status; }

    public HttpStatus getStatus() { return status; }
}
