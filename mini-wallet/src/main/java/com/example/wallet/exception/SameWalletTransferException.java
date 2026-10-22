package com.example.wallet.exception;

public class SameWalletTransferException extends WalletException {
    public SameWalletTransferException() {
        super(ErrorCode.SAME_WALLET_TRANSFER, "Sender and receiver wallets must be different");
    }
}
