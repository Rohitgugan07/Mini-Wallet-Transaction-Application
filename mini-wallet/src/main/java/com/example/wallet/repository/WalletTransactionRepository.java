package com.example.wallet.repository;

import com.example.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /** Newest first; id is a tie-breaker for entries created in the same instant. */
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDescIdDesc(Long walletId);
}
