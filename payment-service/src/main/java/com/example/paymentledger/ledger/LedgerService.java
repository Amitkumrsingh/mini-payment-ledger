package com.example.paymentledger.ledger;

import com.example.paymentledger.account.*;
import com.example.paymentledger.common.DomainException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class LedgerService {
    private final LedgerTransactionRepository repository; private final AccountRepository accounts;
    public LedgerService(LedgerTransactionRepository repository,AccountRepository accounts){this.repository=repository;this.accounts=accounts;}
    @Transactional public LedgerTransaction create(String referenceType,UUID referenceId,String description,String currency,List<LedgerTransaction.AccountEntry> entries){
        if(entries==null||entries.size()<2) throw DomainException.badRequest("LEDGER_MINIMUM_ENTRIES","A transaction requires at least two entries");
        if(!"USD".equals(currency)) throw DomainException.badRequest("UNSUPPORTED_CURRENCY","Only USD is supported");
        long debits=0,credits=0;
        for(var entry:entries){
            if(entry.amountInCents()<=0) throw DomainException.badRequest("INVALID_ENTRY_AMOUNT","Journal entry amounts must be positive");
            if(!currency.equals(entry.currency())) throw DomainException.badRequest("LEDGER_CURRENCY_MISMATCH","All entries must use the transaction currency");
            Account account=accounts.findById(entry.accountId()).orElseThrow(()->DomainException.notFound("ACCOUNT_NOT_FOUND","Account not found"));
            if(!currency.equals(account.getCurrency())) throw DomainException.badRequest("ACCOUNT_CURRENCY_MISMATCH","Account currency does not match transaction currency");
            if(entry.type()==JournalEntry.Type.DEBIT) debits=Math.addExact(debits,entry.amountInCents()); else credits=Math.addExact(credits,entry.amountInCents());
        }
        if(debits!=credits) throw DomainException.badRequest("UNBALANCED_LEDGER_TRANSACTION","Total debits must equal total credits");
        var transaction=new LedgerTransaction(referenceType,referenceId,description,currency); entries.forEach(transaction::addEntry); return repository.save(transaction);
    }
    @Transactional(readOnly=true) public List<LedgerTransaction> list(int limit){return repository.findAll(PageRequest.of(0,Math.max(1,Math.min(limit,200)),Sort.by(Sort.Direction.DESC,"createdAt"))).getContent().stream().map(t->repository.findWithEntriesById(t.getId()).orElseThrow()).toList();}
    @Transactional(readOnly=true) public LedgerTransaction get(UUID id){return repository.findWithEntriesById(id).orElseThrow(()->DomainException.notFound("LEDGER_TRANSACTION_NOT_FOUND","Ledger transaction not found"));}
}

