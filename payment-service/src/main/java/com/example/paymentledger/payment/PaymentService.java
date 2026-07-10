package com.example.paymentledger.payment;

import com.example.paymentledger.account.*;
import com.example.paymentledger.common.DomainException;
import com.example.paymentledger.invoice.*;
import com.example.paymentledger.ledger.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PaymentService {
    private final PaymentRepository payments; private final InvoiceRepository invoices; private final AccountRepository accounts; private final LedgerService ledger;
    public PaymentService(PaymentRepository payments,InvoiceRepository invoices,AccountRepository accounts,LedgerService ledger){this.payments=payments;this.invoices=invoices;this.accounts=accounts;this.ledger=ledger;}
    @Transactional public Payment apply(String externalId,UUID invoiceId,long amount,String currency){
        Payment replay=payments.findByExternalPaymentId(externalId).orElse(null); if(replay!=null)return validateReplay(replay,invoiceId,amount,currency);
        Invoice invoice=invoices.findByIdForUpdate(invoiceId).orElseThrow(()->DomainException.notFound("INVOICE_NOT_FOUND","Invoice not found"));
        replay=payments.findByExternalPaymentId(externalId).orElse(null); if(replay!=null)return validateReplay(replay,invoiceId,amount,currency);
        if(amount<=0)throw DomainException.badRequest("INVALID_PAYMENT_AMOUNT","Payment amount must be positive");
        if(invoice.getStatus()==Invoice.Status.DRAFT)throw DomainException.conflict("INVOICE_NOT_PAYABLE","Draft invoices cannot accept payments");
        long paid=payments.successfulTotal(invoiceId),outstanding=invoice.total()-paid;
        if(invoice.getStatus()==Invoice.Status.PAID||outstanding==0)throw DomainException.conflict("INVOICE_ALREADY_PAID","Invoice is already paid");
        if(!invoice.getCurrency().equals(currency))throw DomainException.badRequest("PAYMENT_CURRENCY_MISMATCH","Payment currency must match invoice currency");
        if(amount>outstanding)throw DomainException.conflict("INVOICE_OVERPAYMENT","Payment amount exceeds the outstanding invoice amount");
        Payment payment=payments.save(new Payment(externalId,invoiceId,amount,currency));
        Account ap=systemAccount("AP-001"),cash=systemAccount("CASH-001");
        ledger.create("PAYMENT",payment.getId(),"Payment applied to invoice "+invoice.getInvoiceNumber(),currency,List.of(new LedgerTransaction.AccountEntry(ap.getId(),JournalEntry.Type.DEBIT,amount,currency),new LedgerTransaction.AccountEntry(cash.getId(),JournalEntry.Type.CREDIT,amount,currency)));
        long remaining=outstanding-amount; invoice.mark(remaining==0?Invoice.Status.PAID:Invoice.Status.PARTIALLY_PAID);
        return payment;
    }
    private Account systemAccount(String code){return accounts.findByCode(code).orElseThrow(()->new IllegalStateException("Missing system account "+code));}
    private Payment validateReplay(Payment p,UUID invoiceId,long amount,String currency){if(!p.getInvoiceId().equals(invoiceId)||p.getAmountInCents()!=amount||!p.getCurrency().equals(currency))throw DomainException.conflict("IDEMPOTENCY_CONFLICT","External payment ID was already used with a different payload");return p;}
    @Transactional(readOnly=true) public List<Payment> list(int limit){return payments.findAll(PageRequest.of(0,Math.max(1,Math.min(limit,200)),Sort.by(Sort.Direction.DESC,"createdAt"))).getContent();}
    @Transactional(readOnly=true) public Payment get(UUID id){return payments.findById(id).orElseThrow(()->DomainException.notFound("PAYMENT_NOT_FOUND","Payment not found"));}
}

