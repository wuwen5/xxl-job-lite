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
- 发布构建（Maven Central）用 `-P release -DperformRelease=true`；该 profile 只构建 `xxl-job-core`，用 GPG 签名，跳过测试，通过 `central-publishing-maven-plugin` 发布。没有配置 GPG 时不要在本地执行。

## 运行测试

- 仅单元测试：`./mvnw -pl xxl-job-core,xxl-job-admin test`。
- 单个类：`./mvnw -pl xxl-job-admin -Dtest=ClassName test`。
- PostgreSQL DAO 测试在 `xxl-job-admin/src/test/java/com/xxl/job/admin/dao/pg/*`，继承 `AbstractPostgreSQLTest`，会启动 Testcontainers `postgres:16-alpine`——需要 Docker 运行。
- H2 切片测试使用 `test` profile（`application-test.properties`、`schema.sql`）。
- E2E 测试**不在** Maven 流程中——它们在 `e2e/`（Playwright 1.44），通过模块化 Compose 文件运行。例如 MySQL：`docker compose -f compose.yml -f compose.mysql.yml -f compose.e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests`；PostgreSQL 替换为 `-f compose.postgres.yml`。`.github/workflows/e2e.yml` 中的 Maven 构建步骤用 `-Dmaven.test.skip=true`；e2e 流水线和 CI 流水线是独立的，且支持 MySQL / PostgreSQL 矩阵测试。
- E2E 测试编写与 playwright-cli 使用指南见 `e2e/AGENTS.md`。

## 运行时默认值

- Admin Web 端口：`8080`，**上下文路径 `/`**。Actuator 在 `9001`（探针启用，只暴露 `health,info`；readiness 包含 `db`）。E2E 与 Compose 健康检查都轮询 `/actuator/health/readiness`。
- API 基础路径：`/admin-api/v1/...`（前端 Vite proxy 默认目标 `http://localhost:8080`，见 `xxl-job-admin-ui/vite.config.ts`）。Docker Compose 中执行器 sample 以 `XXL_JOB_ADMIN_ADDRESSES=http://xxl-job-admin:8080` 注册（监听 `8081`）。
- 默认登录：`admin / 123456`（由 SQL 初始化脚本种子数据生成，在 `e2e/tests/example.spec.ts` 中断言）。
- 默认 MySQL JDBC URL 硬编码在 `application.properties`（`jdbc:mysql://127.0.0.1:3306/xxl_job`），通过 `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` 环境变量覆盖（Compose 即用此方式注入）。
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

## 优先使用 Lambda

- 对于简单、一次性的函数式接口实现，优先使用 Lambda。
- 当 Lambda 比普通写法更简洁、更易读时，优先使用 Lambda。
- 如果 Lambda 包含复杂业务逻辑、多个条件分支或多行代码，应提取为普通方法，而不是继续扩展 Lambda。
- 不要为了使用 Lambda 而使用 Lambda。

## 优先使用 Stream

- 对集合的过滤、映射、分组、统计等操作，优先使用 Stream API。
- 简单的数据转换优先使用 `map`、`filter`、`flatMap`、`collect` 等标准操作。
- 当使用 Stream 会降低可读性、影响调试或产生复杂嵌套时，应改用普通 `for` 循环。

## 1. 编码前思考

**不要假设。不要隐藏困惑。呈现权衡。**

在实施之前：

- **明确说明假设** — 如果不确定，询问而不是猜测
- **呈现多种解释** — 当存在歧义时，提出意见，不要默默选择
- **适时提出异议** — 如果存在更简单的方法，说出来
- **困惑时停下来** — 指出不清楚的地方并要求澄清

## 2. 简洁优先

**用最少的代码解决问题。不要过度推测。**

- 不要添加要求之外的功能
- 不要为一次性代码创建抽象
- 不要添加未要求的"灵活性"或"可配置性"
- 不要为不可能发生的场景做错误处理
- 如果 200 行代码可以写成 50 行，重写它

**检验标准：** 资深工程师会觉得这过于复杂吗？如果是，简化。

## 3. 精准修改

**只碰必须碰的。只清理自己造成的混乱。**

编辑现有代码时：

- 不要"改进"相邻的代码、注释或格式
- 不要重构没坏的东西
- 匹配现有风格，即使你更倾向于不同的写法
- 如果注意到无关的死代码，提一下 —— 不要删除它

当你的改动产生孤儿代码时：

- 删除因你的改动而变得无用的导入/变量/函数
- 不要删除预先存在的死代码，除非被要求

**检验标准：** 每一行修改都应该能直接追溯到用户的请求。

## 4. 目标驱动执行

**定义成功标准。循环验证直到达成。**

将指令式任务转化为可验证的目标：

| 不要这样做... | 转化为... |
|--------------|-----------------|
| "添加验证" | "为无效输入编写测试，然后让它们通过" |
| "修复 bug" | "编写重现 bug 的测试，然后让它通过" |
| "重构 X" | "确保重构前后测试都能通过" |

对于多步骤任务，说明一个简短的计划：

```
1. [步骤] → 验证: [检查]
2. [步骤] → 验证: [检查]
3. [步骤] → 验证: [检查]
```