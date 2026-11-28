package com.intrumentoev.demo.model.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationAmountRangeResponse {
    private Short idOperationAmount;
    private String label;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean isActive;
}
