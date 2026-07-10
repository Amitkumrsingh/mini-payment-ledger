package com.example.paymentledger.invoice;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity @Table(name="invoices")
public class Invoice {
    public enum Status { DRAFT, SENT, PARTIALLY_PAID, PAID, OVERDUE }
    @Id private UUID id;
    @Column(name="invoice_number",nullable=false,unique=true,length=60) private String invoiceNumber;
    @Column(name="vendor_name",nullable=false,length=160) private String vendorName;
    @Column(name="vendor_reference",nullable=false,length=100) private String vendorReference;
    @Column(nullable=false,length=3) private String currency;
    @Column(name="issue_date",nullable=false) private LocalDate issueDate;
    @Column(name="due_date",nullable=false) private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Version private long version;
    @OneToMany(mappedBy="invoice",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id") private List<InvoiceLineItem> lineItems=new ArrayList<>();
    protected Invoice(){}
    public Invoice(String number,String vendor,String vendorReference,String currency,LocalDate issue,LocalDate due){id=UUID.randomUUID();invoiceNumber=number;vendorName=vendor;this.vendorReference=vendorReference;this.currency=currency;issueDate=issue;dueDate=due;status=Status.DRAFT;createdAt=updatedAt=Instant.now();}
    public void addLine(String description,int quantity,long unitPrice){lineItems.add(new InvoiceLineItem(this,description,quantity,unitPrice));touch();}
    public void send(){status=Status.SENT;touch();}
    public void mark(Status next){status=next;touch();}
    private void touch(){updatedAt=Instant.now();}
    public long total(){return lineItems.stream().mapToLong(InvoiceLineItem::lineTotal).reduce(0,Math::addExact);}
    public UUID getId(){return id;} public String getInvoiceNumber(){return invoiceNumber;} public String getVendorName(){return vendorName;} public String getVendorReference(){return vendorReference;} public String getCurrency(){return currency;} public LocalDate getIssueDate(){return issueDate;} public LocalDate getDueDate(){return dueDate;} public Status getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;} public List<InvoiceLineItem> getLineItems(){return List.copyOf(lineItems);}
}

