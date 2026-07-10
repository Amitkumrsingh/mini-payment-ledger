package com.example.paymentledger.payment;

import com.example.paymentledger.invoice.InvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class PaymentController {
    private final PaymentService service; private final InvoiceService invoices; public PaymentController(PaymentService service,InvoiceService invoices){this.service=service;this.invoices=invoices;}
    public record ApplyRequest(@NotBlank @Size(max=120)String externalPaymentId,@NotNull UUID invoiceId,@Positive long amountInCents,@NotBlank String currency){}
    public record Response(PaymentView payment,InvoiceService.View invoice){}
    @PostMapping("/invoices/{invoiceId}/payments") public Response apply(@PathVariable UUID invoiceId,@Valid @RequestBody ApplyRequest r){if(!invoiceId.equals(r.invoiceId()))throw com.example.paymentledger.common.DomainException.badRequest("INVOICE_ID_MISMATCH","Path and payload invoice IDs differ");Payment p=service.apply(r.externalPaymentId(),invoiceId,r.amountInCents(),r.currency());return new Response(PaymentView.from(p),invoices.get(invoiceId));}
    @GetMapping("/payments") public List<PaymentView> list(@RequestParam(defaultValue="100")int limit){return service.list(limit).stream().map(PaymentView::from).toList();}
    @GetMapping("/payments/{id}") public Response get(@PathVariable UUID id){Payment p=service.get(id);return new Response(PaymentView.from(p),invoices.get(p.getInvoiceId()));}
}
