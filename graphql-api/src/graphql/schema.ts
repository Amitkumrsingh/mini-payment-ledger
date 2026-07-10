import { GraphQLError,GraphQLScalarType,Kind } from 'graphql';
import { makeExecutableSchema } from '@graphql-tools/schema';
import { applyPayment,createAccount,createInvoice,createLedger } from '../validation/schemas.js';
import { ZodError } from 'zod';
import { toGraphQLError } from '../errors/backendError.js';
import type { PaymentServiceClient } from '../clients/paymentServiceClient.js';

export interface Context { client:PaymentServiceClient; correlationId:string }
const typeDefs=`#graphql
scalar Long
enum Currency { USD } enum AccountType { ASSET LIABILITY EXPENSE REVENUE EQUITY } enum EntryType { DEBIT CREDIT }
enum InvoiceStatus { DRAFT SENT PARTIALLY_PAID PAID OVERDUE } enum PaymentStatus { PROCESSING SUCCEEDED FAILED }
type Account { id:ID! code:String! name:String! type:AccountType! currency:Currency! createdAt:String! }
type AccountBalance { accountId:ID! totalDebitsInCents:Long! totalCreditsInCents:Long! netBalanceInCents:Long! currency:Currency! }
type JournalEntry { id:ID! accountId:ID! entryType:EntryType! amountInCents:Long! currency:Currency! }
type LedgerTransaction { id:ID! referenceType:String! referenceId:ID! description:String! currency:Currency! createdAt:String! entries:[JournalEntry!]! debitTotalInCents:Long! creditTotalInCents:Long! balanced:Boolean! }
type InvoiceLineItem { id:ID! description:String! quantity:Int! unitPriceInCents:Long! lineTotalInCents:Long! }
type Payment { id:ID! externalPaymentId:String! invoiceId:ID! amountInCents:Long! currency:Currency! status:PaymentStatus! createdAt:String! processedAt:String invoice:Invoice }
type Invoice { id:ID! invoiceNumber:String! vendorName:String! vendorReference:String! currency:Currency! issueDate:String! dueDate:String! status:InvoiceStatus! subtotalInCents:Long! amountPaidInCents:Long! outstandingAmountInCents:Long! createdAt:String! updatedAt:String! version:Long! lineItems:[InvoiceLineItem!]! paymentHistory:[Payment!]! }
type OverdueRefreshResult { updatedCount:Int! invoiceIds:[ID!]! }
input CreateAccountInput { code:String! name:String! type:AccountType! currency:Currency!=USD }
input JournalEntryInput { accountId:ID! entryType:EntryType! amountInCents:Long! currency:Currency!=USD }
input CreateLedgerTransactionInput { referenceType:String! referenceId:ID! description:String! currency:Currency!=USD entries:[JournalEntryInput!]! }
input InvoiceLineItemInput { description:String! quantity:Int! unitPriceInCents:Long! }
input CreateInvoiceInput { invoiceNumber:String! vendorName:String! vendorReference:String! currency:Currency!=USD issueDate:String! dueDate:String! lineItems:[InvoiceLineItemInput!]! }
input ApplyPaymentInput { externalPaymentId:String! invoiceId:ID! amountInCents:Long! currency:Currency!=USD }
type Query { accounts(limit:Int=100):[Account!]! account(id:ID!):Account accountBalance(accountId:ID!):AccountBalance! ledgerTransactions(limit:Int=100):[LedgerTransaction!]! ledgerTransaction(id:ID!):LedgerTransaction invoices(status:InvoiceStatus,limit:Int=100):[Invoice!]! invoice(id:ID!):Invoice payments(limit:Int=100):[Payment!]! payment(id:ID!):Payment }
type Mutation { createAccount(input:CreateAccountInput!):Account! createLedgerTransaction(input:CreateLedgerTransactionInput!):LedgerTransaction! createInvoice(input:CreateInvoiceInput!):Invoice! sendInvoice(invoiceId:ID!):Invoice! applyPayment(input:ApplyPaymentInput!):Payment! refreshOverdueInvoices:OverdueRefreshResult! }
`;
const LongScalar=new GraphQLScalarType({name:'Long',description:'Signed 64-bit integer serialized as a decimal string',serialize:v=>String(v),parseValue:v=>{if(typeof v!=='string'&&typeof v!=='number')throw new TypeError('Long must be a decimal value');return String(v)},parseLiteral:ast=>{if(ast.kind!==Kind.INT&&ast.kind!==Kind.STRING)throw new TypeError('Long must be a decimal value');return ast.value}});
const safe=(fn:(...args:any[])=>Promise<any>)=>async(...args:any[])=>{try{return await fn(...args)}catch(error){if(error instanceof ZodError)throw new GraphQLError(error.issues[0]?.message??'Invalid input',{extensions:{code:'BAD_USER_INPUT',httpStatus:400,correlationId:args[2].correlationId}});throw toGraphQLError(error,args[2].correlationId)}};
const query=(path:(a:any)=>string)=>safe((_:any,a:any,c:Context)=>c.client.get(path(a),c.correlationId));
const post=(path:(a:any)=>string,body:(a:any)=>unknown)=>safe((_:any,a:any,c:Context)=>c.client.post(path(a),body(a),c.correlationId));
const total=(entries:any[],type:string)=>entries.filter(e=>e.entryType===type).reduce((sum,e)=>sum+BigInt(e.amountInCents),0n).toString();
export const schema=makeExecutableSchema({typeDefs,resolvers:{Long:LongScalar,Query:{accounts:query(a=>`/api/accounts?limit=${a.limit}`),account:query(a=>`/api/accounts/${a.id}`),accountBalance:query(a=>`/api/accounts/${a.accountId}/balance`),ledgerTransactions:query(a=>`/api/ledger-transactions?limit=${a.limit}`),ledgerTransaction:query(a=>`/api/ledger-transactions/${a.id}`),invoices:query(a=>`/api/invoices?limit=${a.limit}${a.status?`&status=${a.status}`:''}`),invoice:query(a=>`/api/invoices/${a.id}`),payments:query(a=>`/api/payments?limit=${a.limit}`),payment:safe(async(_:any,a:any,c:Context)=>{const r=await c.client.get(`/api/payments/${a.id}`,c.correlationId);return {...r.payment,invoice:r.invoice}})},Mutation:{createAccount:post(()=>'/api/accounts',a=>createAccount.parse(a.input)),createLedgerTransaction:post(()=>'/api/ledger-transactions',a=>createLedger.parse(a.input)),createInvoice:post(()=>'/api/invoices',a=>createInvoice.parse(a.input)),sendInvoice:post(a=>`/api/invoices/${a.invoiceId}/send`,()=>({})),applyPayment:safe(async(_:any,a:any,c:Context)=>{const input=applyPayment.parse(a.input);const r=await c.client.post(`/api/invoices/${input.invoiceId}/payments`,input,c.correlationId);return {...r.payment,invoice:r.invoice}}),refreshOverdueInvoices:post(()=>'/api/invoices/refresh-overdue',()=>({}))},LedgerTransaction:{debitTotalInCents:(t:any)=>total(t.entries,'DEBIT'),creditTotalInCents:(t:any)=>total(t.entries,'CREDIT'),balanced:(t:any)=>total(t.entries,'DEBIT')===total(t.entries,'CREDIT')},Payment:{invoice:(p:any,_:any,c:Context)=>p.invoice??c.client.get(`/api/invoices/${p.invoiceId}`,c.correlationId)},Invoice:{paymentHistory:(i:any)=>i.paymentHistory??[]}}});
