# Testing Conditional Bills Section

## Test Case 1: New Receipt (No Bills Added)
**Endpoint:** `GET /v1/general-receipts/{transactionPoid}`

**Steps:**
1. Create a receipt with Credit GL but NO bills
2. Call GET endpoint
3. Verify response contains `pendingBills` array with data
4. Verify `bills` array is empty

**Expected:**
```json
{
  "creditGL": { "glCode": "GL-1201" },
  "receivedFrom": "ABC TRADING CO",  // Auto-populated from GL
  "bills": [],
  "pendingBills": [...]  // Should have data
}
```

## Test Case 2: Receipt With Bills
**Steps:**
1. Create a receipt with bills already added
2. Call GET endpoint
3. Verify `pendingBills` is null
4. Verify `bills` array has data

**Expected:**
```json
{
  "bills": [{ "billReference": "INV-001" }],
  "pendingBills": null  // Should be null
}
```

## Test Case 3: Verify Auto-Population
**Endpoint:** `POST /v1/general-receipts`

**Request Body:**
```json
{
  "header": {
    "creditGL": "GL-1201",
    "receivedFrom": "",  // Leave empty
    "currency": "USD",
    "rate": 0.376,
    "receiptAmount": 1000,
    "refType": "GENERAL",
    "narration": "Test",
    "companyPoid": 101
  },
  "payments": [
    {
      "type": "CASH",
      "amount": 1000
    }
  ]
}
```

**Expected Response:**
- `receivedFrom` should be auto-populated with GL description
- `pendingBills` should contain pending bills for GL-1201

## Manual Testing via Swagger

1. Go to: `http://localhost:8080/swagger-ui.html`
2. Find: `General Receipt` section
3. Test: `GET /v1/general-receipts/{transactionPoid}`
4. Check response for `pendingBills` field

## Database Verification

```sql
-- Check if GL has pending bills
SELECT * FROM GL_LEDGER 
WHERE GL_POID = 5001 
AND BALANCE > 0;

-- Check receipt data
SELECT * FROM AR_GEN_RECEIPT_HDR 
WHERE TRANSACTION_POID = 12345;

-- Check if bills exist
SELECT * FROM AR_GEN_RECEIPT_BILL_DTL 
WHERE TRANSACTION_POID = 12345;
```

## Expected Behavior Summary

| Condition | bills[] | pendingBills[] |
|-----------|---------|----------------|
| New receipt, no bills added | Empty | Has data |
| Receipt with bills | Has data | null |
| GL with no pending bills | Empty | Empty array |
