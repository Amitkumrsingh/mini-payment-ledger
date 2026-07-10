CREATE TABLE accounts (
  id UUID PRIMARY KEY, code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL,
  type VARCHAR(20) NOT NULL CHECK (type IN ('ASSET','LIABILITY','EXPENSE','REVENUE','EQUITY')),
  currency VARCHAR(3) NOT NULL CHECK (currency = 'USD'), created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE ledger_transactions (
  id UUID PRIMARY KEY, reference_type VARCHAR(40) NOT NULL, reference_id UUID NOT NULL,
  description VARCHAR(255) NOT NULL, currency VARCHAR(3) NOT NULL CHECK (currency = 'USD'), created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE journal_entries (
  id UUID PRIMARY KEY, transaction_id UUID NOT NULL REFERENCES ledger_transactions(id) ON DELETE CASCADE,
  account_id UUID NOT NULL REFERENCES accounts(id), entry_type VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
  amount_in_cents BIGINT NOT NULL CHECK (amount_in_cents > 0), currency VARCHAR(3) NOT NULL CHECK (currency = 'USD')
);
CREATE TABLE invoices (
  id UUID PRIMARY KEY, invoice_number VARCHAR(60) NOT NULL UNIQUE, vendor_name VARCHAR(160) NOT NULL,
  vendor_reference VARCHAR(100) NOT NULL, currency VARCHAR(3) NOT NULL CHECK (currency = 'USD'),
  issue_date DATE NOT NULL, due_date DATE NOT NULL CHECK (due_date >= issue_date),
  status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','SENT','PARTIALLY_PAID','PAID','OVERDUE')),
  created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE invoice_line_items (
  id UUID PRIMARY KEY, invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  description VARCHAR(255) NOT NULL, quantity INTEGER NOT NULL CHECK (quantity > 0),
  unit_price_in_cents BIGINT NOT NULL CHECK (unit_price_in_cents >= 0)
);
CREATE TABLE payments (
  id UUID PRIMARY KEY, external_payment_id VARCHAR(120) NOT NULL UNIQUE, invoice_id UUID NOT NULL REFERENCES invoices(id),
  amount_in_cents BIGINT NOT NULL CHECK (amount_in_cents > 0), currency VARCHAR(3) NOT NULL CHECK (currency = 'USD'),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING','SUCCEEDED','FAILED')),
  created_at TIMESTAMPTZ NOT NULL, processed_at TIMESTAMPTZ
);
CREATE INDEX idx_journal_entries_account ON journal_entries(account_id);
CREATE INDEX idx_ledger_transactions_created ON ledger_transactions(created_at DESC);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_status ON payments(status);

