package com.example.wallet.exception;

public class InsufficientBalanceException extends WalletException {
    public InsufficientBalanceException() {
        super(ErrorCode.INSUFFICIENT_BALANCE, "Insufficient balance to complete the transfer");
    }
}
