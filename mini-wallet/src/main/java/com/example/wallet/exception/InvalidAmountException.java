package com.example.wallet.exception;

public class InvalidAmountException extends WalletException {
    public InvalidAmountException(String message) {
        super(ErrorCode.INVALID_AMOUNT, message);
    }
}
