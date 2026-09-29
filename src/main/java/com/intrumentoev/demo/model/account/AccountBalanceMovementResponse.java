package com.intrumentoev.demo.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceMovementResponse {

    private Long idBalance;
    private Long idAccount;
    private String accountNumber;
    private BigDecimal previousBalance;
    private BigDecimal amount;
    private BigDecimal currentBalance;
    private String movementType;
    private String description;
    private OffsetDateTime createdAt;
}
