# Verification record

Date: 6 September 2026.

## Automated checks

| Check | Result |
| --- | --- |
| Backend Java compilation | Passed on Java 17 |
| Maven verify and executable Spring Boot JAR packaging | Passed with Maven 3.9.9 |
| Backend tests | 35 passed; 0 failures, 0 errors, 0 skipped |
| Flyway migration and JPA schema validation | Passed against H2 in PostgreSQL compatibility mode |
| Frontend TypeScript checking and Vite production build | Passed on Node.js 24.19.0 |
| Compose YAML and referenced build contexts | Parsed and checked |
| Git whitespace check | Passed |

Test coverage by class:

- `HaversineTest`: 6 cases — zero distance, known arc length, antipodes,
  international date line, symmetry and coordinate validation.
- `ZoneTest`: 8 cases — values immediately around 50/300 km and invalid distances.
- `NominatimClientTest`: 12 cases — valid response, User-Agent, query encoding,
  empty results, HTTP failures and malformed/invalid responses using a local stub.
- `GeocodingServiceTest`: 7 cases — cached data without API calls, cache insertion,
  outage/cooldown, empty results, failure fallback, concurrent identical requests,
  and global interval for different addresses.
- `DeliveryApiTest`: 2 integration scenarios — CRUD, cache reuse, combined filters,
  provider outage, UNKNOWN persistence, explicit recovery, status-only edits,
  clearing old coordinates after changing to an unresolved address, validation
  and 404 behavior.

Tests mock interfaces using Mockito's subclass mock maker; no dynamic Java-agent
attachment is needed. Public Nominatim is never called by these tests.

## Not executed in this environment

- Full-stack `docker compose up`: Docker is unavailable here. Compose configuration
  was inspected, but container build/startup is not claimed as verified.
- A real PostgreSQL instance: integration tests use H2; actual PostgreSQL startup,
  schema validation and queries should be checked through Compose before submission.
- Browser interaction, layout and accessibility testing: frontend build/type checks
  do not prove the complete browser experience. Use the demo checklist locally.
- Live Nominatim resolution: the HTTP adapter was verified with a local stub,
  so current public-service availability/address coverage is not claimed.
- GitHub Actions: workflow is included but has not run on GitHub yet.

The project is not deployed and has not been pushed to GitHub from this environment.
Use the included Git bundle to retain its actual incremental history when publishing.
