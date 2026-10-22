package com.example.wallet.controller;

import com.example.wallet.dto.*;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public List<WalletResponse> getAll() {
        return walletService.getAllWallets();
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse created = walletService.createWallet(request);
        return ResponseEntity.created(URI.create("/api/wallets/" + created.walletId())).body(created);
    }

    @PostMapping("/{walletId}/deposit")
    public DepositResponse deposit(@PathVariable Long walletId, @Valid @RequestBody DepositRequest request) {
        return walletService.deposit(walletId, request);
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return walletService.transfer(request);
    }

    @GetMapping("/{walletId}")
    public WalletResponse get(@PathVariable Long walletId) {
        return walletService.getWallet(walletId);
    }

    @GetMapping("/{walletId}/transactions")
    public List<TransactionResponse> transactions(@PathVariable Long walletId) {
        return walletService.getTransactions(walletId);
    }
}
