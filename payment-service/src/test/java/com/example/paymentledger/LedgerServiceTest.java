package com.example.paymentledger;

import com.example.paymentledger.account.*;
import com.example.paymentledger.common.DomainException;
import com.example.paymentledger.ledger.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerServiceTest {
    private final LedgerTransactionRepository transactions=mock(LedgerTransactionRepository.class);
    private final AccountRepository accounts=mock(AccountRepository.class);
    private final LedgerService service=new LedgerService(transactions,accounts);
    private final Account debit=new Account("EXP","Expense",Account.Type.EXPENSE,"USD");
    private final Account credit=new Account("CASH","Cash",Account.Type.ASSET,"USD");
    @BeforeEach void setup(){when(accounts.findById(debit.getId())).thenReturn(Optional.of(debit));when(accounts.findById(credit.getId())).thenReturn(Optional.of(credit));when(transactions.save(any())).thenAnswer(i->i.getArgument(0));}
    @Test void balancedTransactionSucceeds(){var result=service.create("TEST",UUID.randomUUID(),"Balanced","USD",List.of(entry(debit,JournalEntry.Type.DEBIT,100),entry(credit,JournalEntry.Type.CREDIT,100)));assertEquals(2,result.getEntries().size());verify(transactions).save(any());}
    @Test void unbalancedTransactionIsRejected(){var error=assertThrows(DomainException.class,()->service.create("TEST",UUID.randomUUID(),"Bad","USD",List.of(entry(debit,JournalEntry.Type.DEBIT,100),entry(credit,JournalEntry.Type.CREDIT,90))));assertEquals("UNBALANCED_LEDGER_TRANSACTION",error.code());}
    @Test void nonPositiveEntryIsRejected(){assertThrows(DomainException.class,()->service.create("TEST",UUID.randomUUID(),"Bad","USD",List.of(entry(debit,JournalEntry.Type.DEBIT,0),entry(credit,JournalEntry.Type.CREDIT,0))));verifyNoInteractions(transactions);}
    private LedgerTransaction.AccountEntry entry(Account account,JournalEntry.Type type,long amount){return new LedgerTransaction.AccountEntry(account.getId(),type,amount,"USD");}
}

