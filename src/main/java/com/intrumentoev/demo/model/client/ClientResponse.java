package com.intrumentoev.demo.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private Long idClient;
    private String name;
    private String secondName;
    private String lastName;
    private String secondLastName;
    private LocalDate birthDate;
    private String curp;
    private String rfc;
    private Short idGender;
    private String genderName;
    private Short idNationality;
    private String nationalityName;
    private Short idMaritalStatus;
    private String maritalStatusName;
    private Boolean isActive;
    private OffsetDateTime deactivatedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}