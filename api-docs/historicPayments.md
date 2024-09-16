# Get Historic Payments

Returns items which have been paid or part-paid where the amount is above 0 (i.e. not Nil or owing to the customer) for a specific year.

Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be returned.

**URL**: `/alcohol-duty-account/producers/:appaId/payments/historic/:year`

**Method**: `GET`

**URL Params**:

| Parameter Name | Type    | Description        | Notes                                       |
|----------------|---------|--------------------|---------------------------------------------|
| appaId         | String  | The appa Id        |                                             |
| year           | Integer | The year to query  | Only a single year can be queried at a time |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

The response body returns items which have been paid or part-paid payments relating to the year where the amount is above 0 (i.e. not Nil or owing to the user).
There is nothing to distinguish whether a payment is part or fully paid. Part paid items will also be returned in open payments.

If NOT_FOUND is returned by the downstream API, an empty array of payments is returned.

| Field Name               | Description                                  | Data Type       | Mandatory/Optional | Notes                                         |
|--------------------------|----------------------------------------------|-----------------|--------------------|-----------------------------------------------|
| year                     | The year queried                             | Integer         | Mandatory          |                                               |
| payments                 | An array of payments                         | Array(Payments) | Mandatory          | Only those paid or part paid (amountPaid > 0) |
| payments.period          | The period this relates to (as a period key) | String          | Mandatory          | YYAM (year, 'A,' month A-L)                   |
| payments.transactionType | The type of transaction this refers to       | Enum            | Mandatory          | Return, LPI, RPI                              |
| payments.chargeReference | The charge reference if applicable           | String          | Optional           |                                               |
| payments.amountPaid      | The amount paid                              | Numeric         | Mandatory          |                                               |

**Response Body Examples**

***A (part) paid return and (part) paid LPI: ***

```json
{
  "year": 2024,
  "payments": [
    {
      "period": "24AE",
      "transactionType": "Return",
      "chargeReference": "XA57978503902370",
      "amountPaid": 2000
    },
    {
      "period": "24AC",
      "transactionType": "LPI",
      "chargeReference": "XA02088676456437",
      "amountPaid": 10
    }
  ]
}
```

***No outstanding payments found for that year: ***

```json
{
"year": 2024,
"payments": []
}
```

### Responses
**Code**: `400 BAD_REQUEST`
This response can occur when year is before 2024 or after the current year

**Code**: `401 UNAUTHORIZED`
This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.

**Code**: `500 INTERNAL_SERVER_ERROR`
This response can occur if the downstream query to the API fails, the response cannot be parsed or the data is in error