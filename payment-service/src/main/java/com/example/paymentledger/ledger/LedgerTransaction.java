package com.example.paymentledger.ledger;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="ledger_transactions")
public class LedgerTransaction {
    @Id private UUID id;
    @Column(name="reference_type",nullable=false,length=40) private String referenceType;
    @Column(name="reference_id",nullable=false) private UUID referenceId;
    @Column(nullable=false,length=255) private String description;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @OneToMany(mappedBy="transaction",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @OrderBy("id") private List<JournalEntry> entries=new ArrayList<>();
    protected LedgerTransaction(){}
    public LedgerTransaction(String referenceType,UUID referenceId,String description,String currency){this.id=UUID.randomUUID();this.referenceType=referenceType;this.referenceId=referenceId;this.description=description;this.currency=currency;this.createdAt=Instant.now();}
    public void addEntry(AccountEntry entry){entries.add(new JournalEntry(this,entry.accountId(),entry.type(),entry.amountInCents(),entry.currency()));}
    public UUID getId(){return id;} public String getReferenceType(){return referenceType;} public UUID getReferenceId(){return referenceId;} public String getDescription(){return description;} public String getCurrency(){return currency;} public Instant getCreatedAt(){return createdAt;} public List<JournalEntry> getEntries(){return List.copyOf(entries);}
    public record AccountEntry(UUID accountId,JournalEntry.Type type,long amountInCents,String currency){}
}

