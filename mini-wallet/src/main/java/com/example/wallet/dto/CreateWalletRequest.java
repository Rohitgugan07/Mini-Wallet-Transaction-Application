package com.example.wallet.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateWalletRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @PositiveOrZero(message = "initialBalance cannot be negative")
        @Digits(integer = 17, fraction = 2, message = "initialBalance must have at most 2 decimal places")
        BigDecimal initialBalance
) { }
