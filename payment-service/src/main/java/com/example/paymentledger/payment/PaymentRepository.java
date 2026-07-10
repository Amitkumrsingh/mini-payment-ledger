package com.example.paymentledger.payment;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment,UUID>{
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
    List<Payment> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
    @Query("select coalesce(sum(p.amountInCents),0) from Payment p where p.invoiceId=:invoiceId and p.status=com.example.paymentledger.payment.Payment.Status.SUCCEEDED") long successfulTotal(@Param("invoiceId")UUID invoiceId);
}
