package com.example.paymentledger.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentView(UUID id,String externalPaymentId,UUID invoiceId,long amountInCents,String currency,Payment.Status status,Instant createdAt,Instant processedAt){
    public static PaymentView from(Payment p){return new PaymentView(p.getId(),p.getExternalPaymentId(),p.getInvoiceId(),p.getAmountInCents(),p.getCurrency(),p.getStatus(),p.getCreatedAt(),p.getProcessedAt());}
}
