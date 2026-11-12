# E-Bookstore API — AI Specialist Capstone

Spring Boot + PostgreSQL backend for an online bookstore, built from the capstone wireframes:
browse a catalogue, fill a cart, check out with gift points, pay, see order history, buy again and
cancel within 48 hours.

| | |
|---|---|
| Stack | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL 16, springdoc-openapi |
| API docs | Swagger UI at <http://localhost:8080/swagger-ui.html> · spec in [docs/openapi.yaml](docs/openapi.yaml) |
| Design | [docs/data-model.md](docs/data-model.md): wireframe → entity mapping, ER diagram, business rules |
| Tests | 28 tests: unit tests (payment gateway, points rules) + MockMvc integration tests of the full journey on H2 |

## Customer journeys → endpoints

| # | Journey step (slide 3) | Endpoint |
|---|---|---|
| 1-2 | Login / authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/me` |
| 5 | Select category | `GET /api/categories` |
| 3 | Catalogue per category | `GET /api/categories/{id}/products`, `GET /api/products?categoryId=&brandId=&q=&minPrice=&maxPrice=&inStock=&sort=` |
| 6 | Browse brands | `GET /api/brands`, `GET /api/products?brandId=` |
| 7 | Product page, tentative delivery date, related products | `GET /api/products/{id}`, `GET /api/products/{id}/related` |
| 8 | Add to basket | `GET/DELETE /api/cart`, `POST /api/cart/items`, `PUT/DELETE /api/cart/items/{productId}` |
| 8 | Recommendations from order history | `GET /api/recommendations` |
| 9 | Delivery address | `GET/POST /api/addresses`, `PUT/DELETE /api/addresses/{id}` |
| 10 | Payment options, redeem gift points | `GET /api/checkout/preview?pointsToRedeem=`, `GET /api/payments/methods`, `GET /api/users/me/gift-points` |
| 10 | Place order | `POST /api/orders` `{addressId, pointsToRedeem}` |
| 11 | Payment & confirmation | `POST /api/orders/{id}/payments` `{method, card / upiId / bankCode}` |
| 12 | Purchase confirmation | `GET /api/orders/{id}` (includes `message`) |
| 4 | Order history, buy again | `GET /api/orders`, `POST /api/orders/{id}/buy-again` |
| 12 | Cancel within 48 h | `POST /api/orders/{id}/cancel` |
| — | Admin catalogue CRUD | `POST/PUT/DELETE /api/admin/products…`, `POST /api/admin/categories`, `POST /api/admin/brands` |

Errors share one JSON shape: `{timestamp, status, code, message, path, fieldErrors?}`. For example,
`422 CANCELLATION_WINDOW_EXPIRED`, `422 INSUFFICIENT_STOCK`, `409 ORDER_NOT_PAYABLE`, `400 VALIDATION_FAILED`.

## Run it locally

Prerequisites: JDK 21 and PostgreSQL 16. Maven comes from the bundled wrapper (`mvnw` / `mvnw.cmd`).

**1. Database.** Create a `bookstore` role and database (password `bookstore`):

```sql
CREATE ROLE bookstore LOGIN PASSWORD 'bookstore';
CREATE DATABASE bookstore OWNER bookstore;
```

Flyway creates the schema and loads 22 demo books on first start. Connection settings are overridable
with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` (see `application.properties`).

**2. Start the API**

```bash
cd bookstore-api
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

**3. Try it.** Open Swagger UI, call `POST /api/auth/login`, click **Authorize** and paste the `accessToken`.

| Demo user | Password | Notes |
|---|---|---|
| `reader@bookstore.local` | `Reader@123` | Customer with 200 gift points |
| `admin@bookstore.local` | `Admin@123` | ADMIN role (catalogue management) |

Test payment details: card `4111 1111 1111 1111` (approved), `4000 0000 0000 0002` (declined),
any future expiry and CVV; UPI `name@okbank` (approved) or `name@fail` (declined).

**4. Tests and smoke test**

```bash
cd bookstore-api && ./mvnw test           # 28 unit + integration tests (H2, no database needed)
python scripts/smoke_test.py              # full journey against the running API
```

### Front end (React)

`bookstore-ui` is a React 19 + TypeScript single-page app (Vite) that talks to the API. Screens: sign in /
register, catalogue (category and brand filters, search, sort, recommendations), product page (delivery date,
related books), cart, checkout (address, gift points, payment), order confirmation, order history, buy again
and cancel.

```bash
cd bookstore-ui
npm install
npm run dev          # http://localhost:5173 (the API must be running on :8080)
```

The dev server proxies `/api` to `http://localhost:8080` (see `vite.config.ts`), so no CORS setup is needed.
Sign in with the demo customer, which the login form prefills. `npm run build` type-checks and builds to `dist/`.

### This machine (portable tools)

The JDK 21, Maven 3.9.9 and PostgreSQL 16 used during development are unpacked, without admin rights, in
`%LOCALAPPDATA%\capstone-tools` (outside OneDrive). The helper scripts use them:

```powershell
.\scripts\db.ps1 start       # also: stop | status | reset | psql
.\scripts\run-app.ps1        # or: .\scripts\run-app.ps1 -Test
```

If PowerShell blocks local scripts, run `mvnw.cmd spring-boot:run` from `bookstore-api` instead, or allow
scripts for the current window only with `Set-ExecutionPolicy -Scope Process RemoteSigned`.
Delete `%LOCALAPPDATA%\capstone-tools` to remove everything afterwards.

## Project layout

```
bookstore-api/src/main/java/com/capstone/bookstore/
  config/          security, OpenAPI, clock, seed users, typed app properties
  security/        JWT issue/verify, auth filter, principal
  common/          error model + global exception handler, paging
  user/ address/   auth & profile, address book
  catalog/         categories, brands, products, search specs, admin CRUD
  cart/            cart & cart items
  order/           checkout, history, buy again, cancellation, confirmation messages
  payment/         payment records, simulated gateway (Luhn, expiry, decline hooks)
  points/          gift-point ledger, redeem/earn/refund/reverse
  recommendation/  order-history-based recommendations
bookstore-api/src/main/resources/db/migration/   V1 schema, V2 demo catalogue
bookstore-ui/      React + Vite front end (src/pages, src/components, api.ts, store.tsx)
docs/              data model, OpenAPI spec
scripts/           DB/app helpers, smoke test
```

## Design notes

- **Schema owned by Flyway** (`ddl-auto=validate`), so Hibernate checks the entities against it at startup.
- **No oversell:** stock is reserved with a conditional `UPDATE … WHERE stock >= qty` at checkout and released on cancel.
- **Snapshots:** order items keep their title and price, and orders keep the shipping address, so later catalogue or address edits don't change history.
- **Payments:** only the card brand and last 4 digits are stored; the CVV is never persisted or logged. The gateway sits behind the `PaymentGateway` interface, so a real provider can replace the simulator.
- **Time is injectable** (a `Clock` bean). Tests move the clock forward to check the 48-hour cancellation window and JWT expiry.
- **Ownership checks:** every customer query is scoped by the user id from the JWT, so another customer's order returns `404`.
