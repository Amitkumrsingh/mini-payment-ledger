package com.example.paymentledger.account;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="accounts")
public class Account {
    public enum Type { ASSET, LIABILITY, EXPENSE, REVENUE, EQUITY }
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=40) private String code;
    @Column(nullable=false, length=120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Type type;
    @Column(nullable=false, length=3) private String currency;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    protected Account() {}
    public Account(String code, String name, Type type, String currency) { this.id=UUID.randomUUID(); this.code=code; this.name=name; this.type=type; this.currency=currency; this.createdAt=Instant.now(); }
    public UUID getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public Type getType(){return type;} public String getCurrency(){return currency;} public Instant getCreatedAt(){return createdAt;}
}

