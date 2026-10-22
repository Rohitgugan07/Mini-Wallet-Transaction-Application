package com.example.wallet.exception;

public class WalletNotFoundException extends WalletException {
    public WalletNotFoundException(Long walletId) {
        super(ErrorCode.WALLET_NOT_FOUND, "Wallet not found with id " + walletId);
    }
}
