# AGENTS.md

## Project shape
- Maven multi-module: `xxl-job-core` (executor/client lib), `xxl-job-admin` (Spring Boot 3 scheduling center), `xxl-job-executor-samples` (Spring Boot + frameless examples).
- Group/version: `io.github.wuwen5.xxl-job` / `2.5.3-SNAPSHOT` (in `pom.xml`, do not edit per-module).
- Compatibility target is xxl-job 2.5.0; refactor preserves handler / DB / API conventions from that version.

## Dual JDK strategy (non-obvious, do not unify)
- `xxl-job-core` is **JDK 8** (`maven.compiler.source/target=1.8`).
- `xxl-job-admin` and `xxl-job-executor-samples` are **JDK 17** (Spring Boot 3.5 / Jakarta EE).
- Do not introduce Java 9+ APIs into `xxl-job-core` (no `List.of`, no `var`, no `jakarta.*`).
- CI matrix runs JDK 17 and 21 on Ubuntu; local minimum is JDK 17 to build the admin module. JDK 8 toolchain is not required locally — the compiler plugin targets 1.8 bytecode.

## Build & verification
- Use the wrapper: `./mvnw -B clean verify --file pom.xml`. Do not call a system `mvn` — versions are pinned in `pom.xml`.
- Default Maven build runs **Spotless check** (palantir-java-format) at `process-sources`; format errors fail the build. Auto-fix with `./mvnw spotless:apply`.
- Surefire dumps output to files; Allure results go to `../target/allure-results` at each module.
- JaCoCo coverage is collected on every `verify`; SonarCloud consumes it via `sonarcloud.yml` workflow.
- Release build (Maven Central) uses `-P release -DperformRelease=true`; that profile only builds `xxl-job-core` and signs with GPG, skips tests, and publishes via `central-publishing-maven-plugin`. Do not run it locally without GPG configured.

## Running tests
- Unit tests only: `./mvnw -pl xxl-job-core,xxl-job-admin test`.
- Single class: `./mvnw -pl xxl-job-admin -Dtest=ClassName test`.
- PostgreSQL DAO tests under `xxl-job-admin/src/test/java/com/xxl/job/admin/dao/pg/*` and `AbstractPostgreSQLTest` start a Testcontainers `postgres:16-alpine` — Docker must be running.
- H2-backed slice tests use the `test` profile (`application-test.properties`, `schema.sql`).
- E2E tests are **not** part of Maven — they live in `e2e/` (Playwright 1.44) and run via `docker compose -f docker-compose-e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests`. The compose stack builds `xxl-job-admin` and `xxl-job-executor-sample-springboot` images, starts MySQL with `doc/db/tables_xxl_job.sql` seeded, and runs `npx playwright test` against `http://xxl-job-admin:8080`. Maven build step in `.github/workflows/e2e.yml` uses `-Dmaven.test.skip=true`; the e2e pipeline and CI pipeline are independent.

## Runtime defaults to know
- Admin web port: `8080`, context path `/xxl-job-admin`. Actuator on `9001` (probes enabled, only `health,info` exposed). E2E healthcheck polls `/actuator/health/readiness`.
- Default login: `admin / 123456` (seeded by the SQL init script and asserted in `e2e/tests/example.spec.ts`).
- Default MySQL JDBC URL hardcoded in `application.properties` points at `127.0.0.1:3306`; override via `SPRING_DATASOURCE_URL` etc. in Docker / env.
- Multi-DB schemas live in `doc/db/`: `tables_xxl_job.sql` (MySQL), `tables_xxl_job_pg.sql`, `tables_xxl_job_oracle.sql`, `tables_xxl_job_dm.sql` (Dameng). Test schemas: `xxl-job-admin/src/test/resources/schema.sql` (H2/MySQL mode) and `schema-postgresql.sql`.

## Style & conventions
- Code is palantir-java-format 2.38.0 (4-space indent, 120-col, no tabs) — re-run `spotless:apply` after edits; do not hand-format.
- Lombok is used widely (`@Slf4j` style is **not** in use — manual `Logger` fields appear in samples and core; check neighboring files before introducing `@Slf4j`).
- `xxl-job-core` package root: `com.xxl.job.core.*`. `xxl-job-admin` package root: `com.xxl.job.admin.*`. Do not move classes across these roots.
- `.gitattributes` marks `*.js`, `*.css`, `*.html`, `*.ftl` as Java for linguist (intentional — they are Freemarker/JS templates inside the admin).
- Commit `pom.xml.versionsBackup` files appear locally after `mvn versions:set`; they are not committed (not in `.gitignore` but the release workflow runs `versions:commit` to remove them).
- `applogs/` and `logs/` at repo root are runtime output dirs, not source.

## Release / deploy
- `release.yml` is `workflow_dispatch` only. Required inputs: `release_version`, `next_snapshot`. Required secrets: `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `MAVEN_CENTRAL_TOKEN`, `MAVEN_USERNAME`, `SONAR_TOKEN`, `CODECOV_TOKEN`.
- `xxl-job-admin` and `xxl-job-executor-samples` set `skip_maven_deploy=true`; only `xxl-job-core` is published to Maven Central.
- Dependabot is configured for both Maven and GitHub Actions (daily, 20 open PRs cap).

## Things agents commonly get wrong here
- Don't bump `xxl-job-core` to JDK 17 — it breaks the "JDK 8 client" promise in `README.md`.
- Don't `mvn install` the parent without `-DskipTests` after touching admin code unless you have Docker (PostgreSQL container will start on first DAO test).
- Don't treat `e2e/` as a Maven subproject; it has its own `package.json` and is run via Docker Compose.
- The admin's default Spring profile is unset — for tests it switches to `test` (H2) or `pgtest` (Testcontainers PG); the `pgtest` profile sets `xxl.job.accessToken=` and forces `spring.sql.init.schema-locations=classpath:schema-postgresql.sql` via `AbstractPostgreSQLTest`.
- Sonar exclusions already drop `**/src/test/**` and `xxl-job-executor-samples/**`; coverage reports therefore exclude samples.
