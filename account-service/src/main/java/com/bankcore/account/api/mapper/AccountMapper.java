package com.bankcore.account.api.mapper;

import com.bankcore.account.api.dto.AccountResponse;
import com.bankcore.account.domain.Account;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);

    List<AccountResponse> toResponseList(List<Account> accounts);
}