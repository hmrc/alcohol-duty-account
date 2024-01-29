# Get BTA Tile Data

Returns the Alcohol Duty data required in order to display the different possible states on the Business Tax Account ADR tile.
Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be returned.

**URL**: `/alcohol-duty-account/bta-tile-data/:alcoholDutyReference`

**Method**: `GET`

**URL Params**: `alcoholDutyReference` - String

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

| Field Name                            | Description                                                                                              | Data Type  | Mandatory/Optional                                                      | Notes                                        |
|---------------------------------------|----------------------------------------------------------------------------------------------------------|------------|-------------------------------------------------------------------------|----------------------------------------------|
| alcoholDutyReference                  | The unique Alcohol Duty reference that is the identifier for the ADR enrolment                           | String     | Mandatory                                                               |                                              |
| approvalStatus                        | The current approval status of the user(Approved, Insolvent, De-registered, Revoked, SmallCiderProducer) | Enum       | Mandatory                                                               |                                              |
| hasReturnsError                       | Indicates if there is an error for displaying the returns section                                        | Boolean    | Mandatory                                                               |                                              |
| hasPaymentsError                      | Indicates if there is an error for displaying the payments section                                       | Boolean    | Mandatory                                                               |                                              |
| returns                               | The data needed to create the returns section                                                            | Object     | Optional (Conditional based on the approvalStatus and hasReturnsError)  | Empty if there is no return due              |
| returns.dueReturnExists               | Indicates if there is a return due for the current period                                                | Boolean    | Mandatory                                                               |                                              |
| returns.numberOfOverdueReturns        | The number of overdue returns for previous periods                                                       | Integer    | Mandatory                                                               |                                              |
| payments                              | The data needed to create the payments section                                                           | Object     | Optional (Conditional based on the approvalStatus and hasPaymentsError) |                                              |
| payments.balance                      | The data needed to show the balance information                                                          | Object     | Optional                                                                | (Empty when there is no payment due)         |
| payments.balance.totalPaymentAmount   | The outstanding total payment amount                                                                     | BigDecimal | Mandatory                                                               |                                              |
| payments.balance.isMultiplePaymentDue | Indicates if the total payment amount is related to multiple payments(charges)                           | Boolean    | Mandatory                                                               |                                              |
| payments.balance.chargeReference      | The reference that is needed to start an OPS journey(Only applies if a single payment is due)            | String     | Optional (Conditional based on the isMultiplePaymentDue)                |                                              |
| payments.directDebit                  | The data needed to show the balance information                                                          | Object     | Optional                                                                | (Empty when there is no direct debit set up) |
| payments.directDebit.periodEndDate    | The end date of the return period that the direct debit is related to                                    | Date       | Mandatory                                                               |                                              |
| payments.directDebit.collectionDate   | The collection date for the direct debit                                                                 | Date       | Mandatory                                                               |                                              |

**Response Body Examples**

***Where no return is due / no payment is due / no direct debit set up:***

```json
{
  "alcoholDutyReference": "AP0000000001",
  "approvalStatus": "Approved",
  "hasReturnsError": false,
  "hasPaymentsError": false,
  "returns": {},
  "payments": {}
}
```

***Where latest return is due / payment is due / direct debit is set up:***

```json
{
  "alcoholDutyReference": "AP0000000001",
  "approvalStatus": "Approved",
  "hasReturnsError": false,
  "hasPaymentsError": false,
  "returns": {
    "dueReturnExists": true,
    "numberOfOverdueReturns": 0
  },
  "payments": {
    "balance": {
      "totalPaymentAmount": 3200.00,
      "isMultiplePaymentDue": false,
      "chargeReference": "APCHR123456789"
    },
    "directDebit": {
      "periodEndDate": "2023-03-31",
      "collectionDate": "2023-04-15"
    }
  }
}
```

***Where due and multiple overdue returns exist / multiple payments are due / direct debit is not set up:***

```json
{
  "alcoholDutyReference": "AP0000000001",
  "approvalStatus": "Approved",
  "hasReturnsError": false,
  "hasPaymentsError": false,
  "returns": {
    "dueReturnExists": true,
    "numberOfOverdueReturns": 3
  },
  "payments": {
    "balance": {
      "totalPaymentAmount": 3200.00,
      "isMultiplePaymentDue": true
    },
    "directDebit": {}
  }
}
```

***Insolvent:***

```json
{
  "alcoholDutyReference": "AP0000000001",
  "approvalStatus": "Insolvent",
  "hasReturnsError": false,
  "hasPaymentsError": false,
  "returns": {},
  "payments": {}
}
```

***De-registered / Revoked / Small Cider Producer:***

TODO

***Small Cider Producer:***

TODO

***Return Error (Same logic applies to payment error):***

```json
{
  "alcoholDutyReference": "AP0000000001",
  "approvalStatus": "Approved",
  "hasReturnsError": true,
  "hasPaymentsError": false,
  "returns": {},
  "payments": {
    "balance": {
      "totalPaymentAmount": 3200.00,
      "isMultiplePaymentDue": false,
      "chargeReference": "APCHR123456789"
    },
    "directDebit": {
      "periodEndDate": "2023-03-31",
      "collectionDate": "2023-04-15"
    }
  }
}
```

### Unauthorized response

**Code**: `401 UNAUTHORIZED`

This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.