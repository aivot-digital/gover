# Prosuna Project Instructions

## Project Context

- Prosuna is an open-source platform for end-to-end digitized administrative processes: data intake, process modeling, case handling, and internal or external participation, with manual and automated steps.
- Preserve traceability across responsibilities, inquiries, evidence, decisions, and processing steps. Prosuna integrates with existing IT landscapes; it does not aim to replace every specialized system.

## Repository Structure

- `app/` contains the React/Vite frontend. It is built in separate staff and customer modes.
- `backend/` contains the Spring Boot backend and its Maven build.
- `backend/mails/` contains the mail templates and their Node.js build.
- `development/` contains the local development environment and setup documentation.
- `default-assets/` contains assets shipped with Prosuna installations.
- Keep documentation consistent with build configuration and CI. Use executable configuration for currently effective versions and commands; flag discrepancies and verify the intended change.

## Working Agreements

- Inspect the surrounding code and follow established project patterns before introducing new abstractions.
- Keep changes scoped to the requested behavior. Do not include unrelated refactoring or formatting changes.
- Preserve unrelated changes already present in the working tree and never revert them without explicit instruction.
- Do not add or upgrade dependencies unless the requested change requires it. Explain any such dependency change in the final response.

## Git Workflow

- Follow the [Aivot contribution guidelines](https://github.com/aivot-digital/.github/blob/main/docs/CONTRIBUTING.md) when asked to branch, commit, or prepare a pull request. Do not switch branches, commit, or publish changes merely because files were edited.
- New features use `feature/<descriptive-label>` from the current `milestone/X.Y.Z`; bug fixes use `fix/<descriptive-label>` from the current `patch/X.Y.Z`. Target the corresponding base branch with the PR, not `main`. Verify the intended release line instead of guessing it; no direct commits to `main`.
- Write English commit headers as `type(scope): Subject`, with optional scope, a capitalized imperative subject, and no final period. Types: `build`, `ci`, `docs`, `feat`, `fix`, `ref`, `test`, `meta`; use `revert` for reversals. Example: `fix(permissions): Restrict team updates to authorized resources`.
- If needed, separate the body with a blank line, explain what and why in imperative form, and wrap it at 80 characters. Put breaking-change details and applicable issue references in the footer; never invent issue or task IDs. Consult the contribution guidelines for PR requirements and revert formatting.

## Development Runtime

- Do not start or keep development servers, preview servers, Docker Compose stacks, or other long-running development services running unless explicitly requested by the developer; the developer owns their lifecycle.
- Finite verification commands such as tests, type checks, and production builds may be run as needed.
- If visual or runtime verification requires a running application, state what should be verified and which command the developer can run.

## Code Comments

- Comment next to code to explain non-obvious intent, domain rules, invariants, security decisions, compatibility constraints, or workarounds, not straightforward implementation. Update or remove comments when that behavior changes.

## Roles and Permissions

- Authorize through permission keys, not hard-coded role names or assumptions about administrators. System roles and domain roles grant those permissions.
- `UserRoleEntity`, `UserRoleService`, the `user-roles` frontend module, and `/api/user-roles/` represent domain roles. Do not introduce a separate "user role" concept or rename this terminology in unrelated changes.
- System roles grant permissions globally; domain roles grant scoped permissions through department or team memberships. Deputies and explicit process or process-instance access can contribute further grants. Reuse existing projections, repositories, and services to derive effective permissions.
- A system permission overrides scope restrictions for the same key in department, team, process, and process-instance checks, in backend `has*`/`require*` methods and frontend helpers. Resource-grant list repositories may omit system grants; callers must handle global access separately.
- Use the narrowest applicable `PermissionService` check. `hasInAny*`/`requireInAny*` means at least one resource, not all; restrict list queries to authorized IDs unless the user has the system permission. Example: with only `team.update` for team 17, updating team 18 must fail; a system-level `team.update` authorizes both permission checks.
- Backend enforcement is the security boundary. Prefer permission checks in services before protected operations; controller checks are a justified exception, not the default. Keep all entry paths protected and scoped queries restricted. Frontend checks must use the shared hooks and helpers in `app/src/modules/permissions/` and match the backend scope and resource ID.
- Define keys as constants and `PermissionEntry` values in the owning `*PermissionProvider`; use the constants for backend checks. All permissions are system-role assignable. Domain-role assignment requires `supportsDomainRoleAssignment()` and may exclude individual keys. Decide and test assignability when adding or changing permissions; see [PermissionProviderTest](backend/src/test/java/de/aivot/prosuna/backend/permissions/models/PermissionProviderTest.java) for provider serialization and assignment metadata examples.
- `app/src/data/permissions/permission.ts` is generated from the backend permission providers. Never edit it manually; run `npm run generate:permissions` from `app/` after changing provider permissions and include the generated result.
- After a mutation that can change the current user's effective access, refresh the frontend permission set with the established `useRefreshPermissionSet` flow and broadcast the invalidation when other open tabs can be affected.
- Cover authorization changes with focused tests for the allowed and denied cases. For scoped permissions, also cover the system-level override, the correct resource grant, and rejection for a different resource or scope.

## UI Copy

- Write product-authored German UI text with correct umlauts and `ß`, not ASCII transliterations. Use respectful `Sie` address unless the affected customer experience explicitly configures a different form.
- Use gender-inclusive wording: prefer natural, semantically accurate neutral terms; otherwise follow the established colon style, such as `Mitarbeiter:innen`. Preserve domain terminology and distinguish roles from permissions.
- For writing or reviewing UI text, validation and error messages shown to users, or product emails, read the [Prosuna UI Copy skill](.agents/skills/prosuna-ui-copy/SKILL.md). Keep copy changes scoped; do not rewrite customer-authored content or rename technical identifiers.

## Frontend Code Conventions

- Place reusable UI and cross-feature building blocks in `app/src/components/`. Keep domain-specific functionality in the corresponding `app/src/modules/<domain>/` area and shell-specific behavior in `app/src/shells/customer/` or `app/src/shells/staff/`.
- Use kebab-case for new frontend file and directory names, PascalCase for React components and exported types, and named exports for new code. Avoid introducing new default exports unless a framework integration requires one.
- Build ordinary application UI with the existing MUI components and the established `sx` or `styled` approach. Use icons from `@aivot/mui-material-symbols-400-n25-outlined`; do not introduce a second general-purpose icon or styling system. Follow the neighboring implementation when a specialized surface already uses a different established mechanism.
- Reuse the shared `FormField` and `FormFieldGroup` abstractions and the form-field theme tokens for standard form controls where they fit. Preserve the established semantics for labels, hints, errors, required state, disabled state, read-only state, busy state, and ARIA relationships in both shared and specialized controls.
- Route calls to the Prosuna backend through the API service abstraction already used by the affected module, normally the `BaseApiService`, `BaseReadApiService`, or `BaseCrudApiService` family. Use direct `fetch` only for established exceptions such as external resources, static assets, or authentication-specific flows.
- Colocate Vitest/Testing Library tests using `.spec.ts`, `.spec.tsx`, or `.integration.spec.tsx`. Assert roles, accessible names, descriptions, and observable behavior; see [CheckboxFieldComponent tests](app/src/components/checkbox-field/checkbox-field-component.spec.tsx) for accessible label, error, and busy-state examples.

## Frontend Viewport Support

- Customer UI supports mobile and desktop. Staff UI is desktop-only with a minimum viewport of 1280 x 720; do not implement below-minimum layouts unless requested.
- When visual verification is available, check staff changes at 1280 x 720 and a larger desktop viewport, and customer changes at relevant mobile and desktop sizes.
- Check which shells use shared components before applying viewport assumptions; preserve mobile usability wherever the customer UI uses them.

## Backend Code Conventions

- Organize backend code by domain under `backend/src/main/java/de/aivot/prosuna/backend/<domain>/`, then use established subpackages such as `controllers`, `services`, `entities`, `dtos`, `repositories`, `filters`, `permissions`, and `models`.
- Use constructor injection for Spring dependencies and keep injected dependency fields `final`.
- Declare Java nullability explicitly with `jakarta.annotation.Nonnull` and `jakarta.annotation.Nullable` where values cross method, entity, service, or API boundaries.
- Prefer Java records for new request and response DTOs unless mutability, inheritance, or framework behavior requires a class.
- Before implementing conventional read or CRUD behavior, check whether the existing `GenericReadController`, `GenericCrudController`, `ReadEntityService`, or `EntityService` abstractions fit the use case.
- Use `ResponseException` and its factory methods for expected API-facing failures. Preserve meaningful, user-facing error messages and do not expose raw internal exceptions to clients.
- Keep public, citizen/customer, and staff API boundaries explicit. Follow the existing controller separation and `/api/public/` namespace, authentication model, and trailing-slash URL convention.
- Treat audit logging as part of operation behavior. Prefer `ScopedAuditService` events in services; use controllers only where justified. Preserve auditable staff-side mutations without duplicate events or success events for failed operations, and test changed audit behavior. This service-first policy also applies in [development/GUIDELINES.md](development/GUIDELINES.md); do not relocate unrelated existing checks or logging.
- Document new or changed endpoints with the existing OpenAPI `@Tag`, `@Operation`, and security annotations used by neighboring controllers.
- Apply database schema changes through a Flyway migration in `backend/src/main/resources/db/migration/` using the established `V<version>__<description>.sql` naming pattern, and keep the migration and JPA model changes aligned.
- Treat migrations up to and including `V8_0_0` as immutable production history. While working on the current development line, migrations with versions greater than `8.0.0` have not been used in production and may be edited, consolidated, replaced, or removed when that produces a clearer migration history. Development databases may be recreated; preserving their data or migration checksums is not required. Revisit this branch-specific exception before releasing any migration above `8.0.0` to production.
- `development/examples/*/` contains the ordered SQL example datasets used to populate newly created example databases. When changing the database schema, constraints, required data, or entity relationships, inspect every affected example dataset and update it where necessary. Keep all example datasets consistent with the final schema and loadable in numeric file order; a Flyway migration for existing databases does not replace this check.
- Place backend tests in the matching package under `backend/src/test/java/` and use the existing JUnit 5 and Mockito patterns.

## Verification

- Run the smallest relevant checks first and broaden verification according to the change's risk and scope.
- Frontend tests: run `npm test` from `app/`; select a test file with `npm test -- <path-to-test>`.
- Frontend type checking: run `npm run typecheck` from `app/`.
- Staff frontend build: run `npm run build:staff` from `app/` when the staff build is affected.
- Customer frontend build: run `npm run build:customer` from `app/` when the customer build is affected.
- Backend tests: run `mvn --batch-mode test` from `backend/`; select a class with `mvn --batch-mode -Dtest=<TestClass> test`.
- Mail template build: run `npm run build:dev` from `backend/mails/` when mail templates are affected.
- Add or update tests when behavior changes.
- Report checks that could not be run instead of claiming successful verification.
- Review the final diff for regressions, accidental scope expansion, and unrelated changes.
