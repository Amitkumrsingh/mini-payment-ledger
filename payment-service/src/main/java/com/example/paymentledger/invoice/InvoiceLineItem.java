package com.example.paymentledger.invoice;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="invoice_line_items")
public class InvoiceLineItem {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invoice_id") private Invoice invoice;
    @Column(nullable=false,length=255) private String description;
    @Column(nullable=false) private int quantity;
    @Column(name="unit_price_in_cents",nullable=false) private long unitPriceInCents;
    protected InvoiceLineItem(){}
    InvoiceLineItem(Invoice invoice,String description,int quantity,long unitPrice){id=UUID.randomUUID();this.invoice=invoice;this.description=description;this.quantity=quantity;unitPriceInCents=unitPrice;}
    public long lineTotal(){return Math.multiplyExact(quantity,unitPriceInCents);}
    public UUID getId(){return id;} public String getDescription(){return description;} public int getQuantity(){return quantity;} public long getUnitPriceInCents(){return unitPriceInCents;}
}

