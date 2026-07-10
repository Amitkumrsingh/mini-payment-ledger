package com.example.paymentledger.ledger;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="journal_entries")
public class JournalEntry {
    public enum Type { DEBIT, CREDIT }
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="transaction_id") private LedgerTransaction transaction;
    @Column(name="account_id",nullable=false) private UUID accountId;
    @Enumerated(EnumType.STRING) @Column(name="entry_type",nullable=false) private Type entryType;
    @Column(name="amount_in_cents",nullable=false) private long amountInCents;
    @Column(nullable=false,length=3) private String currency;
    protected JournalEntry(){}
    JournalEntry(LedgerTransaction transaction,UUID accountId,Type type,long amount,String currency){this.id=UUID.randomUUID();this.transaction=transaction;this.accountId=accountId;this.entryType=type;this.amountInCents=amount;this.currency=currency;}
    public UUID getId(){return id;} public UUID getAccountId(){return accountId;} public Type getEntryType(){return entryType;} public long getAmountInCents(){return amountInCents;} public String getCurrency(){return currency;}
}

