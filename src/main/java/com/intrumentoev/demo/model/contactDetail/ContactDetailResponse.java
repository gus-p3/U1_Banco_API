package com.intrumentoev.demo.model.contactDetail;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactDetailResponse {

    private Long idContactDetail;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Long idClient;
    private String email;
    private String mobilePhone;
    private String alternativePhone;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}