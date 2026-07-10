package com.example.paymentledger.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service){this.service=service;}
    public record CreateRequest(@NotBlank @Size(max=40) String code,@NotBlank @Size(max=120) String name,@NotNull Account.Type type,@NotBlank String currency){}
    public record Response(UUID id,String code,String name,Account.Type type,String currency,Instant createdAt) { static Response from(Account a){return new Response(a.getId(),a.getCode(),a.getName(),a.getType(),a.getCurrency(),a.getCreatedAt());}}
    @PostMapping public Response create(@Valid @RequestBody CreateRequest request){return Response.from(service.create(request.code(),request.name(),request.type(),request.currency()));}
    @GetMapping public List<Response> list(@RequestParam(defaultValue="100") int limit){return service.list(limit).stream().map(Response::from).toList();}
    @GetMapping("/{id}") public Response get(@PathVariable UUID id){return Response.from(service.get(id));}
    @GetMapping("/{id}/balance") public AccountService.Balance balance(@PathVariable UUID id){return service.balance(id);}
}
