package com.intrumentoev.demo.repository.account;

import com.intrumentoev.demo.entity.account.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    List<AccountBalance> findByIdAccountOrderByCreatedAtDesc(Long idAccount);

    List<AccountBalance> findByIdAccountOrderByCreatedAtAsc(Long idAccount);
}
