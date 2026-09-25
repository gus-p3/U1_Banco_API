package com.intrumentoev.demo.model.contactDetail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDetailResponse {

    private Long idContactDetail;
    private Long idClient;
    private String email;
    private String mobilePhone;
    private String alternativePhone;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}