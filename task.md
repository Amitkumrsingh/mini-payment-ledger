You are a Staff Full-Stack Engineer with strong experience in FinTech, payment ledgers, accounting systems, GraphQL, Node.js, Spring Boot, React, PostgreSQL, Docker, automated testing, and cloud deployment.

Build a complete, production-quality skill-test project named:

# Mini Payment Ledger & Invoice Service

The system represents an Accounts Payable payment-processing module for a Transportation Management System.

The final application must:

* Contain working backend and frontend code.
* Be runnable locally.
* Be deployable using free or low-cost cloud services.
* Include Docker support.
* Include automated tests.
* Include sample data.
* Include clear API documentation.
* Include a professional README.
* Be suitable for submission through a public GitHub repository.
* Avoid unfinished placeholders, fake implementations, and unexplained TODO comments.

Prioritize correctness, clean architecture, transaction safety, financial accuracy, idempotency, and understandable code over excessive features.

---

# 1. Mandatory Technology Stack

Use only the following primary technologies.

## Frontend

* React
* TypeScript
* Vite
* HTML
* CSS
* React Router
* Apollo Client for GraphQL
* React Hook Form
* Zod for client-side validation
* Vitest and React Testing Library

Use plain CSS or CSS Modules. Do not use a large UI framework unless necessary.

## GraphQL API Layer

* Node.js
* TypeScript
* Apollo Server
* GraphQL
* Express
* Zod
* Axios or native fetch for communicating with the Spring Boot service
* Jest for testing

The Node.js service acts as the public GraphQL API/BFF consumed by the React application.

## Core Domain Service

* Java 21
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Spring Validation
* PostgreSQL
* Flyway
* Spring Boot Actuator
* JUnit 5
* Mockito
* Testcontainers

The Spring Boot service owns:

* Accounts
* Ledger transactions
* Journal entries
* Invoices
* Payments
* Financial validations
* Database transactions
* Idempotency
* Concurrency control

## Infrastructure

* PostgreSQL
* Docker
* Docker Compose
* GitHub Actions

Do not use MongoDB or an in-memory database as the main production database.

---

# 2. Required Architecture

Use this architecture:

```text
React Frontend
      |
      | GraphQL
      v
Node.js Apollo GraphQL API
      |
      | Internal REST API
      v
Spring Boot Payment Service
      |
      v
PostgreSQL
```

Responsibilities must be separated clearly.

## React application

Responsible for:

* Pages and forms
* GraphQL queries and mutations
* Loading and error states
* Client-side validation
* User-friendly money and date formatting

## Node.js GraphQL service

Responsible for:

* Public GraphQL schema
* GraphQL queries and mutations
* Input validation
* Mapping GraphQL requests to Spring Boot REST APIs
* Mapping backend errors to safe GraphQL errors
* Correlation IDs
* Basic logging

Do not duplicate financial rules in Node.js. The Spring Boot service remains the source of truth.

## Spring Boot service

Responsible for:

* Domain rules
* Database transactions
* Double-entry ledger enforcement
* Invoice state transitions
* Idempotent payment handling
* Concurrency control
* Persistence
* Balance calculation
* Validation and exception handling

---

# 3. Core Functional Requirements

## Part 1: Core Ledger

Implement the following features.

### Create accounts

Each account must have:

* UUID
* Account code
* Account name
* Account type
* Currency
* Created timestamp

Supported account types:

* ASSET
* LIABILITY
* EXPENSE
* REVENUE
* EQUITY

For this assignment, support USD as the default currency.

Examples:

* Cash Account
* Accounts Payable
* Freight Expense
* Vendor Payable

### Record ledger transactions

Every financial transaction must use double-entry accounting.

A transaction must contain:

* Transaction UUID
* Reference type
* Reference ID
* Description
* Currency
* Created timestamp
* Journal entries

Each journal entry must contain:

* Account ID
* Entry type: DEBIT or CREDIT
* Amount in minor units
* Currency

Financial rules:

1. Every transaction must have at least two journal entries.
2. Total debit amount must equal total credit amount.
3. Amount must be greater than zero.
4. Debit and credit entries must use the same currency.
5. Never use floating-point values for money.
6. Store USD amounts as integer cents using `long` or `BIGINT`.
7. A journal transaction and all its entries must be saved atomically.
8. A partially saved transaction must never be possible.

### Account balances

Do not store account balance as a mutable column.

Calculate balances from journal entries using aggregation.

For the assignment, expose:

* Total debits
* Total credits
* Net balance

Apply account-type-aware balance calculation:

* ASSET and EXPENSE: debits minus credits
* LIABILITY, REVENUE and EQUITY: credits minus debits

Include a comment in the code explaining this accounting behavior.

---

# 4. Invoice Flow

Implement invoice management.

## Invoice fields

Each invoice must contain:

* UUID
* Invoice number
* Vendor name
* Vendor reference
* Currency
* Issue date
* Due date
* Status
* Subtotal in cents
* Amount paid in cents
* Outstanding amount in cents
* Created timestamp
* Updated timestamp
* Version field for optimistic locking
* Line items

Do not maintain `amountPaid` or `outstandingAmount` as manually editable values.

They must be derived from invoice line items and successful payment records, either through database queries or safe calculated response fields.

## Invoice line-item fields

Each line item must contain:

* UUID
* Description
* Quantity
* Unit price in cents
* Line total in cents

Use an integer quantity for the assignment.

Calculate:

```text
line total = quantity × unit price
invoice total = sum of line totals
outstanding amount = invoice total - successful payments
```

Validate all calculations on the backend.

## Invoice statuses

Support:

* DRAFT
* SENT
* PARTIALLY_PAID
* PAID
* OVERDUE

Required transitions:

```text
DRAFT -> SENT
SENT -> PARTIALLY_PAID
SENT -> PAID
SENT -> OVERDUE
PARTIALLY_PAID -> PAID
PARTIALLY_PAID -> OVERDUE
OVERDUE -> PARTIALLY_PAID
OVERDUE -> PAID
```

Rules:

* A DRAFT invoice cannot accept payments.
* A PAID invoice cannot accept another payment.
* An invoice becomes PARTIALLY_PAID when successful payments are greater than zero but lower than the total.
* An invoice becomes PAID when total successful payments equal the invoice total.
* An invoice is OVERDUE when its due date is before the current date and it is not fully paid.
* Do not allow arbitrary status changes from the frontend.

Implement an endpoint or scheduled method that updates eligible invoices to OVERDUE. For this assignment, also expose a manually callable operation so the functionality can be demonstrated easily.

---

# 5. Payment Processing

Implement applying a payment to an invoice.

Each payment must contain:

* UUID
* External payment ID
* Invoice ID
* Amount in cents
* Currency
* Payment status
* Created timestamp
* Processed timestamp

Supported payment statuses:

* PROCESSING
* SUCCEEDED
* FAILED

Payment request example:

```json
{
  "externalPaymentId": "pay_gateway_10001",
  "invoiceId": "invoice-uuid",
  "amountInCents": 5000,
  "currency": "USD"
}
```

Required validations:

1. Payment amount must be greater than zero.
2. Invoice must exist.
3. Invoice must not be DRAFT.
4. Invoice must not already be fully paid.
5. Payment currency must match invoice currency.
6. Payment must not exceed the current outstanding amount.
7. A duplicate external payment ID must not apply money twice.
8. Payment creation, ledger posting, and invoice result must succeed or fail atomically.

---

# 6. Idempotency and Duplicate Webhooks

Assume the payment provider can send the same webhook multiple times.

Make `external_payment_id` unique at the database level.

When the same external payment ID is submitted again:

* Do not create another payment.
* Do not create another ledger transaction.
* Do not modify the invoice twice.
* Return the previously processed payment result.
* Treat the request as a successful idempotent replay when the payload matches.

If the same external payment ID is reused with a different:

* Invoice ID
* Amount
* Currency

return a conflict error.

Use both:

* Application-level idempotency validation
* A unique database constraint

Do not rely only on an in-memory cache.

---

# 7. Edge-Case Challenge: Concurrent Payments

Implement the concurrent-payment edge case.

Scenario:

```text
Invoice total: $100
Outstanding amount: $100

Payment A: $70
Payment B: $70

Both requests arrive at nearly the same time.
```

Only one payment may succeed because accepting both would overpay the invoice.

Implement this using a database transaction and one of the following:

* PostgreSQL row-level pessimistic locking with `SELECT ... FOR UPDATE`
* JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`

Preferred implementation:

* Lock the invoice row before calculating the latest outstanding amount.
* Query successful payments inside the same transaction.
* Validate the new payment against the latest outstanding amount.
* Save the payment and ledger transaction in the same transaction.
* Commit only after all validations pass.

Add a concurrency integration test that launches two payment requests simultaneously and verifies:

* Only one succeeds.
* One is rejected due to overpayment.
* The invoice is not overpaid.
* Only one successful payment ledger entry is created.
* The double-entry ledger remains balanced.

Also keep the optimistic-lock version field on the invoice for additional safety and auditability.

---

# 8. Payment Ledger Entries

When an invoice is created or sent, do not automatically create a ledger transaction unless clearly documented.

When a payment succeeds, create a balanced ledger transaction.

For this simplified Accounts Payable system, use:

```text
Debit: Accounts Payable
Credit: Cash
```

Example for a $50 payment:

```text
Accounts Payable   DEBIT   5000 cents
Cash               CREDIT  5000 cents
```

The transaction reference must point to the payment.

Example:

```text
referenceType = "PAYMENT"
referenceId = payment UUID
description = "Payment applied to invoice INV-1001"
```

Create default system accounts using a Flyway migration or seed script:

* `AP-001` — Accounts Payable — LIABILITY
* `CASH-001` — Cash — ASSET

Do not hardcode database UUIDs inside business services. Look accounts up by stable account codes.

---

# 9. REST API Inside Spring Boot

Expose internal REST endpoints for the Node.js GraphQL service.

Suggested endpoints:

```text
POST   /api/accounts
GET    /api/accounts
GET    /api/accounts/{id}
GET    /api/accounts/{id}/balance

POST   /api/ledger-transactions
GET    /api/ledger-transactions
GET    /api/ledger-transactions/{id}

POST   /api/invoices
GET    /api/invoices
GET    /api/invoices/{id}
POST   /api/invoices/{id}/send
POST   /api/invoices/{id}/payments
POST   /api/invoices/refresh-overdue

GET    /api/payments
GET    /api/payments/{id}

GET    /actuator/health
```

Use request and response DTOs.

Do not return JPA entities directly from controllers.

Use a global exception handler based on `@RestControllerAdvice`.

Return consistent errors:

```json
{
  "code": "INVOICE_OVERPAYMENT",
  "message": "Payment amount exceeds the outstanding invoice amount",
  "correlationId": "request-correlation-id",
  "timestamp": "2026-07-10T12:00:00Z"
}
```

Appropriate status codes:

* 400 for validation problems
* 404 for missing resources
* 409 for duplicate idempotency conflicts, invalid transitions, concurrent updates, and overpayment
* 500 only for unexpected errors

---

# 10. GraphQL API

Build a clean GraphQL schema in the Node.js service.

## Scalar decisions

Represent money using integer minor units in the API.

Use fields such as:

```graphql
amountInCents: Int!
```

For values that may exceed GraphQL’s standard 32-bit `Int`, implement a custom `Long` scalar.

Use ISO-8601 strings or a DateTime scalar for dates.

## Required GraphQL types

Create types for:

* Account
* AccountBalance
* JournalEntry
* LedgerTransaction
* Invoice
* InvoiceLineItem
* Payment
* API result or mutation result where useful

## Required queries

```graphql
accounts
account(id: ID!)
accountBalance(accountId: ID!)

ledgerTransactions
ledgerTransaction(id: ID!)

invoices(status: InvoiceStatus)
invoice(id: ID!)

payments
payment(id: ID!)
```

## Required mutations

```graphql
createAccount(input: CreateAccountInput!): Account!

createLedgerTransaction(
  input: CreateLedgerTransactionInput!
): LedgerTransaction!

createInvoice(input: CreateInvoiceInput!): Invoice!

sendInvoice(invoiceId: ID!): Invoice!

applyPayment(input: ApplyPaymentInput!): Payment!

refreshOverdueInvoices: OverdueRefreshResult!
```

Validate GraphQL inputs before sending requests to Spring Boot.

Map Spring Boot domain errors to GraphQL errors with safe extension codes:

```json
{
  "extensions": {
    "code": "INVOICE_OVERPAYMENT",
    "httpStatus": 409,
    "correlationId": "..."
  }
}
```

Do not expose Java stack traces, database details, or internal URLs.

---

# 11. React UI

Create a polished and responsive UI.

## Required pages

### Dashboard

Display:

* Total invoices
* Draft invoices
* Sent invoices
* Partially paid invoices
* Paid invoices
* Overdue invoices
* Total outstanding amount
* Recent payments

### Accounts

Display:

* Account code
* Account name
* Account type
* Total debits
* Total credits
* Net balance

Include a form to create an account.

### Ledger

Display:

* Transaction date
* Description
* Reference type
* Reference ID
* Debit total
* Credit total
* Balanced indicator

Allow users to expand a transaction and view its journal entries.

### Invoices

Display:

* Invoice number
* Vendor name
* Due date
* Status badge
* Total
* Paid
* Outstanding

Include filters by invoice status.

### Create Invoice

Form fields:

* Invoice number
* Vendor name
* Vendor reference
* Issue date
* Due date
* Dynamic line items
* Description
* Quantity
* Unit price

The UI may accept dollars for user convenience, but it must convert the value safely to cents before sending it to the API.

Do not use floating-point arithmetic for calculations. Parse dollar input as a string and convert it deterministically to cents.

### Invoice Details

Display:

* Invoice information
* Line items
* Payment history
* Total
* Paid amount
* Outstanding amount
* Status
* Send invoice button
* Apply payment form

Disable payment actions when:

* Invoice is DRAFT
* Invoice is PAID
* Outstanding amount is zero

Show friendly validation errors for:

* Overpayment
* Duplicate payment conflict
* Invalid status
* Invalid currency
* Server errors

### Payment Demonstration

Include an `externalPaymentId` field or automatically generate one with an option to reuse it.

This should make it easy for reviewers to demonstrate duplicate webhook idempotency.

---

# 12. UI and UX Requirements

Use:

* Responsive navigation
* Accessible labels
* Semantic HTML
* Loading indicators
* Empty states
* Error banners
* Success notifications
* Disabled button states
* Status badges
* Confirmation before important actions
* Currency formatting using `Intl.NumberFormat`

Do not focus heavily on animation.

The design should look professional, clean, and suitable for a finance operations dashboard.

---

# 13. Database Design

Create normalized PostgreSQL tables.

Suggested tables:

```text
accounts
ledger_transactions
journal_entries
invoices
invoice_line_items
payments
```

Important constraints:

* Unique account code
* Unique invoice number
* Unique external payment ID
* Positive journal-entry amount
* Positive payment amount
* Positive line-item quantity
* Nonnegative unit price
* Invoice due date cannot be before issue date
* Valid enum values
* Foreign-key constraints
* Useful indexes

Recommended indexes:

* `journal_entries.account_id`
* `ledger_transactions.created_at`
* `invoices.status`
* `invoices.due_date`
* `payments.invoice_id`
* `payments.external_payment_id`
* `payments.status`

Use Flyway migrations for the complete database schema.

Do not depend on Hibernate auto-create in production.

Set:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

---

# 14. Transaction Boundaries

Use `@Transactional` in the Spring Boot service.

The apply-payment workflow must be one transaction:

1. Check whether the external payment ID already exists.
2. Lock the invoice row.
3. Load successful payments.
4. Derive current outstanding amount.
5. Validate the payment.
6. Save the payment.
7. Create the ledger transaction.
8. Create equal debit and credit journal entries.
9. Determine the resulting invoice status.
10. Commit.

Any failure must roll back the full operation.

Do not make network calls inside the database transaction.

---

# 15. Testing Requirements

Include meaningful automated tests.

## Spring Boot unit tests

Test:

* Balanced transaction succeeds.
* Unbalanced transaction is rejected.
* Zero or negative journal-entry amounts are rejected.
* Invoice total calculation is correct.
* Partial payment changes the calculated status to PARTIALLY_PAID.
* Full payment changes the calculated status to PAID.
* Overpayment is rejected.
* Duplicate external payment ID does not apply twice.
* Duplicate ID with different payload is rejected.
* Payment on a DRAFT invoice is rejected.
* Payment on a PAID invoice is rejected.

## Spring Boot integration tests

Use Testcontainers with PostgreSQL.

Test:

* Ledger transaction and entries persist atomically.
* Payment and ledger posting persist atomically.
* Unique external payment constraint works.
* Account balance is derived correctly.
* Concurrent payments cannot overpay an invoice.

Do not use H2 for PostgreSQL-specific locking tests.

## Node.js tests

Test:

* GraphQL input validation.
* Resolver-to-Spring-service mapping.
* Backend errors are converted to GraphQL errors.
* Correlation ID forwarding.

Mock the Spring Boot HTTP client where appropriate.

## React tests

Add at least a few tests:

* Invoice list renders data.
* Create-invoice validation works.
* Payment form prevents an amount above the displayed outstanding amount.
* API errors are shown to the user.

---

# 16. Local Development

Create a root-level Docker Compose configuration.

The following command should start the complete application:

```bash
docker compose up --build
```

Suggested local ports:

```text
React frontend:        5173
Node GraphQL service:  4000
Spring Boot service:   8080
PostgreSQL:            5432
```

Configure environment variables through `.env.example` files.

Never commit real credentials.

Required variables may include:

```text
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
DATABASE_URL
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
PAYMENT_SERVICE_BASE_URL
VITE_GRAPHQL_URL
CORS_ALLOWED_ORIGINS
```

Add Docker health checks.

Use service dependencies carefully so the application waits for PostgreSQL readiness.

---

# 17. Deployment

Prepare the project for deployment.

Suggested deployment model:

* React frontend: Vercel or Netlify
* Node.js GraphQL service: Render, Railway, or another Docker-compatible platform
* Spring Boot service: Render, Railway, or another Docker-compatible platform
* PostgreSQL: Neon, Supabase, Railway PostgreSQL, or Render PostgreSQL

Do not place service URLs directly in source code.

Use environment variables.

Deployment requirements:

1. Add a production Dockerfile for Node.js.
2. Add a multi-stage production Dockerfile for Spring Boot.
3. Add a production build configuration for React.
4. Configure CORS using environment variables.
5. Expose health endpoints.
6. Document every deployment step.
7. Include the following placeholders in the README:

```text
Live Frontend:
GraphQL Endpoint:
GraphQL Playground/Sandbox:
Spring Boot Health Endpoint:
GitHub Repository:
```

The final solution should be deployable without source-code changes.

Where platform configuration files are useful, include them.

---

# 18. GitHub Actions

Add a CI workflow that runs on pull requests and pushes.

The workflow must:

* Install frontend dependencies.
* Run frontend linting and tests.
* Build the frontend.
* Install Node.js API dependencies.
* Run Node.js linting and tests.
* Build the Node.js API.
* Set up Java 21.
* Run Spring Boot tests.
* Build the Spring Boot JAR.

A failing test must fail the pipeline.

---

# 19. Logging and Observability

Implement basic structured logging.

Every incoming GraphQL request should have a correlation ID.

Forward the correlation ID from Node.js to Spring Boot using:

```text
X-Correlation-ID
```

Include it in:

* Node.js logs
* Spring Boot logs
* Error responses

Add:

* Node health endpoint
* Spring Boot Actuator health endpoint

Do not log:

* Secrets
* Database passwords
* Full exception traces in client responses

---

# 20. Security and Validation

For this skill test, full authentication is optional.

However:

* Validate all inputs.
* Prevent SQL injection through parameterized JPA queries.
* Configure CORS explicitly.
* Use Helmet in Node.js.
* Add reasonable request-body limits.
* Avoid exposing internal errors.
* Do not commit secrets.
* Do not trust frontend calculations.
* Recalculate all financial values in Spring Boot.
* Sanitize and validate text lengths.
* Add pagination or reasonable result limits to list endpoints.

Document in the README that authentication and authorization would be added in a production system.

---

# 21. Suggested Repository Structure

Create a monorepo:

```text
mini-payment-ledger/
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── features/
│   │   │   ├── accounts/
│   │   │   ├── invoices/
│   │   │   ├── ledger/
│   │   │   └── payments/
│   │   ├── pages/
│   │   ├── routes/
│   │   ├── styles/
│   │   ├── utils/
│   │   └── tests/
│   └── .env.example
│
├── graphql-api/
│   ├── Dockerfile
│   ├── package.json
│   ├── tsconfig.json
│   ├── src/
│   │   ├── config/
│   │   ├── graphql/
│   │   │   ├── schema/
│   │   │   ├── resolvers/
│   │   │   └── scalars/
│   │   ├── clients/
│   │   ├── middleware/
│   │   ├── errors/
│   │   ├── validation/
│   │   └── tests/
│   └── .env.example
│
└── payment-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/example/paymentledger/
        │   │       ├── account/
        │   │       ├── ledger/
        │   │       ├── invoice/
        │   │       ├── payment/
        │   │       ├── common/
        │   │       └── config/
        │   └── resources/
        │       ├── application.yml
        │       └── db/migration/
        └── test/
            └── java/
```

Within Spring Boot, organize each domain by feature rather than putting every controller, service, and repository in global folders.

Example:

```text
invoice/
├── Invoice.java
├── InvoiceLineItem.java
├── InvoiceStatus.java
├── InvoiceRepository.java
├── InvoiceService.java
├── InvoiceController.java
├── InvoiceMapper.java
├── CreateInvoiceRequest.java
└── InvoiceResponse.java
```

---

# 22. Seed Data

Provide a development seed mechanism with:

* Accounts Payable account
* Cash account
* At least one additional expense account
* One DRAFT invoice
* One SENT invoice
* One PARTIALLY_PAID invoice
* One PAID invoice
* One OVERDUE invoice
* Sample successful payments
* Balanced ledger transactions

Seed data must not create incorrect balances or overpaid invoices.

Document how to disable development seed data.

---

# 23. README Requirements

Write a professional README containing:

## Project overview

Explain:

* Business problem
* Architecture
* Technology choices
* Core accounting model
* Idempotency strategy
* Concurrency strategy

## Architecture diagram

Include a Mermaid diagram.

## Local setup

Include exact commands:

```bash
git clone ...
cd mini-payment-ledger
cp .env.example .env
docker compose up --build
```

## Manual non-Docker setup

Document how to run:

* PostgreSQL
* Spring Boot service
* Node.js GraphQL API
* React frontend

## Test commands

Include exact commands for every module.

## API usage

Include sample GraphQL queries and mutations for:

* Creating an account
* Creating an invoice
* Sending an invoice
* Applying a partial payment
* Applying the final payment
* Retrying the same payment
* Attempting an overpayment
* Fetching account balance
* Viewing ledger transactions

## Important design decisions

Explain:

* Why money uses cents
* Why account balances are derived
* How double-entry accounting is validated
* How idempotency prevents duplicate payments
* How row-level locking prevents concurrent overpayment
* Why domain logic is inside Spring Boot rather than GraphQL resolvers

## Assumptions and shortcuts

Clearly state reasonable assignment shortcuts, such as:

* USD only
* No authentication
* No real payment gateway
* Fixed system accounts
* Integer line-item quantity
* Manual overdue refresh endpoint in addition to optional scheduling

## What I would improve with more time

Include:

* Authentication and role-based authorization
* Vendor management
* Payment gateway signature verification
* Refund and payment-reversal workflow
* Outbox pattern for reliable events
* Audit-event stream
* Pagination and filtering improvements
* Currency and exchange-rate support
* OpenTelemetry tracing
* Production secrets manager
* Rate limiting
* Kubernetes deployment
* Better UI accessibility testing

## Deployment

Include exact deployment steps and environment variables.

## Live links

Include placeholders for all hosted URLs.

---

# 24. Sample GraphQL Operations

Include examples similar to the following.

## Create invoice

```graphql
mutation CreateInvoice {
  createInvoice(
    input: {
      invoiceNumber: "INV-1001"
      vendorName: "ABC Transport Services"
      vendorReference: "VENDOR-123"
      currency: USD
      issueDate: "2026-07-10"
      dueDate: "2026-07-20"
      lineItems: [
        {
          description: "Freight service"
          quantity: 2
          unitPriceInCents: 5000
        }
        {
          description: "Loading charge"
          quantity: 1
          unitPriceInCents: 2000
        }
      ]
    }
  ) {
    id
    invoiceNumber
    totalInCents
    outstandingAmountInCents
    status
  }
}
```

Expected total:

```text
12000 cents
```

## Apply partial payment

```graphql
mutation ApplyPartialPayment {
  applyPayment(
    input: {
      externalPaymentId: "pay_gateway_1001"
      invoiceId: "replace-with-invoice-id"
      amountInCents: 5000
      currency: USD
    }
  ) {
    id
    externalPaymentId
    amountInCents
    status
    invoice {
      status
      totalInCents
      paidAmountInCents
      outstandingAmountInCents
    }
  }
}
```

Expected invoice result:

```text
status: PARTIALLY_PAID
paid: 5000
outstanding: 7000
```

Repeating the same mutation with the same payload must return the same payment without creating duplicate financial records.

---

# 25. Acceptance Criteria

The project is complete only when all of the following are true:

* Accounts can be created.
* Ledger transactions require balanced debit and credit entries.
* Account balances are calculated from the journal-entry log.
* Currency values never use floating point.
* Invoices can be created with multiple line items.
* Invoice totals are calculated by the backend.
* Draft invoices can be sent.
* Partial payments work.
* Full payments mark invoices as paid.
* Overpayments are rejected.
* Duplicate payment webhook requests are idempotent.
* Duplicate idempotency keys with different payloads are rejected.
* Concurrent payments cannot overpay an invoice.
* Successful payments create balanced ledger entries.
* React consumes the GraphQL API.
* GraphQL communicates with Spring Boot.
* Data persists in PostgreSQL.
* Docker Compose runs the complete system.
* Automated tests cover ledger, invoices, idempotency, and concurrency.
* CI builds and tests every module.
* The application is deployable.
* The README contains local and cloud deployment instructions.
* The repository contains no secrets.
* The UI provides a clear demonstration of all core flows.

---

# 26. Implementation Order

Build the project in this order:

1. Create repository structure.
2. Create PostgreSQL schema using Flyway.
3. Implement accounts and ledger domain.
4. Add ledger unit and integration tests.
5. Implement invoice creation and status rules.
6. Implement payment processing.
7. Add idempotency.
8. Add invoice locking and concurrent-payment test.
9. Implement internal Spring Boot REST APIs.
10. Implement Node.js GraphQL schema and resolvers.
11. Add GraphQL API tests.
12. Implement React pages.
13. Add frontend tests.
14. Add Dockerfiles and Docker Compose.
15. Add seed data.
16. Add GitHub Actions.
17. Write the README.
18. Add deployment configuration.
19. Deploy all services.
20. Verify deployed end-to-end flows.

---

# 27. Output Instructions

Do not only provide an explanation or architecture.

Generate the actual application code.

Work file by file and ensure imports, package names, configurations, scripts, and commands are consistent.

For every generated file:

* Provide its complete path.
* Provide its complete contents.
* Do not omit important sections using comments such as “rest of code here.”
* Do not leave compilation errors.
* Do not invent dependencies that are not included in package files or `pom.xml`.
* Keep names consistent across the database, Java DTOs, REST APIs, GraphQL schema, resolvers, and React types.

After implementation:

1. Run or logically verify all builds.
2. Fix compilation and type errors.
3. Fix failing tests.
4. Verify Docker Compose configuration.
5. Verify database migrations.
6. Verify example GraphQL operations.
7. Verify duplicate-payment behavior.
8. Verify concurrent-payment behavior.
9. Provide final local-run commands.
10. Provide final deployment steps.
11. Provide a submission checklist.

The result must look like a deliberate engineering submission, not a tutorial or generated demo.
