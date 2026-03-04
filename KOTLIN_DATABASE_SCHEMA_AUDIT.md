# Database Schema Audit — Kotlin Migration Compatibility

**Source**: Ruby on Rails 7.1 schema (`db/schema.rb`)  
**Target**: Kotlin/Spring Boot 3 + Flyway + PostgreSQL 16  
**Audited**: 2026-03-01

---

## Summary

The Rails schema is functional but was built incrementally through Devise retrofits and index-addition migrations. Translated naively, it carries several compatibility gaps against the Kotlin target. The most critical is the **missing `idempotency_key` column** — a banking-grade requirement that does not exist anywhere in the Ruby schema. Secondary issues involve type precision, naming conventions, constraint coverage, and a dangerously permissive delete cascade.

| Severity | Count | Items |
|---|---|---|
| 🔴 Critical | 2 | Missing `idempotency_key`, missing DB-level constraints on currency codes |
| 🟡 Significant | 4 | Column naming (`from_value`/`to_value`), `timestamp` column name, integer PK for users, `ON DELETE CASCADE` |
| 🟢 Minor | 3 | Precision differences, duplicate indexes, `updated_at` on transactions |

---

## Table: `transactions`

### Ruby schema (current)

```sql
CREATE TABLE transactions (
    id            BIGSERIAL PRIMARY KEY,          -- integer auto-increment
    user_id       BIGINT NOT NULL,
    from_currency VARCHAR(255) NOT NULL,          -- unconstrained string
    to_currency   VARCHAR(255) NOT NULL,          -- unconstrained string
    from_value    NUMERIC(12, 4) NOT NULL,        -- 12 total digits
    to_value      NUMERIC(12, 4) NOT NULL,
    rate          NUMERIC(12, 8) NOT NULL,
    timestamp     TIMESTAMP NOT NULL,             -- no timezone, bad name
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)    -- ON DELETE CASCADE (Rails default)
);
```

### Kotlin target (blueprint V2/V3)

```sql
CREATE TABLE transactions (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         BIGINT  NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key UUID    NOT NULL,
    from_currency   CHAR(3) NOT NULL,
    to_currency     CHAR(3) NOT NULL,
    from_amount     NUMERIC(19, 4) NOT NULL,
    to_amount       NUMERIC(19, 4) NOT NULL,
    rate            NUMERIC(19, 8) NOT NULL,
    converted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transactions_positive_amounts
        CHECK (from_amount > 0 AND to_amount > 0 AND rate > 0),
    CONSTRAINT chk_transactions_different_currencies
        CHECK (from_currency != to_currency),
    CONSTRAINT uk_transactions_idempotency_key
        UNIQUE (idempotency_key)
);
```

---

### Findings: `transactions`

#### 🔴 CRITICAL — `idempotency_key` column is absent

**Rails**: No such column exists in any migration or the schema file.  
**Kotlin target**: `idempotency_key UUID NOT NULL UNIQUE` is a first-class column with a unique constraint.  
**Risk**: Without this, `POST /transactions` creates duplicate records on any client retry. Unacceptable in a financial API.  
**Action**: Flyway `V3__add_idempotency_key.sql` must add this column. If backfilling an existing Rails database, a data migration step is required to populate the column before the `NOT NULL` constraint can be enforced.

```sql
-- Migration path for an existing Rails database
ALTER TABLE transactions ADD COLUMN idempotency_key UUID;
UPDATE transactions SET idempotency_key = gen_random_uuid(); -- backfill
ALTER TABLE transactions ALTER COLUMN idempotency_key SET NOT NULL;
CREATE UNIQUE INDEX uk_transactions_idempotency_key ON transactions(idempotency_key);
```

---

#### 🔴 CRITICAL — No DB-level constraint on currency codes

**Rails**: `from_currency` and `to_currency` are `VARCHAR(255)`. Validation is purely at the application layer (`SUPPORTED_CURRENCIES` constant).  
**Kotlin target**: `CHAR(3)` with `CHECK` constraints enforcing the enum (`BRL`, `USD`, `EUR`, `JPY`).  
**Risk**: Any Rails migration, seed, or direct DB write can insert arbitrary strings. The Kotlin service would never produce invalid values, but the data inherited from Rails is not guaranteed clean.  
**Action**:
1. Audit existing rows: `SELECT DISTINCT from_currency, to_currency FROM transactions;`
2. Add `CHAR(3)` type change and CHECK constraints in a separate migration after validating no bad data exists.
3. In V3, add constraints as shown in the blueprint.

---

#### 🟡 SIGNIFICANT — Column naming: `from_value`/`to_value` → `from_amount`/`to_amount`

**Rails**: `from_value`, `to_value`  
**Kotlin target**: `from_amount`, `to_amount`  
**Why it matters**: The JPA `TransactionEntity` maps to `from_amount`/`to_amount` via `@Column(name = "from_amount")`. If the Kotlin service runs against the unchanged Rails schema, all queries that read or write these columns will fail at runtime.  
**Action**: Rename columns in the Flyway migration or update JPA `@Column` annotations to match the Rails names (preferred if not doing a clean migration).

```sql
-- Option A: rename columns (clean, preferred)
ALTER TABLE transactions RENAME COLUMN from_value TO from_amount;
ALTER TABLE transactions RENAME COLUMN to_value TO to_amount;

-- Option B: keep Rails names, override in JPA (no migration required)
@Column(name = "from_value") val fromAmount: BigDecimal
```

---

#### 🟡 SIGNIFICANT — `timestamp` column → `converted_at`

**Rails**: `timestamp TIMESTAMP NOT NULL` — no time zone, semantically ambiguous name.  
**Kotlin target**: `converted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()`  
**Why it matters**: `TIMESTAMP` (without time zone) stores values in server local time. In any environment where the server is not UTC, this produces silent data corruption when querying across DST transitions. `TIMESTAMP WITH TIME ZONE` stores UTC and converts on read.  
**Action**:
1. Rename the column: `ALTER TABLE transactions RENAME COLUMN timestamp TO converted_at;`
2. Change type: `ALTER TABLE transactions ALTER COLUMN converted_at TYPE TIMESTAMP WITH TIME ZONE USING converted_at AT TIME ZONE 'UTC';`
3. Verify existing Rails data was written in UTC (it likely was, if `config.time_zone = 'UTC'`).

---

#### 🟡 SIGNIFICANT — `id` type: `BIGSERIAL` (integer) → `UUID`

**Rails**: `transactions.id BIGSERIAL` — sequential integer PK.  
**Kotlin target**: `transactions.id UUID DEFAULT gen_random_uuid()`  
**Why it matters**: Integer PKs expose table size (record count is trivially deduced from max ID), are enumerable (allows scraping all records by incrementing IDs), and create hotspot inserts on B-tree indexes. UUIDs avoid all three.  
**Impact on migration**: If migrating an existing Rails database, integer IDs cannot be converted to UUIDs without rebuilding the table and all foreign keys. This is a **hard cut** — the Kotlin service should start with a fresh schema or use a separate database.  
**Recommendation**: Start the Kotlin service on a fresh PostgreSQL database. Use the Rails database for read-only historical queries if needed.

---

#### 🟡 SIGNIFICANT — `ON DELETE CASCADE` (implicit Rails default) → `ON DELETE RESTRICT`

**Rails**: `t.references :user, null: false, foreign_key: true` — Rails' `foreign_key: true` creates `ON DELETE CASCADE`, silently deleting all transactions when a user is deleted.  
**Kotlin target**: `REFERENCES users(id) ON DELETE RESTRICT`  
**Risk**: Deleting a user in production would destroy their transaction history — a regulatory and audit violation.  
**Action**: The Kotlin schema must explicitly declare `ON DELETE RESTRICT`. For the existing Rails database, add it explicitly:
```sql
ALTER TABLE transactions DROP CONSTRAINT transactions_user_id_fkey;
ALTER TABLE transactions ADD CONSTRAINT transactions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
```

---

#### 🟡 SIGNIFICANT — `NUMERIC` precision: `(12, 4)` → `(19, 4)`

**Rails**: `NUMERIC(12, 4)` — 12 significant digits total, 4 decimal places, so max 8 integer digits: `99,999,999.9999`.  
**Kotlin target**: `NUMERIC(19, 4)` — 19 significant digits, supports up to `999,999,999,999,999.9999`.  
**Why it matters**: `NUMERIC(12, 4)` overflows at ~100 million base currency units. A `JPY 100,000,000` conversion (100M yen, not uncommon) would fail. The Kotlin schema correctly uses `(19, 4)`.  
**Action**: Widen the columns:
```sql
ALTER TABLE transactions
    ALTER COLUMN from_amount TYPE NUMERIC(19, 4),
    ALTER COLUMN to_amount   TYPE NUMERIC(19, 4),
    ALTER COLUMN rate        TYPE NUMERIC(19, 8);
```

---

#### 🟢 MINOR — Duplicate index on `(from_currency, to_currency)`

**Rails schema** shows two identical indexes:
```
index_transactions_on_currency_pair
index_transactions_on_from_currency_and_to_currency
```
Both cover the same columns. The duplicate was created by running the original `create_table` migration (which adds one) and the later `AddIndexesToTransactions` migration (which adds the other with `if_not_exists: true` — but `if_not_exists` checks by name, not by column set, so a differently-named duplicate was created).  
**Action**: Drop one:
```sql
DROP INDEX index_transactions_on_from_currency_and_to_currency;
```

---

#### 🟢 MINOR — `updated_at` on an append-only table

**Rails**: `transactions` has `updated_at` (via `t.timestamps`).  
**Kotlin target**: No `updated_at` column — transactions are immutable facts.  
**Risk**: Low (no functional breakage), but it is semantically incorrect and adds noise to schema audits.  
**Action**: `ALTER TABLE transactions DROP COLUMN updated_at;` — do this only after confirming no Rails code reads `updated_at` on transactions.

---

## Table: `users`

### Ruby schema (current)

```sql
CREATE TABLE users (
    id                     BIGSERIAL PRIMARY KEY,
    email                  VARCHAR(255),           -- nullable, no length constraint
    jti                    VARCHAR(255),           -- nullable initially
    encrypted_password     VARCHAR(255) NOT NULL DEFAULT '',
    reset_password_token   VARCHAR(255),
    reset_password_sent_at TIMESTAMP,
    remember_created_at    TIMESTAMP,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL
);
```

### Kotlin target (blueprint V1)

```sql
CREATE TABLE users (
    id                     BIGSERIAL PRIMARY KEY,
    email                  VARCHAR(255) NOT NULL,
    encrypted_password     VARCHAR(255) NOT NULL DEFAULT '',
    jti                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    reset_password_token   VARCHAR(255),
    reset_password_sent_at TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_jti   UNIQUE (jti)
);
```

---

### Findings: `users`

#### 🟡 SIGNIFICANT — `email` is nullable in Rails

**Rails**: `t.string :email` — no `null: false`. The unique index exists but the column itself is nullable. Two users with `NULL` email would not violate the unique index (NULLs are not equal in SQL).  
**Kotlin target**: `email VARCHAR(255) NOT NULL`  
**Action**: Check for any NULL email rows, then:
```sql
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
```

---

#### 🟡 SIGNIFICANT — `jti` type: `VARCHAR(255)` → `UUID`

**Rails**: `jti VARCHAR(255)` — stores the JWT ID as a string.  
**Kotlin target**: `jti UUID NOT NULL DEFAULT gen_random_uuid()`  
**Why it matters**: `UUID` type uses 16 bytes vs. 36 bytes for the string representation. More importantly, the `DEFAULT gen_random_uuid()` ensures new users always get a JTI, eliminating the nullable window.  
**Action**:
```sql
ALTER TABLE users ALTER COLUMN jti TYPE UUID USING jti::UUID;
ALTER TABLE users ALTER COLUMN jti SET NOT NULL;
ALTER TABLE users ALTER COLUMN jti SET DEFAULT gen_random_uuid();
```
⚠️ This will fail if any `jti` values are not valid UUID strings. Audit first:
```sql
SELECT id, jti FROM users WHERE jti !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
```

---

#### 🟢 MINOR — Timestamps without timezone

**Rails**: `created_at TIMESTAMP`, `updated_at TIMESTAMP` — no timezone on users table either.  
**Kotlin target**: `TIMESTAMP WITH TIME ZONE`  
**Action**: Same conversion as transactions — add `AT TIME ZONE 'UTC'` cast in the migration.

---

## Index Audit

| Index | Rails | Kotlin Target | Status |
|---|---|---|---|
| `users(email)` UNIQUE | ✅ | ✅ | Match |
| `users(jti)` UNIQUE | ✅ | ✅ | Match |
| `users(reset_password_token)` UNIQUE | ✅ | ✅ | Match |
| `transactions(user_id, converted_at DESC)` | `(user_id, timestamp)` — no direction | ✅ with DESC | Needs direction clause |
| `transactions(from_currency, to_currency)` | ✅ (duplicated) | ✅ (once) | Drop duplicate |
| `transactions(idempotency_key)` UNIQUE | ❌ Missing | ✅ | Must add |
| `transactions(user_id)` standalone | ✅ (redundant with composite) | Not in target | Can drop |
| `transactions(timestamp)` standalone | ✅ (redundant) | Not in target | Can drop |

**Note on index direction**: The primary list query is `ORDER BY converted_at DESC`. The index `(user_id, converted_at DESC)` allows an index scan with no sort step. The Rails index `(user_id, timestamp)` without DESC still works but adds a sort pass on large datasets.

---

## Full Migration Script (Rails DB → Kotlin Compatible)

This script transforms an existing Rails database to be compatible with the Kotlin schema. Run inside a transaction; test on a copy first.

```sql
BEGIN;

-- 1. Rename ambiguous columns
ALTER TABLE transactions RENAME COLUMN from_value TO from_amount;
ALTER TABLE transactions RENAME COLUMN to_value   TO to_amount;
ALTER TABLE transactions RENAME COLUMN timestamp  TO converted_at;

-- 2. Fix timestamp timezones
ALTER TABLE transactions
    ALTER COLUMN converted_at TYPE TIMESTAMP WITH TIME ZONE
        USING converted_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at   TYPE TIMESTAMP WITH TIME ZONE
        USING created_at   AT TIME ZONE 'UTC';
ALTER TABLE users
    ALTER COLUMN created_at             TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at             TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN reset_password_sent_at TYPE TIMESTAMP WITH TIME ZONE USING reset_password_sent_at AT TIME ZONE 'UTC';

-- 3. Widen numeric precision
ALTER TABLE transactions
    ALTER COLUMN from_amount TYPE NUMERIC(19, 4),
    ALTER COLUMN to_amount   TYPE NUMERIC(19, 4),
    ALTER COLUMN rate        TYPE NUMERIC(19, 8);

-- 4. Add idempotency key (backfill, then enforce NOT NULL)
ALTER TABLE transactions ADD COLUMN idempotency_key UUID;
UPDATE transactions SET idempotency_key = gen_random_uuid();
ALTER TABLE transactions ALTER COLUMN idempotency_key SET NOT NULL;
CREATE UNIQUE INDEX uk_transactions_idempotency_key ON transactions(idempotency_key);

-- 5. Add CHECK constraints on currency codes
ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_positive_amounts
        CHECK (from_amount > 0 AND to_amount > 0 AND rate > 0),
    ADD CONSTRAINT chk_transactions_different_currencies
        CHECK (from_currency != to_currency),
    ADD CONSTRAINT chk_from_currency CHECK (from_currency IN ('BRL','USD','EUR','JPY')),
    ADD CONSTRAINT chk_to_currency   CHECK (to_currency   IN ('BRL','USD','EUR','JPY'));

-- 6. Fix ON DELETE behavior
ALTER TABLE transactions DROP CONSTRAINT transactions_user_id_fkey;
ALTER TABLE transactions ADD CONSTRAINT transactions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

-- 7. Fix users table
ALTER TABLE users ALTER COLUMN email SET NOT NULL;
ALTER TABLE users ALTER COLUMN jti   TYPE UUID USING jti::UUID;
ALTER TABLE users ALTER COLUMN jti   SET NOT NULL;
ALTER TABLE users ALTER COLUMN jti   SET DEFAULT gen_random_uuid();

-- 8. Drop duplicate / redundant indexes
DROP INDEX IF EXISTS index_transactions_on_from_currency_and_to_currency;
DROP INDEX IF EXISTS index_transactions_on_user_id;      -- covered by composite
DROP INDEX IF EXISTS index_transactions_on_timestamp;    -- covered by composite

-- 9. Add DESC direction to primary query index
DROP INDEX IF EXISTS index_transactions_on_user_and_timestamp;
CREATE INDEX idx_transactions_user_converted_at ON transactions(user_id, converted_at DESC);

-- 10. Drop updated_at from immutable transactions table (optional)
-- ALTER TABLE transactions DROP COLUMN updated_at;  -- uncomment after confirming no readers

COMMIT;
```

---

## JPA Entity vs. Schema Compatibility Checklist

| JPA field | Mapped column | Type | Nullable | Match? |
|---|---|---|---|---|
| `TransactionEntity.id` | `id` | `UUID` | ❌ | ✅ after migration |
| `TransactionEntity.userId` | `user_id` | `BIGINT` | ❌ | ✅ |
| `TransactionEntity.idempotencyKey` | `idempotency_key` | `UUID` | ❌ | 🔴 Missing in Rails |
| `TransactionEntity.fromCurrency` | `from_currency` | `CHAR(3)` | ❌ | 🟡 Rails has `VARCHAR(255)` |
| `TransactionEntity.toCurrency` | `to_currency` | `CHAR(3)` | ❌ | 🟡 Rails has `VARCHAR(255)` |
| `TransactionEntity.fromAmount` | `from_amount` | `NUMERIC(19,4)` | ❌ | 🟡 Rails has `from_value` |
| `TransactionEntity.toAmount` | `to_amount` | `NUMERIC(19,4)` | ❌ | 🟡 Rails has `to_value` |
| `TransactionEntity.rate` | `rate` | `NUMERIC(19,8)` | ❌ | 🟡 Rails has `NUMERIC(12,8)` |
| `TransactionEntity.convertedAt` | `converted_at` | `TIMESTAMP WITH TIME ZONE` | ❌ | 🟡 Rails has `timestamp TIMESTAMP` |
| `UserEntity.id` | `id` | `BIGINT` | ❌ | ✅ |
| `UserEntity.email` | `email` | `VARCHAR(255)` | ❌ | 🟡 Rails allows NULL |
| `UserEntity.jti` | `jti` | `UUID` | ❌ | 🟡 Rails has `VARCHAR(255)` |

**Bottom line**: Running the Kotlin service against an unmodified Rails database will cause runtime failures on every transaction read and write. The migration script above must be applied first, or the Kotlin service must target a fresh database seeded by Flyway.

