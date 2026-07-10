package com.example.paymentledger.ledger;

import org.springframework.data.jpa.repository.*;
import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {
    long countByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
    @EntityGraph(attributePaths="entries") @Query("select t from LedgerTransaction t where t.id=:id") java.util.Optional<LedgerTransaction> findWithEntriesById(@org.springframework.data.repository.query.Param("id") UUID id);
}
