package com.intrumentoev.demo.model.account;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

    private Long idAccount;
    private String accountNumber;
    private Long idClient;
    private BigDecimal balance;
    private String status;
    private OffsetDateTime openedAt;
    private OffsetDateTime updatedAt;
}
