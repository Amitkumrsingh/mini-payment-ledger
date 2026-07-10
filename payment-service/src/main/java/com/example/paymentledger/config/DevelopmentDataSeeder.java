package com.example.paymentledger.config;

import com.example.paymentledger.account.*;
import com.example.paymentledger.invoice.*;
import com.example.paymentledger.payment.PaymentService;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component @ConditionalOnProperty(name="app.seed-data-enabled",havingValue="true")
public class DevelopmentDataSeeder implements ApplicationRunner {
    private final AccountRepository accounts; private final AccountService accountService; private final InvoiceRepository invoiceRepository; private final InvoiceService invoiceService; private final PaymentService paymentService;
    public DevelopmentDataSeeder(AccountRepository accounts,AccountService accountService,InvoiceRepository invoiceRepository,InvoiceService invoiceService,PaymentService paymentService){this.accounts=accounts;this.accountService=accountService;this.invoiceRepository=invoiceRepository;this.invoiceService=invoiceService;this.paymentService=paymentService;}
    @Override public void run(ApplicationArguments args){
        if(accounts.findByCode("EXP-001").isEmpty())accountService.create("EXP-001","Freight Expense",Account.Type.EXPENSE,"USD");
        create("DEMO-DRAFT","Acme Logistics",LocalDate.now().plusDays(20),false,0,"seed-draft");
        create("DEMO-SENT","Northern Freight",LocalDate.now().plusDays(10),true,0,"seed-sent");
        create("DEMO-PARTIAL","Roadrunner Transport",LocalDate.now().plusDays(5),true,4000,"seed-partial");
        create("DEMO-PAID","Blue Line Haulage",LocalDate.now().plusDays(5),true,10000,"seed-paid");
        create("DEMO-OVERDUE","Old Town Carriers",LocalDate.now().minusDays(2),true,0,"seed-overdue");
        invoiceService.refreshOverdue();
    }
    private void create(String number,String vendor,LocalDate due,boolean send,long payment,String external){
        if(invoiceRepository.findByInvoiceNumber(number).isPresent())return;
        var view=invoiceService.create(number,vendor,"REF-"+number,"USD",LocalDate.now().minusDays(10),due,List.of(new InvoiceService.NewLine("Freight service",2,5000)));
        if(send)view=invoiceService.send(view.id()); if(payment>0)paymentService.apply(external,view.id(),payment,"USD");
    }
}
