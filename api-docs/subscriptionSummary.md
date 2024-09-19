# Get Subscription Data Summary


Returns the Alcohol Duty data subscription data used to determine if the user can or should fill in a return, and which regimes they are subscribed for.
Calls to this API must be made by an authenticated and authorised user with an ADR enrolment in order for the data to be returned.

**URL**: `/alcohol-duty-account/subscriptionSummary/:alcoholDutyReference`

**Method**: `GET`

**URL Params**

| Parameter Name       | Type   | Description    | Notes                     |
|----------------------|--------|----------------|---------------------------|
| alcoholDutyReference | String | The appa Id    |                           |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Responses

### Success response

**Code**: `200 OK`

**Response Body**

| Field Name                            | Description                             | Data Type  | Mandatory/Optional  | Notes                                                          |
|---------------------------------------|-----------------------------------------|------------|---------------------|----------------------------------------------------------------|
| approvalStatus                        | The current approval status of the user | Enum       | Mandatory           | Approved, Insolvent, DeRegistered, Revoked, SmallCiderProducer |
| regimes                               | The regimes the user is subscribed for  | Set(Enum)  | Mandatory           | Beer, Cider, Wine, Spirits, OtherFermentedProduct              |

**Response Body Examples**

***Where the subscriber's status is approved and all regimes are subscribed to:***

```json
{
  "approvalStatus": "Approved",
  "regimes": [
    "Spirits",
    "Wine",
    "Cider",
    "OtherFermentedProduct",
    "Beer"
  ]
}
```

**Status prioritisation**

The status prioritisation will be as below:

1. DeRegistered - Revoked
1. SmallCiderProducer
1. Approved - Insolvent

### Responses
**Code**: `400 BAD_REQUEST`
This response can occur when the downstream query to the API returns BAD_REQUEST

**Code**: `401 UNAUTHORIZED`
This response can occur when a call is made by any consumer without an authorized session that has an ADR enrolment.

**Code**: `404 NOT_FOUND`
This response can occur when the alcoholDutyReference (appaId) is not found

**Code**: `500 INTERNAL_SERVER_ERROR`
This response can occur if the downstream query to the API fails or the response cannot be parsed