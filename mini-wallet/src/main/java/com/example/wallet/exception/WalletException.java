package com.example.wallet.exception;

/** Base class for expected business failures. Unchecked => triggers @Transactional rollback. */
public class WalletException extends RuntimeException {
    private final ErrorCode errorCode;

    public WalletException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
