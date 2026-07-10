import { z } from 'zod';
const long=z.union([z.string().regex(/^\d+$/),z.number().int().nonnegative()]).transform(String);
const currency=z.literal('USD'); const text=(max:number)=>z.string().trim().min(1).max(max);
export const createAccount=z.object({code:text(40),name:text(120),type:z.enum(['ASSET','LIABILITY','EXPENSE','REVENUE','EQUITY']),currency});
export const journalEntry=z.object({accountId:z.string().uuid(),entryType:z.enum(['DEBIT','CREDIT']),amountInCents:long.refine(v=>BigInt(v)>0n),currency});
export const createLedger=z.object({referenceType:text(40),referenceId:z.string().uuid(),description:text(255),currency,entries:z.array(journalEntry).min(2)});
export const createInvoice=z.object({invoiceNumber:text(60),vendorName:text(160),vendorReference:text(100),currency,issueDate:z.string().date(),dueDate:z.string().date(),lineItems:z.array(z.object({description:text(255),quantity:z.number().int().positive(),unitPriceInCents:long})).min(1)}).refine(v=>v.dueDate>=v.issueDate,{message:'Due date cannot be before issue date',path:['dueDate']});
export const applyPayment=z.object({externalPaymentId:text(120),invoiceId:z.string().uuid(),amountInCents:long.refine(v=>BigInt(v)>0n),currency});

