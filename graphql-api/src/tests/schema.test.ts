import { graphql } from 'graphql';
import { jest } from '@jest/globals';
import { schema } from '../graphql/schema.js';
import { BackendError } from '../errors/backendError.js';

const correlationId='test-correlation';
function context(client:any){return {client,correlationId}}
describe('GraphQL BFF',()=>{
  test('rejects invalid account input before calling Spring',async()=>{const client={post:jest.fn()};const result=await graphql({schema,source:'mutation($input:CreateAccountInput!){createAccount(input:$input){id}}',variableValues:{input:{code:'',name:'Expense',type:'EXPENSE',currency:'USD'}},contextValue:context(client)});expect(result.errors?.[0].extensions.code).toBe('BAD_USER_INPUT');expect(client.post).not.toHaveBeenCalled()});
  test('maps resolver input and forwards correlation ID',async()=>{const client={post:jest.fn().mockResolvedValue({id:'1',code:'EXP-1',name:'Expense',type:'EXPENSE',currency:'USD',createdAt:'2026-01-01T00:00:00Z'})};const result=await graphql({schema,source:'mutation($input:CreateAccountInput!){createAccount(input:$input){code}}',variableValues:{input:{code:'EXP-1',name:'Expense',type:'EXPENSE',currency:'USD'}},contextValue:context(client)});expect(result.errors).toBeUndefined();expect(client.post).toHaveBeenCalledWith('/api/accounts',expect.objectContaining({code:'EXP-1'}),correlationId)});
  test('maps backend errors without leaking internals',async()=>{const client={post:jest.fn().mockRejectedValue(new BackendError(409,{code:'INVOICE_OVERPAYMENT',message:'Too much',correlationId}))};const result=await graphql({schema,source:'mutation($input:ApplyPaymentInput!){applyPayment(input:$input){id}}',variableValues:{input:{externalPaymentId:'pay-1',invoiceId:'7481de8a-7104-4e8f-8a5d-16cff9197cdd',amountInCents:'7000',currency:'USD'}},contextValue:context(client)});expect(result.errors?.[0].extensions).toMatchObject({code:'INVOICE_OVERPAYMENT',httpStatus:409,correlationId})});
  test('Long serializes as a string',async()=>{const client={get:jest.fn().mockResolvedValue({accountId:'1',totalDebitsInCents:5000000000,totalCreditsInCents:0,netBalanceInCents:5000000000,currency:'USD'})};const result=await graphql({schema,source:'{accountBalance(accountId:"1"){totalDebitsInCents}}',contextValue:context(client)});expect((result.data?.accountBalance as {totalDebitsInCents:string}).totalDebitsInCents).toBe('5000000000')});
});
