# Get Fulfilled Obligation Details

Returns fulfilled obligations for the last 7 calendar years including the current year, with a minimum year of 2024.

Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be
returned.

**URL**: `/alcohol-duty-account/obligationDetails/fulfilled/:alcoholDutyReference`

**Method**: `GET`

**URL Params**

| Parameter Name       | Type   | Description | Notes |
|----------------------|--------|-------------|-------|
| alcoholDutyReference | String | The appa Id |       |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

***Example request:***

/alcohol-duty-account/obligationDetails/fulfilled/XMADP0000000001

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

Within the object for a specific year, the obligations array contains the fulfilled obligation details.

If NOT_FOUND is returned by the upstream API, an empty obligations array is returned for that year.

| Field Name            | Description                                        | Data Type    | Mandatory/Optional | Notes                       |
|-----------------------|----------------------------------------------------|--------------|--------------------|-----------------------------|
| year                  | The year of the obligation                         | Integer      | Mandatory          |                             |
| obligations           | An array of obligations                            | Array(Items) | Mandatory          |                             |
| obligations.status    | The current obligation status                      | Enum         | Mandatory          | Fulfilled                   |
| obligations.fromDate  | The date from which the period applies             | Date         | Mandatory          | YYYY-MM-DD                  |
| obligations.toDate    | The date to which the period applies               | Date         | Mandatory          | YYYY-MM-DD                  |
| obligations.dueDate   | The date the return is due to be filed and paid by | Date         | Mandatory          | YYYY-MM-DD                  |
| obligations.periodKey | The period key of the obligation                   | String       | Mandatory          | YYAM (year, 'A', month A-L) |

**Response Body Examples**

```json
[
  {
    "year": 2024,
    "obligations": [
      {
        "status": "Fulfilled",
        "fromDate": "2024-08-01",
        "toDate": "2024-08-31",
        "dueDate": "2024-09-15",
        "periodKey": "24AH"
      },
      {
        "status": "Fulfilled",
        "fromDate": "2024-05-01",
        "toDate": "2024-05-31",
        "dueDate": "2024-06-15",
        "periodKey": "24AE"
      }
    ]
  },
  {
    "year": 2025,
    "obligations": [
      {
        "status": "Fulfilled",
        "fromDate": "2025-03-01",
        "toDate": "2025-03-31",
        "dueDate": "2025-04-15",
        "periodKey": "25AC"
      }
    ]
  }
]
```

***No obligation details found:***

```json
[
  {
    "year": 2024,
    "obligations": []
  },
  {
    "year": 2025,
    "obligations": []
  }
]
```

### Responses

**Code**: `401 UNAUTHORIZED`
This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.

**Code**: `400 BAD_REQUEST`
This response can occur when the API returns BAD_REQUEST (e.g. invalid query parameter).

**Code**: `422 UNPROCESSABLE_ENTITY`
This response can occur when the API is called with an invalid regime.

**Code**: `500 INTERNAL_SERVER_ERROR`
This response can occur if the query to the upstream API fails or the response cannot be parsed.
