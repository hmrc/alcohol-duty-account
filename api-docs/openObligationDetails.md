# Get Open Obligation Details

Returns the Obligation Details for a specific period if it's open

Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be returned.

**URL**: `/alcohol-duty-account/openObligationDetails/:alcoholDutyReference/:periodKey`

**Method**: `GET`

**URL Params**

| Parameter Name       | Type   | Description    | Notes                     |
|----------------------|--------|----------------|---------------------------|
| alcoholDutyReference | String | The appa Id    |                           |
| periodKey            | String | The period key | YYAM (year, A, month A-L) |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

***Example request:***

/alcohol-duty-account/openObligationDetails/AP0000000001/24AF

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

The response body returns the obligation details

Unlike [obligationDetails](obligationDetails.md) If NOT_FOUND is returned by the downstream API, or there is no open obligations for the period key, NOT_FOUND is returned.
The usecase is when an open return has been selected, so NOT_FOUND is an error.

| Field Name | Description                                        | Data Type | Mandatory/Optional | Notes                     |
|------------|----------------------------------------------------|-----------|--------------------|---------------------------|
| status     | The current obligation status                      | Enum      | Mandatory          | Open                      | 
| fromDate   | The date from which the period applies             | Date      | Mandatory          | YYYY-MM-DD                |
| toDate     | The date to which the period applies               | Date      | Mandatory          | YYYY-MM-DD                |
| dueDate    | The date the return is due to be filed and paid by | Date      | Mandatory          |                           |
| periodKey  | The period key of the obligation                   | String    | Mandatory          | YYAM (year, A, month A-L) |

**Response Body Examples**

***The obligation:***

```json
{
  "status": "Open",
  "fromDate": "2024-08-01",
  "toDate": "2024-08-31",
  "dueDate": "2024-09-10",
  "periodKey": "24AH"
}
```

### Responses
**Code**: `401 UNAUTHORIZED`
This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.

**Code**: `404 NOT_FOUND`
This response can occur when the alcoholDutyReference (appaId) is not found or the obligation is not open

**Code**: `500 INTERNAL_SERVER_ERROR`
This response can occur if the downstream query to the API fails or the response cannot be parsed