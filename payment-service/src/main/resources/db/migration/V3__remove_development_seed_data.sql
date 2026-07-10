DELETE FROM ledger_transactions
WHERE reference_type = 'PAYMENT'
  AND reference_id IN (
    SELECT id
    FROM payments
    WHERE external_payment_id IN ('seed-partial', 'seed-paid')
  );

DELETE FROM payments
WHERE external_payment_id IN ('seed-partial', 'seed-paid');

DELETE FROM invoices
WHERE invoice_number IN (
  'DEMO-DRAFT',
  'DEMO-SENT',
  'DEMO-PARTIAL',
  'DEMO-PAID',
  'DEMO-OVERDUE'
);

DELETE FROM accounts
WHERE code = 'EXP-001'
  AND NOT EXISTS (
    SELECT 1 FROM journal_entries WHERE account_id = accounts.id
  );
