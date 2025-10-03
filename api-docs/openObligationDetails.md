# Get Open Obligation Details

Returns the Obligation Details for open obligations.

Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be
returned.

**URL**: `/alcohol-duty-account/obligationDetails/open/:alcoholDutyReference`

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

/alcohol-duty-account/obligationDetails/open/XMADP0000000001

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

The response body returns an array of obligations (each containing the following fields)

If NOT_FOUND is returned by the upstream API, an empty array is returned.

| Field Name | Description                                        | Data Type | Mandatory/Optional | Notes                       |
|------------|----------------------------------------------------|-----------|--------------------|-----------------------------|
| status     | The current obligation status                      | Enum      | Mandatory          | Open                        |
| fromDate   | The date from which the period applies             | Date      | Mandatory          | YYYY-MM-DD                  |
| toDate     | The date to which the period applies               | Date      | Mandatory          | YYYY-MM-DD                  |
| dueDate    | The date the return is due to be filed and paid by | Date      | Mandatory          | YYYY-MM-DD                  |
| periodKey  | The period key of the obligation                   | String    | Mandatory          | YYAM (year, 'A', month A-L) |

**Response Body Examples**

***Two open obligations:***

```json
[
  {
    "status": "Open",
    "fromDate": "2024-08-01",
    "toDate": "2024-08-31",
    "dueDate": "2024-09-10",
    "periodKey": "24AH"
  },
  {
    "status": "Open",
    "fromDate": "2024-05-01",
    "toDate": "2024-05-31",
    "dueDate": "2024-06-15",
    "periodKey": "24AE"
  }
]
```

***No obligation details found:***

```json
[]
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
