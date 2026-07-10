package com.example.paymentledger.invoice;

import com.example.paymentledger.common.DomainException;
import com.example.paymentledger.payment.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class InvoiceService {
    private final InvoiceRepository invoices; private final PaymentRepository payments;
    public InvoiceService(InvoiceRepository invoices,PaymentRepository payments){this.invoices=invoices;this.payments=payments;}
    public record NewLine(String description,int quantity,long unitPriceInCents){}
    public record LineView(UUID id,String description,int quantity,long unitPriceInCents,long lineTotalInCents){}
    public record View(UUID id,String invoiceNumber,String vendorName,String vendorReference,String currency,LocalDate issueDate,LocalDate dueDate,Invoice.Status status,long subtotalInCents,long amountPaidInCents,long outstandingAmountInCents,Instant createdAt,Instant updatedAt,long version,List<LineView> lineItems,List<PaymentView> paymentHistory){}
    @Transactional public View create(String number,String vendor,String vendorReference,String currency,LocalDate issue,LocalDate due,List<NewLine> lines){
        if(!"USD".equals(currency)) throw DomainException.badRequest("UNSUPPORTED_CURRENCY","Only USD is supported");
        if(due.isBefore(issue)) throw DomainException.badRequest("INVALID_DUE_DATE","Due date cannot be before issue date");
        if(lines==null||lines.isEmpty()) throw DomainException.badRequest("INVOICE_LINE_ITEMS_REQUIRED","At least one line item is required");
        if(invoices.findByInvoiceNumber(number).isPresent()) throw DomainException.conflict("INVOICE_NUMBER_EXISTS","Invoice number already exists");
        var invoice=new Invoice(number,vendor,vendorReference,currency,issue,due);
        for(var line:lines){if(line.quantity()<=0||line.unitPriceInCents()<0)throw DomainException.badRequest("INVALID_LINE_ITEM","Quantity must be positive and unit price nonnegative"); invoice.addLine(line.description(),line.quantity(),line.unitPriceInCents());}
        invoice.total(); return view(invoices.save(invoice));
    }
    @Transactional public View send(UUID id){Invoice invoice=required(id);if(invoice.getStatus()!=Invoice.Status.DRAFT)throw DomainException.conflict("INVALID_INVOICE_TRANSITION","Only draft invoices can be sent");invoice.send();return view(invoice);}
    @Transactional public RefreshResult refreshOverdue(){
        var eligible=invoices.findOverdue(LocalDate.now(ZoneOffset.UTC),List.of(Invoice.Status.SENT,Invoice.Status.PARTIALLY_PAID));
        eligible.forEach(i->i.mark(Invoice.Status.OVERDUE)); return new RefreshResult(eligible.size(),eligible.stream().map(Invoice::getId).toList());
    }
    @Transactional(readOnly=true) public List<View> list(Invoice.Status status,int limit){Pageable page=PageRequest.of(0,Math.max(1,Math.min(limit,200)),Sort.by(Sort.Direction.DESC,"createdAt"));return (status==null?invoices.findAll(page):invoices.findByStatus(status,page)).stream().map(this::view).toList();}
    @Transactional(readOnly=true) public View get(UUID id){return view(required(id));}
    Invoice required(UUID id){return invoices.findDetailedById(id).orElseThrow(()->DomainException.notFound("INVOICE_NOT_FOUND","Invoice not found"));}
    public View view(Invoice i){long total=i.total(),paid=payments.successfulTotal(i.getId());return new View(i.getId(),i.getInvoiceNumber(),i.getVendorName(),i.getVendorReference(),i.getCurrency(),i.getIssueDate(),i.getDueDate(),i.getStatus(),total,paid,total-paid,i.getCreatedAt(),i.getUpdatedAt(),i.getVersion(),i.getLineItems().stream().map(l->new LineView(l.getId(),l.getDescription(),l.getQuantity(),l.getUnitPriceInCents(),l.lineTotal())).toList(),payments.findByInvoiceIdOrderByCreatedAtDesc(i.getId()).stream().map(PaymentView::from).toList());}
    public record RefreshResult(int updatedCount,List<UUID> invoiceIds){}
}
