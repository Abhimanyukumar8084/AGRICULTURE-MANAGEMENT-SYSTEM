# Digital Agriculture backend

Spring Boot 3 / Java 21 REST API for the static Digital Agriculture frontend.

## Requirements

- JDK 21 or newer
- Maven 3.9+

## Run locally

From this `backend` directory:

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. The development profile stores its H2 database in `backend/data/agriculture`; this local data directory is ignored by Git. Serve the project’s HTML files with a static web server such as VS Code Live Server. Development CORS allows localhost and 127.0.0.1 on any port. Do not open the HTML files using `file://`; browser credentials and cross-origin requests require an HTTP origin.

Register as a farmer, buyer, or agriculture expert at `register.html`, then sign in at `login.html` using the matching account type. Passwords must contain at least 12 characters. Admin accounts remain protected and cannot be created through public registration: bootstrap the initial administrator with environment variables.

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "use-a-unique-secret-of-at-least-12-characters"
$env:DATA_GOV_API_KEY = "<your data.gov.in API key>"
mvn spring-boot:run
```

The bootstrap administrator is created only when both values are supplied and no account already exists for the email. Do not use a sample password in a deployed environment.

## API overview

All request and response bodies use JSON. State-changing browser requests use session cookies and CSRF tokens; the frontend obtains a token from `GET /api/csrf` and sends it in `X-XSRF-TOKEN`, with credentials included on every request. To deploy the static HTML pages on another host, set `window.DIGITAL_AGRICULTURE_API` to the API base URL (ending in `/api`) before the page’s inline script runs.

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | Health check |
| GET | `/api/public/summary` | Public | Database-backed account, crop, and active listing totals |
| GET | `/api/csrf` | Public | Initialize/read CSRF token |
| POST | `/api/auth/register` | Public | Create a farmer, buyer, or expert account |
| POST | `/api/auth/password/forgot` | Public + CSRF | Request a password-reset token for a registered account |
| POST | `/api/auth/password/reset` | Public + CSRF | Update a password using a valid reset token |
| POST | `/api/auth/login` | Public + CSRF | Authenticate and establish a session |
| POST | `/api/auth/logout` | Signed-in + CSRF | End the session |
| GET | `/api/auth/me` | Signed-in | Current account |
| PATCH | `/api/auth/me` | Signed-in | Update profile and farm preferences |
| PATCH | `/api/auth/me/location` | Signed-in | Save browser GPS coordinates and location details |
| GET | `/api/weather/current` | Signed-in | Read the user’s most recently saved forecast |
| POST | `/api/weather/refresh` | Signed-in | Fetch live weather for saved coordinates and persist the snapshot |
| POST | `/api/contact` | Public + CSRF | Save a contact message |
| GET | `/api/records/marketplace` | Public | Browse active farmer listings |
| GET/POST | `/api/records?kind=CROP|ACTIVITY|LISTING|FEEDBACK` | Signed in | Manage your farm records |
| DELETE | `/api/records/{id}` | Owner or admin | Remove an owned record |
| POST | `/api/records/{id}/close` | Listing owner or admin | Close an active listing |
| PATCH | `/api/records/{id}/status` | Record owner or admin | Update an activity status |
| GET | `/api/crops`, `/api/crops/{id}` | Public | Browse crop reference data |
| GET | `/api/crops/admin` | Admin | Browse active and inactive crop records |
| POST/PUT/DELETE | `/api/crops` | Admin | Manage crop reference data |
| GET | `/api/consultations` | Farmer, expert, admin | List role-scoped consultations |
| POST | `/api/consultations` | Farmer | Request an expert consultation |
| PATCH | `/api/consultations/{id}/status` | Assigned expert or admin | Advance a consultation status |
| GET | `/api/admin/users` | Admin | Paginated account list |
| POST | `/api/admin/farmers` | Admin | Create a farmer account with location, crop, land, and status details |
| GET | `/api/market/prices?state=&district=` | Signed in | Read cached AGMARKNET prices from the database |
| POST | `/api/market/prices/refresh?state=&district=` | Admin | Fetch current provider records and persist them |
| GET | `/api/admin/summary` | Admin | Current platform counts for the admin dashboard |
| GET/PUT | `/api/admin/settings` | Admin | Read and save database-backed platform preferences |
| POST | `/api/admin/experts` | Admin | Provision an expert account |

Login JSON is `{ "email": "...", "password": "...", "accountType": "user|expert|admin" }`. Public registration accepts account types `farmer`, `buyer`, and `expert`; admin registration remains restricted.

The weather page requests browser location permission. When granted, it saves the device coordinates to the signed-in profile, asks the Java API to refresh Open-Meteo, and then displays the forecast snapshot stored in the database. If permission is denied, it refreshes using saved coordinates; if the provider is unavailable, it can display that user’s prior saved snapshot. Without a location or a saved forecast it shows an explicit error, not sample values. Browser geolocation requires HTTPS or localhost. Set the user’s state and district in Profile Settings to filter mandi records to their local region. Mandi prices are fetched from the configured data.gov.in AGMARKNET resource only when an administrator selects **Refresh provider data**. The resulting source records are stored in the database and shown with their provider arrival date and retrieval time. Set `DATA_GOV_API_KEY` in the backend process environment; never put the key in frontend code.

## Production database

Set `SPRING_PROFILES_ACTIVE=prod`, `DATABASE_URL` (a JDBC PostgreSQL URL), `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and `SESSION_COOKIE_SECURE=true`. Configure `CORS_ALLOWED_ORIGINS` with the exact HTTPS origins that host the frontend. Prefer serving the frontend and API on the same site; if they must be cross-site, set `SESSION_COOKIE_SAME_SITE=none` as well as `SESSION_COOKIE_SECURE=true` and account for browser third-party-cookie restrictions. Apply request rate limits at the production gateway for public registration, login, and contact endpoints. Flyway applies the versioned schema migration at startup; Hibernate validates the schema and does not auto-update it in production.

Run tests with:

```powershell
mvn test
```
