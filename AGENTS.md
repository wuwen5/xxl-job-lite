# AGENTS.md

## 项目结构

- Maven 多模块：`xxl-job-core`（执行器客户端库）、`xxl-job-admin`（Spring Boot 3 调度中心）、`xxl-job-executor-samples`（Spring Boot + 无框架示例）。
- `xxl-job-admin-ui` 是**纯前端项目**（Vue 3 + TypeScript + Vite），不是 Maven 子模块。它由 `xxl-job-admin` 的 `frontend-maven-plugin` 在 `generate-resources` 阶段自动构建，`dist/` 输出打包进 admin jar 的 `static/` 目录。独立前端开发指南见 `xxl-job-admin-ui/AGENTS.md`。
- GAV：`io.github.wuwen5.xxl-job` / `2.5.3-SNAPSHOT`（在根 `pom.xml` 定义，不要在子模块修改版本）。
- 兼容目标是 xxl-job 2.5.0；重构保留原版 handler 约定、数据库表结构和管理端 API。

## 双 JDK 策略（容易搞错，不要统一）

- `xxl-job-core` 编译目标 **JDK 8**（`maven.compiler.source/target=1.8`）。
- `xxl-job-admin` 和 `xxl-job-executor-samples` 是 **JDK 17**（Spring Boot 3.5 / Jakarta EE）。
- 不要在 `xxl-job-core` 中引入 Java 9+ API（不能用 `List.of`、`var`、`jakarta.*`）。
- CI 矩阵在 Ubuntu 上跑 JDK 17 和 21；本地最低要求 JDK 17 来构建 admin 模块。不需要本地 JDK 8 工具链——编译器插件生成 1.8 字节码。

## 构建与验证

- 使用 Wrapper：`./mvnw -B clean verify --file pom.xml`。不要调用系统 `mvn`——版本在 `pom.xml` 中锁定。
- 默认构建在 `process-sources` 阶段运行 **Spotless 检查**（palantir-java-format）；格式错误会构建失败。用 `./mvnw spotless:apply` 自动修复。
- 构建 `xxl-job-admin` 时，`frontend-maven-plugin` 会在 `generate-resources` 阶段自动在 `xxl-job-admin-ui/` 下执行 `npm install` + `npm run build`，然后将 `dist/` 打包进 jar 的 `static/`。首次构建或前端依赖变更时需要联网下载 Node/npm。
- Surefire 输出到文件；Allure 结果写入各模块的 `../target/allure-results`。
- JaCoCo 覆盖率在每次 `verify` 时收集；SonarCloud 通过 `sonarcloud.yml` workflow 消费。
- 发布构建（Maven Central）用 `-P release -DperformRelease=true`；该 profile 只构建 `xxl-job-core`，用 GPG 签名，跳过测试，通过 `central-publishing-maven-plugin` 发布。没有配置 GPG 时不要在本地执行。

## 运行测试

- 仅单元测试：`./mvnw -pl xxl-job-core,xxl-job-admin test`。
- 单个类：`./mvnw -pl xxl-job-admin -Dtest=ClassName test`。
- PostgreSQL DAO 测试在 `xxl-job-admin/src/test/java/com/xxl/job/admin/dao/pg/*`，继承 `AbstractPostgreSQLTest`，会启动 Testcontainers `postgres:16-alpine`——需要 Docker 运行。
- H2 切片测试使用 `test` profile（`application-test.properties`、`schema.sql`）。
- E2E 测试**不在** Maven 流程中——它们在 `e2e/`（Playwright 1.44），通过 `docker compose -f docker-compose-e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests` 运行。Compose 栈构建 `xxl-job-admin` 和 `xxl-job-executor-sample-springboot` 镜像，启动 MySQL 并用 `doc/db/tables_xxl_job.sql` 初始化，然后对 `http://xxl-job-admin:8080` 执行 `npx playwright test`。`.github/workflows/e2e.yml` 中的 Maven 构建步骤用 `-Dmaven.test.skip=true`；e2e 流水线和 CI 流水线是独立的。

## 运行时默认值

- Admin Web 端口：`8080`，上下文路径 `/xxl-job-admin`。Actuator 在 `9001`（探针启用，只暴露 `health,info`）。E2E 健康检查轮询 `/actuator/health/readiness`。
- 默认登录：`admin / 123456`（由 SQL 初始化脚本种子数据生成，在 `e2e/tests/example.spec.ts` 中断言）。
- 默认 MySQL JDBC URL 硬编码在 `application.properties`，指向 `127.0.0.1:3306`；通过 `SPRING_DATASOURCE_URL` 等环境变量覆盖。
- 多数据库 schema 在 `doc/db/`：`tables_xxl_job.sql`（MySQL）、`tables_xxl_job_pg.sql`、`tables_xxl_job_oracle.sql`、`tables_xxl_job_dm.sql`（达梦）。测试 schema：`xxl-job-admin/src/test/resources/schema.sql`（H2/MySQL 模式）和 `schema-postgresql.sql`。

## 代码风格与约定

- 代码格式为 palantir-java-format 2.38.0（4 空格缩进、120 列、无 tab）——编辑后重新运行 `spotless:apply`；不要手动格式化。
- Lombok 广泛使用。`xxl-job-core` 和 `xxl-job-admin` 使用 `@Slf4j` 注解；`xxl-job-executor-samples` 中使用手动 `Logger` 字段。添加日志时参照所在文件的现有模式。
- `xxl-job-core` 包根：`com.xxl.job.core.*`。`xxl-job-admin` 包根：`com.xxl.job.admin.*`。不要跨包根移动类。
- `.gitattributes` 将 `*.js`、`*.css`、`*.html`、`*.ftl` 标记为 Java（这是有意为之——它们是 admin 内部的 Freemarker/JS 模板）。
- `mvn versions:set` 后本地会出现 `pom.xml.versionsBackup` 文件；它们不会被提交（release workflow 运行 `versions:commit` 来清除）。
- `applogs/` 和 `logs/` 是运行时输出目录，不是源码。

## 发布与部署

- `release.yml` 是 `workflow_dispatch`。必需输入：`release_version`、`next_snapshot`。必需 secrets：`GPG_PRIVATE_KEY`、`GPG_PASSPHRASE`、`OSSRH_USERNAME`、`OSSRH_PASSWORD`、`MAVEN_CENTRAL_TOKEN`、`MAVEN_USERNAME`、`SONAR_TOKEN`、`CODECOV_TOKEN`。
- `xxl-job-admin` 和 `xxl-job-executor-samples` 设置 `skip_maven_deploy=true`；只有 `xxl-job-core` 发布到 Maven Central。
- Dependabot 配置为 Maven 和 GitHub Actions（每日，上限 20 个 PR）。

## 容易犯的错误

- 不要把 `xxl-job-core` 升级到 JDK 17——这会破坏 `README.md` 中"JDK 8 客户端"的承诺。
- 修改 admin 代码后，不要在没有 Docker 的情况下不加 `-DskipTests` 就 `mvn install` 父 POM——PostgreSQL 容器会在第一个 DAO 测试时启动。
- 不要把 `e2e/` 当成 Maven 子项目；它有自己的 `package.json`，通过 Docker Compose 运行。
- admin 的默认 Spring profile 未设置——测试时切换到 `test`（H2）或 `pgtest`（Testcontainers PG）；`pgtest` profile 通过 `AbstractPostgreSQLTest` 设置 `xxl.job.accessToken=` 并强制 `spring.sql.init.schema-locations=classpath:schema-postgresql.sql`。
- Sonar 排除已经过滤了 `**/src/test/**` 和 `xxl-job-executor-samples/**`；覆盖率报告因此不包含 samples。
