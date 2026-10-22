package com.example.wallet.service;

import com.example.wallet.dto.*;

import java.util.List;

public interface WalletService {
    List<WalletResponse> getAllWallets();
    WalletResponse createWallet(CreateWalletRequest request);
    DepositResponse deposit(Long walletId, DepositRequest request);
    TransferResponse transfer(TransferRequest request);
    WalletResponse getWallet(Long walletId);
    List<TransactionResponse> getTransactions(Long walletId);
}
