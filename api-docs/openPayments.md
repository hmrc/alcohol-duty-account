# Get Open Payments

Returns the payments which haven't yet been paid or refunded, included unallocated payments.
Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be returned.

**URL**: `/alcohol-duty-account/producers/:appaId/payments/open`

**Method**: `GET`

**URL Params**

| Parameter Name | Type   | Description    | Notes                     |
|----------------|--------|----------------|---------------------------|
| appaId         | String | The appa Id    |                           |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

The response body returns outstanding payments, their total, unallocated payments, their total, and the balance

If NOT_FOUND is returned by the downstream API, empty arrays of outstandingPayments and unallocated payments are returned.

| Field Name                            | Description                                  | Data Type     | Mandatory/Optional | Notes                                                      |
|---------------------------------------|----------------------------------------------|---------------|--------------------|------------------------------------------------------------|
| outstandingPayments                   | The outstanding payments array               | Array(Items)  | Mandatory          |                                                            |
| outstandingPayments.transactionType   | The type of transaction this refers to       | Enum          | Mandatory          | Return, PaymentOnAccount, LPI, RPI                         |
| outstandingPayments.dueDate           | The date the payment is due (or applied)     | Date          | Mandatory          | YYYY-MM-DD                                                 |
| outstandingPayments.chargeReference   | The charge reference if applicable           | String        | Optional           |                                                            |
| outstandingPayments.remainingAmount   | The remaining amount to pay                  | Numeric       | Mandatory          | Positive if a debt, negative if a credit                   |
| totalOutstandingPayments              | The total amount of the outstanding payments | Numeric       | Mandatory          |                                                            |
| unallocatedPayments                   | The unallocated payments array               | Array(Items)  | Mandatory          |                                                            |
| unallocatedPayments.paymentDate       | The date of the payment                      | Date          | Mandatory          | YYYY-MM-DD                                                 |
| unallocatedPayments.unallocatedAmount | The total amount unallocated                 | Numeric       | Mandatory          | As it's a credit, the amount is negative                   |
| totalUnallocatedPayments              | The total of the unallocated payments        | Numeric       | Mandatory          |                                                            |
| totalOpenPaymentsAmount               | The balance between the amounts              | Numeric       | Mandatory          | = totalOutstandingPayments - abs(totalUnallocatedPayments) |


**Response Body Examples**

***An outstanding return and an unallocated payment:***

```json
{
  "outstandingPayments": [
    {
      "transactionType": "Return",
      "dueDate": "2024-10-25",
      "chargeReference": "XA57334461623941",
      "remainingAmount": 9000
    }
  ],
  "totalOutstandingPayments": 9000,
  "unallocatedPayments": [
    {
      "paymentDate": "2024-09-01",
      "unallocatedAmount": -12000
    }
  ],
  "totalUnallocatedPayments": -12000,
  "totalOpenPaymentsAmount": -3000
}
```

***An outstanding return, RPI (not expecting these to appear but will be returned if so), an LPI, and a couple of unallocated payments:***

```json
{
  "outstandingPayments": [
    {
      "transactionType": "Return",
      "dueDate": "2024-07-25",
      "chargeReference": "XA18451492937496",
      "remainingAmount": 4577.44
    },
    {
      "transactionType": "RPI",
      "dueDate": "2024-04-01",
      "chargeReference": "XA43516574432087",
      "remainingAmount": -20.56
    },
    {
      "transactionType": "LPI",
      "dueDate": "2024-03-01",
      "chargeReference": "XA14362244607360",
      "remainingAmount": 10.56
    }
  ],
  "totalOutstandingPayments": 4567.44,
  "unallocatedPayments": [
    {
      "paymentDate": "2024-09-01",
      "unallocatedAmount": -1000
    },
    {
      "paymentDate": "2024-08-01",
      "unallocatedAmount": -500
    }
  ],
  "totalUnallocatedPayments": -1500,
  "totalOpenPaymentsAmount": 3067.44
}
```

***No outstanding payments found:***

```json
{
  "outstandingPayments": [],
  "totalOutstandingPayments": 0,
  "unallocatedPayments": [],
  "totalUnallocatedPayments": 0,
  "totalOpenPaymentsAmount": 0
}
```

### Responses
**Code**: `401 UNAUTHORIZED`
This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.

**Code**: `500 INTERNAL_SERVER_ERROR`
This response can occur if the downstream query to the API fails, the response cannot be parsed or the data is in error