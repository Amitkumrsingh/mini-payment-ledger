package com.example.paymentledger.invoice;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InvoiceRepository extends JpaRepository<Invoice,UUID>{
    Optional<Invoice> findByInvoiceNumber(String number);
    Page<Invoice> findByStatus(Invoice.Status status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Invoice i where i.id=:id") Optional<Invoice> findByIdForUpdate(@Param("id")UUID id);
    @EntityGraph(attributePaths="lineItems") @Query("select i from Invoice i where i.id=:id") Optional<Invoice> findDetailedById(@Param("id")UUID id);
    @Query("select i from Invoice i where i.dueDate < :today and i.status in :statuses") List<Invoice> findOverdue(@Param("today")java.time.LocalDate today,@Param("statuses")Collection<Invoice.Status> statuses);
}

