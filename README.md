# Mini Wallet Transaction System

Spring Boot 3 · Java 17 · Spring Data JPA / Hibernate · **MySQL 8** · Maven

## 1. Run it

1. Install MySQL 8.0.16+ (CHECK constraints are enforced from this version).
2. Edit `src/main/resources/application.properties` and set `spring.datasource.username` / `password`.
   The database `mini_wallet` is created automatically (`createDatabaseIfNotExist=true`).
3. Start the app:
   ```
   mvn spring-boot:run
   ```
   Tables are created from `src/main/resources/schema.sql`. Hibernate only *validates* the
   schema (`ddl-auto=validate`), so the SQL file is the single source of truth.
4. Import `postman/Mini-Wallet.postman_collection.json` into Postman and run the
   "Happy path" folder top to bottom, then "Failure scenarios".

## 2. Endpoints

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/wallets` | 201 | 400 validation, 409 duplicate email |
| POST | `/api/wallets/{id}/deposit` | 200 | 400, 404 |
| POST | `/api/wallets/transfer` | 200 | 400 (amount / same wallet / insufficient balance), 404 |
| GET | `/api/wallets/{id}` | 200 | 404 |
| GET | `/api/wallets/{id}/transactions` | 200 (empty list if none) | 404 |

Error body: `{"errorCode": "INSUFFICIENT_BALANCE", "message": "..."}`
(validation errors add a `details` array). No stack traces or SQL are ever returned.

## 3. Design

* **Layers:** Controller (HTTP + `@Valid`) → Service (rules + transactions) → Repository (Spring Data JPA) → MySQL.
* **DTOs:** Java records; entities are never returned from controllers.
* **Money:** `BigDecimal` everywhere, `DECIMAL(19,2)` in MySQL, max 2 decimal places enforced.
* **History design:** a transfer writes two rows into `wallet_transaction` — `TRANSFER_OUT` for the
  sender and `TRANSFER_IN` for the receiver — sharing one `reference_id` (UUID). The `transactionId`
  returned from `/transfer` is the sender's (`TRANSFER_OUT`) row id. Histories are returned newest first.
* **DB constraints:** unique email, FK `wallet_transaction.wallet_id`, `balance >= 0`, `amount > 0`,
  CHECK on type/status, index on `(wallet_id, created_at, id)` for history queries.

## 4. Transfer & transaction-management strategy

`WalletServiceImpl.transfer` is annotated `@Transactional(rollbackFor = Exception.class)` and does:

1. Validate amount and reject `from == to`.
2. Lock both wallet rows with `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`), **always in ascending id order**.
3. Check balance → debit sender → credit receiver.
4. Insert the `TRANSFER_OUT` and `TRANSFER_IN` history rows.
5. Commit (all of it, or nothing).

**What if an exception happens after the sender is debited but before the receiver is credited?**
The exception propagates out of the `@Transactional` proxy, which calls `rollback()`. MySQL/InnoDB
undoes every statement in that transaction: the sender's debit is reverted and no history rows
exist. Nothing is ever visible to other transactions because it was never committed. Total money is unchanged.

**Why pessimistic locking + ordered locks?**
Without locks, two concurrent transfers could both read the same balance and overdraw the wallet
(lost update). The row lock serialises them. Locking in id order prevents deadlock when A→B and B→A
run at the same time. A lock wait longer than 5 s returns `409 CONCURRENT_UPDATE`.

**Limitations of `@Transactional` (be ready to discuss):**
* Spring uses proxies, so **self-invocation** (`this.transfer()` from another method in the same class) bypasses the transaction.
* Only works on **public** methods of Spring-managed beans.
* By default it rolls back only on **unchecked** exceptions; checked exceptions commit unless `rollbackFor` is set (done here).
* **Swallowing** an exception inside the method (try/catch without rethrow) means Spring sees success and commits.
* It only covers the one database/resource. External side effects (emails, HTTP calls, message queues) are not rolled back.
* The transaction is bound to a **thread**; work on other threads is not included.
* Isolation level alone does not stop lost updates in every case; explicit locking or `@Version` is still needed.
* `readOnly = true` is a hint, not a guarantee against writes.

## 5. Known simplifications / production next steps

* No authentication, no idempotency key on `/transfer` (a client retry could transfer twice).
* Only `SUCCESS` transactions are stored; failed attempts are rolled back and not audited.
* History is not paginated.
* Use Flyway/Liquibase instead of `schema.sql`; add integration tests (Testcontainers MySQL) including a concurrent-transfer test.
