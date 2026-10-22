package com.example.wallet.exception;

public class DuplicateEmailException extends WalletException {
    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, "A wallet already exists for this email");
    }
}
