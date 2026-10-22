package com.example.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(Long transactionId, String type, BigDecimal amount,
                                  String status, LocalDateTime createdAt) { }
