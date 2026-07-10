package com.example.paymentledger.account;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByCode(String code);
    @Query(value="SELECT COALESCE(SUM(CASE WHEN entry_type='DEBIT' THEN amount_in_cents ELSE 0 END),0), COALESCE(SUM(CASE WHEN entry_type='CREDIT' THEN amount_in_cents ELSE 0 END),0) FROM journal_entries WHERE account_id=:id", nativeQuery=true)
    Object[] balance(@Param("id") UUID id);
}

