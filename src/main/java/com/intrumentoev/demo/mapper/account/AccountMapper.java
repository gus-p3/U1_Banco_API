package com.intrumentoev.demo.mapper.account;

import com.intrumentoev.demo.entity.account.Account;
import com.intrumentoev.demo.model.account.AccountBalanceResponse;
import com.intrumentoev.demo.model.account.AccountResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account entity);

    AccountBalanceResponse toBalanceResponse(Account entity);

    List<AccountResponse> toResponseList(List<Account> entities);
}
