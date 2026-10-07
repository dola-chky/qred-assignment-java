# Dummy Data Setup Complete ✅

## What Was Created

A Flyway SQL migration file has been created to automatically insert dummy data when the application starts:

**File:** `/src/main/resources/db/migration/V4__insert_dummy_data.sql`

This migration will:
- ✅ Run automatically when the application boots
- ✅ Insert 6 test clients
- ✅ Insert 7 test contracts (with proper client associations)
- ✅ Use `ON CONFLICT DO NOTHING` to prevent errors on re-runs

---

## Dummy Data Available

### Clients (6 total)
```
CLIENT001   → 550e8400-e29b-41d4-a716-446655440001
CLIENT002   → 550e8400-e29b-41d4-a716-446655440002
CLIENT003   → 550e8400-e29b-41d4-a716-446655440003
BIGCLIENT   → 550e8400-e29b-41d4-a716-446655440004
ACMECORP    → 550e8400-e29b-41d4-a716-446655440005
GLOBALTECH  → 550e8400-e29b-41d4-a716-446655440006
```

### Contracts (7 total)
```
CONTRACT001      ↔ CLIENT001
CONTRACT002      ↔ CLIENT002
CONTRACT003      ↔ CLIENT003
CONTRACT004      ↔ CLIENT001 (second contract)
PREMIUM_PLAN     ↔ BIGCLIENT
ENTERPRISE_PLAN  ↔ ACMECORP
STANDARD_CONTRACT↔ GLOBALTECH
```

---

## How to Use

### Step 1: Start the Application
```bash
cd /home/dchakraborty/workspace/qred-payment-backend
./gradlew bootRun
```

The Flyway migration will automatically run on startup and insert the dummy data.

### Step 2: Create a Payment

Use any of the clients and contracts above:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440111",
    "clientNumber": "CLIENT001",
    "contractNumber": "CONTRACT001",
    "paymentDate": "2024-10-05",
    "amount": 1000.00,
    "type": "incoming"
  }'
```

### Step 3: Get Payments

Query payments for a client:

```bash
curl http://localhost:8080/api/payments?clientNumber=CLIENT001
```

---

## Test Payloads Ready to Use

### Payload 1: CLIENT001 Payment
```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440111",
  "clientNumber": "CLIENT001",
  "contractNumber": "CONTRACT001",
  "paymentDate": "2024-10-05",
  "amount": 1000.00,
  "type": "incoming"
}
```

### Payload 2: BIGCLIENT Payment
```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440222",
  "clientNumber": "BIGCLIENT",
  "contractNumber": "PREMIUM_PLAN",
  "paymentDate": "2024-10-05",
  "amount": 50000.00,
  "type": "outgoing"
}
```

### Payload 3: ACMECORP Payment
```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440333",
  "clientNumber": "ACMECORP",
  "contractNumber": "ENTERPRISE_PLAN",
  "paymentDate": "2024-10-06",
  "amount": 25000.99,
  "type": "incoming"
}
```

### Payload 4: GLOBALTECH Payment
```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440444",
  "clientNumber": "GLOBALTECH",
  "contractNumber": "STANDARD_CONTRACT",
  "paymentDate": "2024-10-06",
  "amount": 15000.50,
  "type": "incoming"
}
```

---

## How Flyway Works

1. **On Application Startup**: Flyway scans `/src/main/resources/db/migration/`
2. **Versioning**: Migration files are prefixed with `V` + version number (V1, V2, V3, V4...)
3. **Execution Order**: Migrations run in version order (V1 → V2 → V3 → V4)
4. **Track Changes**: Flyway maintains a `flyway_schema_history` table
5. **Idempotent**: `ON CONFLICT DO NOTHING` ensures re-runs don't cause errors

---

## Build Status

✅ **Build**: SUCCESSFUL
✅ **Tests**: 41/41 PASSING
✅ **Ready to Run**: YES

The application is ready to start with dummy data! 🚀

