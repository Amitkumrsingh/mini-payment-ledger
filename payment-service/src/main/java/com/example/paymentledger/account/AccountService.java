package com.example.paymentledger.account;

import com.example.paymentledger.common.DomainException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AccountService {
    private final AccountRepository repository;
    public AccountService(AccountRepository repository){this.repository=repository;}
    @Transactional public Account create(String code, String name, Account.Type type, String currency) {
        if (!"USD".equals(currency)) throw DomainException.badRequest("UNSUPPORTED_CURRENCY", "Only USD is supported");
        if (repository.findByCode(code).isPresent()) throw DomainException.conflict("ACCOUNT_CODE_EXISTS", "Account code already exists");
        return repository.save(new Account(code, name, type, currency));
    }
    @Transactional(readOnly=true) public List<Account> list(int limit){return repository.findAll(PageRequest.of(0, bounded(limit), Sort.by(Sort.Direction.ASC,"code"))).getContent();}
    @Transactional(readOnly=true) public Account get(UUID id){return repository.findById(id).orElseThrow(()->DomainException.notFound("ACCOUNT_NOT_FOUND","Account not found"));}
    @Transactional(readOnly=true) public Balance balance(UUID id) {
        Account account=get(id); Object[] raw=repository.balance(id); Object[] row = raw.length == 1 && raw[0] instanceof Object[] nested ? nested : raw;
        long debits=((Number)row[0]).longValue(), credits=((Number)row[1]).longValue();
        // Assets and expenses normally carry debit balances; liabilities, revenue and equity carry credit balances.
        long net = account.getType()==Account.Type.ASSET || account.getType()==Account.Type.EXPENSE ? debits-credits : credits-debits;
        return new Balance(account.getId(), debits, credits, net, account.getCurrency());
    }
    private int bounded(int limit){return Math.max(1,Math.min(limit,200));}
    public record Balance(UUID accountId,long totalDebitsInCents,long totalCreditsInCents,long netBalanceInCents,String currency){}
}

