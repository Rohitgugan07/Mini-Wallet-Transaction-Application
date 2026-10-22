package com.example.wallet.service;

import com.example.wallet.dto.*;
import com.example.wallet.entity.*;
import com.example.wallet.exception.*;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletResponse createWallet(CreateWalletRequest request) {
        String email = request.email().trim().toLowerCase();
        if (walletRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        BigDecimal initial = request.initialBalance() == null
                ? BigDecimal.ZERO.setScale(2)
                : request.initialBalance().setScale(2);
        try {
            // saveAndFlush so the unique constraint is checked inside this method
            // (covers the race between existsByEmail and the insert).
            Wallet wallet = walletRepository.saveAndFlush(new Wallet(request.name().trim(), email, initial));
            return toResponse(wallet);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getAllWallets() {
        return walletRepository.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositResponse deposit(Long walletId, DepositRequest request) {
        BigDecimal amount = requirePositive(request.amount());

        Wallet wallet = lockWallet(walletId);
        BigDecimal previous = wallet.getBalance();
        wallet.credit(amount);
        walletRepository.save(wallet);

        transactionRepository.save(new WalletTransaction(
                wallet, TransactionType.DEPOSIT, amount, TransactionStatus.SUCCESS, newReference()));

        return new DepositResponse(wallet.getId(), previous, amount, wallet.getBalance());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transfer(TransferRequest request) {
        Long fromId = request.fromWalletId();
        Long toId = request.toWalletId();
        BigDecimal amount = requirePositive(request.amount());

        if (fromId.equals(toId)) {
            throw new SameWalletTransferException();
        }

        // Always lock in ascending id order so two opposite transfers (A->B and B->A)
        // can never deadlock waiting on each other.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Wallet first = lockWallet(firstId);
        Wallet second = lockWallet(secondId);
        Wallet from = fromId.equals(firstId) ? first : second;
        Wallet to = toId.equals(firstId) ? first : second;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        from.debit(amount);
        to.credit(amount);
        walletRepository.save(from);
        walletRepository.save(to);

        // Both history rows share one reference id.
        String reference = newReference();
        WalletTransaction out = transactionRepository.save(new WalletTransaction(
                from, TransactionType.TRANSFER_OUT, amount, TransactionStatus.SUCCESS, reference));
        transactionRepository.save(new WalletTransaction(
                to, TransactionType.TRANSFER_IN, amount, TransactionStatus.SUCCESS, reference));

        return new TransferResponse(out.getId(), from.getId(), to.getId(), amount,
                TransactionStatus.SUCCESS.name());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long walletId) {
        return toResponse(walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long walletId) {
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException(walletId);
        }
        return transactionRepository.findByWalletIdOrderByCreatedAtDescIdDesc(walletId).stream()
                .map(t -> new TransactionResponse(t.getId(), t.getType().name(), t.getAmount(),
                        t.getStatus().name(), t.getCreatedAt()))
                .toList();
    }

    // ---- helpers ----

    private Wallet lockWallet(Long id) {
        return walletRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }

    private BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new InvalidAmountException("Amount must have at most 2 decimal places");
        }
        return amount.setScale(2);
    }

    private String newReference() { return UUID.randomUUID().toString(); }

    private WalletResponse toResponse(Wallet w) {
        return new WalletResponse(w.getId(), w.getName(), w.getEmail(), w.getBalance());
    }
}
