package com.example.paymentledger;

import com.example.paymentledger.account.*;
import com.example.paymentledger.common.DomainException;
import com.example.paymentledger.invoice.*;
import com.example.paymentledger.ledger.LedgerTransactionRepository;
import com.example.paymentledger.payment.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Testcontainers
class PaymentIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);r.add("app.seed-data-enabled",()->"false");}
    @Autowired InvoiceService invoices; @Autowired PaymentService payments; @Autowired PaymentRepository paymentRepository; @Autowired LedgerTransactionRepository ledger; @Autowired AccountService accounts;
    @Test void invoiceTotalsAndIdempotentPaymentsAreCorrect(){var invoice=sent("IDEM-"+UUID.randomUUID());assertEquals(10000,invoice.subtotalInCents());var first=payments.apply("ext-"+UUID.randomUUID(),invoice.id(),4000,"USD");var replay=payments.apply(first.getExternalPaymentId(),invoice.id(),4000,"USD");assertEquals(first.getId(),replay.getId());assertEquals(4000,invoices.get(invoice.id()).amountPaidInCents());assertThrows(DomainException.class,()->payments.apply(first.getExternalPaymentId(),invoice.id(),3000,"USD"));}
    @Test void concurrentPaymentsCannotOverpay()throws Exception{var invoice=sent("CONCURRENT-"+UUID.randomUUID());String a="a-"+UUID.randomUUID(),b="b-"+UUID.randomUUID();var start=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);Callable<Boolean> first=()->attempt(start,a,invoice.id()),second=()->attempt(start,b,invoice.id());Future<Boolean> one=pool.submit(first),two=pool.submit(second);start.countDown();int successes=(one.get()?1:0)+(two.get()?1:0);pool.shutdown();assertEquals(1,successes);assertEquals(7000,invoices.get(invoice.id()).amountPaidInCents());assertEquals(1,paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoice.id()).size());assertEquals(1,ledger.countByReferenceTypeAndReferenceId("PAYMENT",paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(invoice.id()).getFirst().getId()));}
    @Test void accountBalanceIsDerivedFromEntries(){var invoice=sent("BAL-"+UUID.randomUUID());payments.apply("bal-"+UUID.randomUUID(),invoice.id(),2500,"USD");var ap=accounts.list(20).stream().filter(x->x.getCode().equals("AP-001")).findFirst().orElseThrow();var balance=accounts.balance(ap.getId());assertTrue(balance.totalDebitsInCents()>=2500);}
    @Test void draftOverpaymentAndPaidInvoiceAreRejected(){var draft=invoices.create("RULES-"+UUID.randomUUID(),"Test Carrier","REF","USD",LocalDate.now(),LocalDate.now().plusDays(2),List.of(new InvoiceService.NewLine("Freight",1,10000)));assertEquals("INVOICE_NOT_PAYABLE",assertThrows(DomainException.class,()->payments.apply("draft-"+UUID.randomUUID(),draft.id(),1000,"USD")).code());var sent=invoices.send(draft.id());assertEquals("INVOICE_OVERPAYMENT",assertThrows(DomainException.class,()->payments.apply("over-"+UUID.randomUUID(),sent.id(),10001,"USD")).code());payments.apply("full-"+UUID.randomUUID(),sent.id(),10000,"USD");assertEquals(Invoice.Status.PAID,invoices.get(sent.id()).status());assertEquals("INVOICE_ALREADY_PAID",assertThrows(DomainException.class,()->payments.apply("again-"+UUID.randomUUID(),sent.id(),1,"USD")).code());}
    private InvoiceService.View sent(String number){var draft=invoices.create(number,"Test Carrier","REF","USD",LocalDate.now(),LocalDate.now().plusDays(10),List.of(new InvoiceService.NewLine("Freight",1,10000)));return invoices.send(draft.id());}
    private boolean attempt(CountDownLatch start,String externalId,UUID invoiceId)throws InterruptedException{start.await();try{payments.apply(externalId,invoiceId,7000,"USD");return true;}catch(DomainException e){assertEquals("INVOICE_OVERPAYMENT",e.code());return false;}}
}
