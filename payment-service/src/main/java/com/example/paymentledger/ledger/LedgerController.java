package com.example.paymentledger.ledger;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/ledger-transactions")
public class LedgerController {
    private final LedgerService service; public LedgerController(LedgerService service){this.service=service;}
    public record EntryRequest(@NotNull UUID accountId,@NotNull JournalEntry.Type entryType,@Positive long amountInCents,@NotBlank String currency){}
    public record CreateRequest(@NotBlank @Size(max=40) String referenceType,@NotNull UUID referenceId,@NotBlank @Size(max=255) String description,@NotBlank String currency,@Size(min=2) List<@Valid EntryRequest> entries){}
    public record EntryResponse(UUID id,UUID accountId,JournalEntry.Type entryType,long amountInCents,String currency){}
    public record Response(UUID id,String referenceType,UUID referenceId,String description,String currency,Instant createdAt,List<EntryResponse> entries){
        static Response from(LedgerTransaction t){return new Response(t.getId(),t.getReferenceType(),t.getReferenceId(),t.getDescription(),t.getCurrency(),t.getCreatedAt(),t.getEntries().stream().map(e->new EntryResponse(e.getId(),e.getAccountId(),e.getEntryType(),e.getAmountInCents(),e.getCurrency())).toList());}
    }
    @PostMapping public Response create(@Valid @RequestBody CreateRequest r){return Response.from(service.create(r.referenceType(),r.referenceId(),r.description(),r.currency(),r.entries().stream().map(e->new LedgerTransaction.AccountEntry(e.accountId(),e.entryType(),e.amountInCents(),e.currency())).toList()));}
    @GetMapping public List<Response> list(@RequestParam(defaultValue="100")int limit){return service.list(limit).stream().map(Response::from).toList();}
    @GetMapping("/{id}") public Response get(@PathVariable UUID id){return Response.from(service.get(id));}
}
