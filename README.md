# Todos. — The Daily Ledger

[![CI](https://github.com/Th1f/todo-app/actions/workflows/ci.yml/badge.svg)](https://github.com/Th1f/todo-app/actions/workflows/ci.yml)
[![Backend Tests](https://img.shields.io/badge/backend%20tests-73%20passing-brightgreen)](https://github.com/Th1f/todo-app/actions/workflows/ci.yml)
[![Frontend Tests](https://img.shields.io/badge/frontend%20tests-93%20passing-brightgreen)](https://github.com/Th1f/todo-app/actions/workflows/ci.yml)

A multi-user todo application with per-user categories, session authentication, and a full-stack test suite. Editorial-styled UI backed by a Spring Boot REST API and PostgreSQL.

---

## Demo & Snippets

**Hosted:** _<!-- TODO: paste your Railway URL here -->_

<!-- TODO: replace with real screenshots -->

| Screen | Description |
| ------ | ----------- |
| _Sign in_ | Username/password form that doubles as registration |
| _Ledger_ | Task list, category tabs, live completion summary |
| _Inline edit_ | Double-click any task or category to rename it in place |


---

## Requirements / Purpose

### MVP

- Register an account and sign in
- Create, rename, complete, and delete tasks
- Group tasks into colour-coded categories that each user owns
- Filter the list by category and see per-category completion progress
- Every user sees only their own data

### Purpose

I wanted to build a complete full-stack application end to end rather than another frontend against a fake API — real persistence, real authentication, real deployment, and a test suite that would catch a regression before a user did. The todo domain is deliberately small so the effort could go into the parts I hadn't done before: JPA relationships, Spring Security, per-user data isolation, and testing both halves of the stack.

### Stack

| Layer | Choice | Why |
| ----- | ------ | --- |
| Frontend | React 19 + TypeScript | Types caught a whole class of bug at the API boundary — the `Task`/`Category` interfaces are the contract |
| Build | Vite 8 | Instant HMR, and Vitest reuses the same config, so there is no second toolchain to maintain |
| Styling | Tailwind CSS v4 | Theme tokens (`--color-accent`, `--color-muted`) in one `@theme` block keep the editorial look consistent |
| State | React Context | Two providers cover the whole app; Redux would have been ceremony for this much state |
| Backend | Spring Boot 4.1 | Wanted to learn the Java ecosystem properly, and Spring Security gives session auth without hand-rolling it |
| Persistence | Spring Data JPA + PostgreSQL | Derived query methods (`findByIdAndOwnerUsername`) make ownership scoping declarative rather than something you remember to write |
| API docs | springdoc-openapi | Swagger UI was how I debugged a 401 that turned out to be a wrong `@RequestMapping` path |
| Testing | JUnit 5 + Mockito + Vitest + Testing Library | 166 tests total across both halves |
| Deployment | Docker → Railway | One multi-stage image serves the API and the built frontend from a single origin |

---

## Build Steps

### Prerequisites

- Java 17
- Node 20+
- PostgreSQL 16+ (18 was used in development)

### 1. Database

```bash
psql -U postgres -c "CREATE DATABASE todoapp;"
```

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8080` with the `dev` profile, which seeds a demo user and sample tasks. Override the defaults with environment variables if your Postgres differs:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/todoapp \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=yourpassword \
./mvnw spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and proxies `/api` to port 8080, so the browser sees one origin in development just as it does in production.

### Tests

```bash
cd backend  && ./mvnw test              # 73 tests
cd frontend && npm test                 # 93 tests
```

Coverage reports:

```bash
cd backend  && ./mvnw test              # target/site/jacoco/index.html
cd frontend && npm run test:coverage    # coverage/index.html
```

The backend suite runs entirely on in-memory H2 — no PostgreSQL required.

### Production image

```bash
docker build -t todo-app .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/todoapp \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=yourpassword \
  todo-app
```

---

## Design Goals / Approach

**One origin, one deployable.** The Dockerfile builds the frontend, copies `dist/` into `src/main/resources/static`, and packages both into a single jar. This removes CORS entirely, lets the session cookie stay `SameSite=Strict`, and means one deploy instead of two. It also means the frontend calls `/api` with a relative path and never needs to know a backend URL.

**Ownership enforced at the query, not in an `if`.** Every lookup is scoped by owner in the repository method itself — `findByIdAndOwnerUsername(id, auth.getName())` rather than `findById(id)` followed by a check. There is no code path where you can fetch a row and forget to verify who owns it. Requests for another user's data return **404, not 403**, so the API never confirms that a resource exists.

**The session is the only source of identity.** No endpoint accepts a user id from the client. `auth.getName()` comes from the Spring Security context, so the request body cannot influence which account is touched.

**Partial updates mean partial.** `TaskUpdate` uses boxed `Boolean` rather than primitive `boolean`, so a missing field is `null` ("leave alone") instead of defaulting to `false`. With a primitive, renaming a task would silently un-complete it.

**Optimistic UI where it's safe.** Toggling a checkbox flips state immediately and reconciles with the server response, rolling back on failure. Rename and delete wait for the server, because guessing wrong there is more disruptive than a brief pause.

**Categories are per-user, with a composite constraint.** `UNIQUE (owner_id, name)` rather than a global unique name, so two users can both have "Work". New accounts are seeded with Work / Personal / Errands from `CategoryDefaults`, which is also what the dev seeder uses — one source of truth for the presets.

**Tests that fail for the right reason.** Mockito returns `Optional.empty()` and empty lists by default, so a test asserting "the other user sees nothing" can pass against a completely broken endpoint. Every ownership test therefore stubs the *unscoped* query to succeed as a tripwire — swap `findByIdAndOwnerUsername` for `findById` and the suite goes red immediately. This was verified by deliberately introducing that mutation.

---

## Features

### Accounts

- Registration with BCrypt-hashed passwords and an 8-character minimum
- Form login with an HTTP-only, `SameSite=Strict` session cookie
- Three starter categories created automatically on registration
- Logout that invalidates the session server-side

### Tasks

- Create with a name and category
- Toggle complete with optimistic UI and rollback on failure
- Rename inline — double-click the label or use the ✎ button; Enter saves, Escape discards
- Delete with a confirmation prompt
- Filter by category, or view all sorted by category name
- Running "x of y done" counter in the header

### Categories

- Create with a colour picked from six swatches or typed as any hex value
- Rename inline, with the active filter following the rename
- Delete, blocked with a clear message while the category still holds tasks
- Duplicate names rejected per-user with a friendly message, not a stack trace
- Per-category progress bars, exposed to assistive tech via `role="progressbar"`

### Interface

- Loading skeletons that mirror the real layout for the page, task list, category tabs, summary, and header counts
- Keyboard-driven inline editing throughout
- Accessible labels on every icon button (`Rename Buy milk`, `Delete Work`)
- Responsive layout from mobile to desktop

### API

- OpenAPI 3 spec at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`
- Consistent status codes: 201 create, 204 delete, 400 validation, 401 unauthenticated, 404 not-yours, 409 conflict

---

## Known issues

- **Session expiry is handled poorly.** When the cookie expires, the next action surfaces a raw 401 error string instead of redirecting to the login screen. A page refresh fixes it.
`NewCategory` handles this correctly; `NewTask` should match it.
- **`ddl-auto=update` in production.** Hibernate manages the schema, which will not survive a destructive change — during development, adding a `NOT NULL owner_id` to a populated table required dropping it manually. Needs Flyway.
- **No pagination.** Every task is fetched on load. Fine at this scale, not at a thousand.

---

## Future Goals

- **Flyway migrations** to replace `ddl-auto=update` — the single most important remaining fix
- **A 401 interceptor** in `client.ts` that clears auth state and routes to login, fixing the session-expiry experience
- **Provider tests** for `TaskProvider` and `AuthProvider`, currently ~4% covered and holding the optimistic-toggle rollback logic
- **Drag to reorder** tasks, with a persisted sort order
- **Due dates and overdue highlighting**
- **Rate limiting** on `/api/login` and `/api/register`
- **Remember-me tokens** so sessions survive a server restart
- **Deploy previews** — extend the CI workflow to build a Railway preview environment per pull request

---

## Change logs

### 10/08/2026 — Frontend shell and the first API

Built the React UI from scratch: header with live counts, category tabs, task list, new-task form, and the summary column with per-category progress bars. Worked through TypeScript select handling, Tailwind v4 theme tokens, and circular checkboxes. Hit a bug where nothing rendered because `Tasks.tsx` returned early during render when `selectedCategory` was still `""` — fixed by deriving `visibleTasks` during render instead of guarding. Then started the backend from an empty Spring Initializr project: `Task` entity, `TaskRepository`, `TaskController` with full CRUD, and PostgreSQL wired up on port 5432.

### 11/08/2026 — Integration, authentication, and deployment

Connected the frontend to the real API and deleted the mock JSON. Added `Category` as a proper entity with a `@ManyToOne` from `Task`, plus category create/rename/delete UI. Built authentication end to end: `User` entity, `UserDetailsService`, BCrypt, form login with session cookies, and a `SecurityConfig` filter chain. Added `owner` to both `Task` and `Category` and rewrote every query to be owner-scoped, so one user cannot touch another's data. Two long debugging sessions: a 401 on `/api/register` that turned out to be a missing `/api` prefix on `@RequestMapping` (found via `/v3/api-docs`), and validation errors returning 401 instead of 400 because the forward to `/error` required authentication. Finished with a multi-stage Dockerfile serving the built frontend from the Spring jar, and env-var-driven configuration for Railway.

### 12/08/2026 — Testing, skeletons, and two real bugs

Wrote the backend test suite: 73 tests across `TaskController`, `CategoryController`, `AuthController`, a login/session test, and `@DataJpaTest` repository tests on H2. Learned the hard way that Mockito's default return values can make a security test pass against broken code, and adopted the tripwire pattern — stub the *unscoped* query to succeed so the test fails if ownership scoping is removed. Verified it by deliberately breaking `CategoryController.delete` and watching the right test go red. Both controllers now sit at 100% instruction and branch coverage. Added `@ActiveProfiles("test")` so the suite runs on in-memory H2 instead of my local Postgres, which was blocking CI.

Then the frontend: Vitest plus Testing Library, 93 tests over the API client, all four API modules, and every component. The API tests immediately found two real bugs in `register` — the request body used the key `lower` instead of `username` (so every registration failed validation), and a missing `return` meant the promise never reached the caller, so `try`/`catch` around it could never fire. Finished by replacing the bare "Loading…" text with proper skeleton components for the page, task list, category tabs, summary, and header counts, and fixed a deploy failure caused by an unused import — `tsc -b` type-checks test files as part of the production build.

---

## What did you struggle with?

**Jackson emitting `done` and `isDone` as separate fields.** *What:* the API returned both keys for the same value. *Why:* Lombok generates `isDone()` for a boolean field named `isDone`, and Jackson counted the field and the getter as two distinct properties. *How:* renamed the field to `done` so `isDone()`/`setDone()` pair with it as one property, then used `@JsonProperty("isDone")` to control the wire format and `@Column(name = "is_done")` for the column.

**Spring Data JPA's method-name conventions for derived queries.** *What:* repository methods that read perfectly well as English refused to work — some failed to resolve at all, others quietly queried the wrong thing. *Why:* the method name **is** the query, so every word after `findBy` has to match a real property path, and the parser resolves greedily from the longest match. `findByOwnerUsername` looks like one property but is actually a traversal: `Task.owner` → `User.username`. `findByCategoryNameIgnoreCase` walks `Task.category` → `Category.name`, and `IgnoreCase` is a keyword that has to sit immediately after the property it modifies — `findByCategoryIgnoreCaseName` is not the same thing. Getting a name wrong doesn't fail at compile time either; Spring builds the implementation at startup, so a typo surfaces as `PropertyReferenceException: No property 'usernme' found` when the whole context refuses to boot. *How:* I stopped inventing names and started reading them as property paths, checking each segment against the entity before writing the method. That gave me the four the app relies on: `findByIdAndOwnerUsername` for ownership-scoped lookups, `findByOwnerUsernameAndCategoryNameIgnoreCase` for the category filter, `existsByOwnerUsernameAndNameIgnoreCase` for the per-user duplicate check, and `countByCategoryId` to block deleting a category that still holds tasks. I also learned where the convention stops paying: once a name gets long enough to be unreadable, `@Query` with explicit JPQL is the better choice. The `@DataJpaTest` suite exists partly because of this — mocked repositories will happily pretend a misnamed method works, so those tests run the real derived queries against H2 and prove they filter the way the name claims.

**Debugging registration through three wrong hypotheses.** *What:* registration returned 401, then 400, then appeared to 409 on a fresh username. *Why:* three separate causes stacked — a missing `/api` prefix on `@RequestMapping`, `/error` requiring authentication so validation failures came back as 401, and `response.json()` throwing on the empty body of a 201. *How:* `/v3/api-docs` to see the paths Spring had actually registered, then reading the real response instead of assuming the status code meant what I expected.


---

## Licensing Details

Released under the [MIT License](LICENSE).

---

## Further details, related projects, reimplementations

- **The API and the client live together.** The Spring Boot backend serves the React frontend as static assets from the same jar, so there is no separate client repository to link. `backend/` and `frontend/` in this repo are the two halves.
- **API documentation** is generated from the code and available at `/swagger-ui.html` on any running instance.
