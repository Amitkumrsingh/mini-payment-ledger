package com.example.paymentledger.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService service; public InvoiceController(InvoiceService service){this.service=service;}
    public record LineRequest(@NotBlank @Size(max=255)String description,@Positive int quantity,@PositiveOrZero long unitPriceInCents){}
    public record CreateRequest(@NotBlank @Size(max=60)String invoiceNumber,@NotBlank @Size(max=160)String vendorName,@NotBlank @Size(max=100)String vendorReference,@NotBlank String currency,@NotNull LocalDate issueDate,@NotNull LocalDate dueDate,@NotEmpty List<@Valid LineRequest> lineItems){}
    @PostMapping public InvoiceService.View create(@Valid @RequestBody CreateRequest r){return service.create(r.invoiceNumber(),r.vendorName(),r.vendorReference(),r.currency(),r.issueDate(),r.dueDate(),r.lineItems().stream().map(x->new InvoiceService.NewLine(x.description(),x.quantity(),x.unitPriceInCents())).toList());}
    @GetMapping public List<InvoiceService.View> list(@RequestParam(required=false)Invoice.Status status,@RequestParam(defaultValue="100")int limit){return service.list(status,limit);}
    @GetMapping("/{id}") public InvoiceService.View get(@PathVariable UUID id){return service.get(id);}
    @PostMapping("/{id}/send") public InvoiceService.View send(@PathVariable UUID id){return service.send(id);}
    @PostMapping("/refresh-overdue") public InvoiceService.RefreshResult refresh(){return service.refreshOverdue();}
}

