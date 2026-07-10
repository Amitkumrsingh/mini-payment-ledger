import {gql} from '@apollo/client';
export const INVOICES=gql`query Invoices($status:InvoiceStatus){invoices(status:$status){id invoiceNumber vendorName dueDate status subtotalInCents amountPaidInCents outstandingAmountInCents}}`;
export const INVOICE=gql`query Invoice($id:ID!){invoice(id:$id){id invoiceNumber vendorName vendorReference issueDate dueDate status currency subtotalInCents amountPaidInCents outstandingAmountInCents lineItems{id description quantity unitPriceInCents lineTotalInCents} paymentHistory{id externalPaymentId amountInCents status processedAt}}}`;
export const ACCOUNTS=gql`query Accounts{accounts{id code name type currency} }`;
export const BALANCE=gql`query Balance($id:ID!){accountBalance(accountId:$id){totalDebitsInCents totalCreditsInCents netBalanceInCents}}`;
export const LEDGER=gql`query Ledger{ledgerTransactions{id createdAt description referenceType referenceId debitTotalInCents creditTotalInCents balanced entries{id accountId entryType amountInCents}}}`;
export const PAYMENTS=gql`query Payments{payments(limit:8){id externalPaymentId amountInCents status processedAt invoice{id invoiceNumber vendorName}}}`;
export const CREATE_ACCOUNT=gql`mutation CreateAccount($input:CreateAccountInput!){createAccount(input:$input){id code name type currency}}`;
export const CREATE_INVOICE=gql`mutation CreateInvoice($input:CreateInvoiceInput!){createInvoice(input:$input){id invoiceNumber}}`;
export const SEND_INVOICE=gql`mutation Send($id:ID!){sendInvoice(invoiceId:$id){id status}}`;
export const APPLY_PAYMENT=gql`mutation Pay($input:ApplyPaymentInput!){applyPayment(input:$input){id externalPaymentId amountInCents status invoice{id status amountPaidInCents outstandingAmountInCents}}}`;
export const REFRESH_OVERDUE=gql`mutation Refresh{refreshOverdueInvoices{updatedCount invoiceIds}}`;

