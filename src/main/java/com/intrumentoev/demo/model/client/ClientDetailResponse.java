package com.intrumentoev.demo.model.client;

import com.intrumentoev.demo.model.account.AccountResponse;
import com.intrumentoev.demo.model.auth.AuthResponse;
import com.intrumentoev.demo.model.contactDetail.ContactDetailResponse;
import com.intrumentoev.demo.model.employment.EmploymentInformationResponse;
import com.intrumentoev.demo.model.home.HomeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDetailResponse {

    private ClientResponse client;
    private ContactDetailResponse contactDetail;
    private HomeResponse home;
    private EmploymentInformationResponse employmentInformation;
    private AccountResponse primaryAccount;
    private List<AccountResponse> accounts;
    private AuthResponse auth;
    private ClientModuleCatalogsResponse catalogs;
}
