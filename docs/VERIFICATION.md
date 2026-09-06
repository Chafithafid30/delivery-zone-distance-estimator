# Verification record

Date: 6 September 2026.

## Automated checks

| Check | Result |
| --- | --- |
| Backend Java compilation | Passed on Java 17 |
| Maven verify and executable Spring Boot JAR packaging | Passed with Maven 3.9.9 |
| Backend tests | 47 passed; 0 failures, 0 errors, 0 skipped |
| Frontend unit tests | 3 passed in Vitest with React Testing Library and jsdom |
| Flyway migration and JPA schema validation | Passed against H2 in PostgreSQL compatibility mode |
| Frontend TypeScript checking and Vite production build | Passed on Node.js 24.19.0 |
| Compose YAML and referenced build contexts | Parsed and checked |
| Swarm stack configuration | Validated using Docker CLI 28.3.3 `docker stack config` |
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
- `RemoteAddressResolverTest`: 9 cases — cache-first behavior, normalized HTTP
  requests, metadata, malformed worker responses, late cache fallback and an
  unavailable worker.
- `SharedGeocodingApiTest`: 2 integration scenarios — two independent clients call
  a real local HTTP geocoder endpoint backed by H2, reuse one successful provider
  lookup, and validate worker input/profile isolation. Nominatim itself is mocked.
- `SwarmProfileTest`: 1 integration scenario — replicated backend selects the
  remote resolver, has no active public-provider adapter, serves delivery/instance
  endpoints, and does not expose the worker endpoint.

Tests mock interfaces using Mockito's subclass mock maker; no dynamic Java-agent
attachment is needed. Public Nominatim is never called by these tests.

The backend checks and build were rerun after the Clean Code and Swarm changes. The JSON
contract, database columns, cache behavior, and zone boundaries remain covered by
the existing integration and unit tests.

Frontend checks cover successful form submission/reset, preserving input and
showing server validation errors after a failed save, and ignoring a stale list
response after changing filters. These run in a simulated DOM, not a real browser.
