package com.intrumentoev.demo.model.employment;

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
public class EmploymentInformationResponse {

    private Long idEmployment;
    private Long idClient;
    private String occupation;
    private String company;
    private BigDecimal monthlyIncome;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
