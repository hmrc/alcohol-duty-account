
# alcohol-duty-account

This is the backend microservice that provides account information for Alcohol Duty Service, e.g. financial data and obligations.

## Shuttering the service

This is done by setting features.bta-service-available in config to false. The service will then return 503 when attempting to get bta tile data

## API Endpoints

- [Get BTA tile data](api-docs/get-bta-tile-data.md): `GET /alcohol-duty-account/bta-tile-data`

## Running the service

> `sbt run`
The service runs on port `16002` by default.

## Running tests

### Unit tests

> `sbt test`
### Integration tests

> `sbt it/test`
## Scalafmt and Scalastyle

To check if all the scala files in the project are formatted correctly:
> `sbt scalafmtCheckAll`

To format all the scala files in the project correctly:
> `sbt scalafmtAll`

To check if there are any scalastyle errors, warnings or infos:
> `sbt scalastyle`

### All tests and checks
This is an sbt command alias specific to this project. It will run a scala format
check, run a scala style check, run unit tests, run integration tests and produce a coverage report:
> `sbt runAllChecks`

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
