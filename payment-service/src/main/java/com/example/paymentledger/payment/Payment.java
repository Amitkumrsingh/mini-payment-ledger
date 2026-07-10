package com.example.paymentledger.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="payments")
public class Payment {
    public enum Status { PROCESSING, SUCCEEDED, FAILED }
    @Id private UUID id;
    @Column(name="external_payment_id",nullable=false,unique=true,length=120) private String externalPaymentId;
    @Column(name="invoice_id",nullable=false) private UUID invoiceId;
    @Column(name="amount_in_cents",nullable=false) private long amountInCents;
    @Column(nullable=false,length=3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="processed_at") private Instant processedAt;
    protected Payment(){}
    public Payment(String externalId,UUID invoiceId,long amount,String currency){id=UUID.randomUUID();externalPaymentId=externalId;this.invoiceId=invoiceId;amountInCents=amount;this.currency=currency;status=Status.SUCCEEDED;createdAt=processedAt=Instant.now();}
    public UUID getId(){return id;} public String getExternalPaymentId(){return externalPaymentId;} public UUID getInvoiceId(){return invoiceId;} public long getAmountInCents(){return amountInCents;} public String getCurrency(){return currency;} public Status getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getProcessedAt(){return processedAt;}
}

