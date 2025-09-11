# Get Historic Payments

Returns items which have been paid or part-paid where the amount is above 0 (i.e. not Nil or owing to the customer)
for the last 7 calendar years including the current year, with a minimum year of 2024.

Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be
returned.

**URL**: `/alcohol-duty-account/producers/:appaId/payments/historic`

**Method**: `GET`

**URL Params**

| Parameter Name | Type   | Description | Notes |
|----------------|--------|-------------|-------|
| appaId         | String | The appa Id |       |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

***Example request:***

/alcohol-duty-account/producers/AP0000000001/payments/historic

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

The response body returns an array in which each item corresponds to the historic payments for a calendar year.
Within the object for a specific year, the payments array contains items which have been paid or part-paid payments
where the amount is above 0 (i.e. not Nil or owing to the user). There is nothing to distinguish whether a payment is
part or fully paid. Part-paid items will also be returned in open payments.

If NOT_FOUND is returned by the downstream API for a specific year, an empty array of payments is returned for that
year.

| Field Name               | Description                                  | Data Type    | Mandatory/Optional | Notes                                         |
|--------------------------|----------------------------------------------|--------------|--------------------|-----------------------------------------------|
| year                     | The year of the return period                | Integer      | Mandatory          |                                               |
| payments                 | An array of payments                         | Array(Items) | Mandatory          | Only those paid or part paid (amountPaid > 0) |
| payments.period          | The period this relates to (as a period key) | String       | Mandatory          | YYAM (year, 'A,' month A-L)                   |
| payments.taxPeriodFrom   | The start date of the period this relates to | Date         | Mandatory          | YYYY-MM-DD                                    |
| payments.taxPeriodTo     | The end date of the period this relates to   | Date         | Mandatory          | YYYY-MM-DD                                    |
| payments.transactionType | The type of transaction this refers to       | Enum         | Mandatory          | Return, LPI, CA, CAI                          |
| payments.chargeReference | The charge reference if applicable           | String       | Optional           |                                               |
| payments.amountPaid      | The amount paid                              | Numeric      | Mandatory          |                                               |

**Response Body Examples**

***A (part) paid return, LPI and central assessment:***

```json
[
  {
    "year": 2024,
    "payments": [
      {
        "period": "24AE",
        "taxPeriodFrom": "2024-05-01",
        "taxPeriodTo": "2024-05-31",
        "transactionType": "Return",
        "chargeReference": "XA57978503902370",
        "amountPaid": 2000
      },
      {
        "period": "24AC",
        "taxPeriodFrom": "2024-03-01",
        "taxPeriodTo": "2024-03-31",
        "transactionType": "LPI",
        "chargeReference": "XA02088676456437",
        "amountPaid": 10
      }
    ]
  },
  {
    "year": 2025,
    "payments": [
      {
        "period": "25AC",
        "taxPeriodFrom": "2025-03-01",
        "taxPeriodTo": "2025-03-31",
        "transactionType": "CA",
        "chargeReference": "XA46070058554819",
        "amountPaid": 1500
      },
      {
        "period": "25AB",
        "taxPeriodFrom": "2025-02-01",
        "taxPeriodTo": "2025-02-28",
        "transactionType": "Return",
        "chargeReference": "XA46070058554819",
        "amountPaid": 3000
      },
      {
        "period": "25AA",
        "taxPeriodFrom": "2025-01-01",
        "taxPeriodTo": "2025-01-31",
        "transactionType": "LPI",
        "chargeReference": "XA20860833337527",
        "amountPaid": 15
      }
    ]
  }
]
```

***No historic payments found:***

```json
[
  {
    "year": 2024,
    "payments": []
  },
  {
    "year": 2025,
    "payments": []
  }
]
```

### Responses

**Code**: `401 UNAUTHORIZED`
This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.

**Code**: `500 INTERNAL_SERVER_ERROR`
This response can occur if the downstream query to the API fails, the response cannot be parsed or the data is in error