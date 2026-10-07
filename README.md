# Qred Payment Backend

Small Spring Boot payment API for the Qred Java developer assignment.

## What is implemented

- `POST /api/payments` — create a payment for a client and contract
- `GET /api/payments?clientNumber=...` — fetch payments for a client
- `GET /api/payments?clientNumber=...&contractNumber=...` — fetch payments matching both

The Java side includes:

- JPA entities for `clients`, `contracts`, and `payments`
- Flyway migration for the schema
- Validation and API error handling
- ID-based lookup for payment reads (`clientNumber` / `contractNumber` are resolved once, then payments are queried by foreign keys)
- DTO projection for payment fetches to avoid loading full entity graphs on the read path
- Composite indexes supporting the payment read queries
- Service, repository, and controller tests

## Notes on scalability

- The fetch endpoint currently returns all matching payments without pagination.
- For this assignment I kept the API simple and non-paginated.
- A natural next improvement would be pagination with page-size limits for large datasets.

## Run locally

```bash
./gradlew test
./gradlew bootRun
```

By default the app starts with an embedded H2 database, so no external Postgres instance is required for local testing. You can override the datasource with environment variables if you want to use PostgreSQL.

## Notes on AI-assisted work

I used AI-assisted tooling during the implementation to help outline the design, scaffold the Spring Boot layers, and review the test structure, write features and tests.

