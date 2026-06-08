# xxl-job-lite

XXL-JOB-Lite — 基于 XXL-Job 2.5.0 深度重构的分布式任务调度框架，聚焦调度核心能力，剔除冗余扩展，保持轻量、稳定与原生使用习惯。

原项目：[xuxueli/xxl-job](https://github.com/xuxueli/xxl-job)

English version: [README.en.md](./README.en.md)

[![CI](https://github.com/wuwen5/xxl-job-lite/actions/workflows/ci.yml/badge.svg)](https://github.com/wuwen5/xxl-job-lite/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=wuwen5_xxl-job-lite&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=wuwen5_xxl-job-lite)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wuwen5_xxl-job-lite&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wuwen5_xxl-job-lite)
<img src="https://img.shields.io/badge/xxl--job--core-JDK%208%2B-339933?logo=openjdk&logoColor=white" alt="xxl-job-core JDK 8+">
<img src="https://img.shields.io/badge/xxl--job--admin-JDK%2017%2B-007396?logo=openjdk&logoColor=white" alt="xxl-job-admin JDK 17+">
[![Maven Central](https://img.shields.io/maven-central/v/io.github.wuwen5.xxl-job/xxl-job-core.svg)](https://central.sonatype.com/artifact/io.github.wuwen5.xxl-job/xxl-job-core)
[![GitHub release](https://img.shields.io/github/release/wuwen5/xxl-job-lite.svg)](https://github.com/wuwen5/xxl-job-lite/releases)
[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html)

---

## 目录

- [简介](#简介)
- [与原项目的关系](#与原项目的关系)
- [设计目标](#设计目标)
- [功能亮点](#功能亮点)
- [环境要求](#环境要求)
- [架构](#架构)
- [数据库支持](#数据库支持)
- [快速开始](#快速开始)
  - [A. Docker Compose 启动（推荐试用）](#a-docker-compose-启动推荐试用)
  - [B. 源码构建并启动](#b-源码构建并启动)
  - [C. 在业务应用中使用 Executor](#c-在业务应用中使用-executor)
- [配置](#配置)
- [兼容性说明](#兼容性说明)
- [开发与测试](#开发与测试)
- [致谢](#致谢)
- [许可](#许可)
- [贡献](#贡献)

---

## 简介

`xxl-job-lite` 是基于 `xxl-job 2.5.0` 的深度重构版本。项目目标是保留并强化分布式调度这一核心能力，同时简化项目结构、移除非核心组件，从而提升系统的可维护性、可扩展性与生态兼容性。重构过程中，handler 约定、调度模型、数据库表结构、管理端 API 与原版 2.5.0 保持一致。

## 与原项目的关系

- 本项目 fork 自 [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job)，以 2.5.0 为基线进行重构。
- 原作者：[@xuxueli](https://github.com/xuxueli)，社区贡献者众多。
- 在此基础上进行的设计调整：剔除 AI 执行器等 3.0+ 的扩展能力、引入双 JDK 策略（客户端 JDK 8、管理端 JDK 17）、移除 xxl 系列自研组件、改用主流开源生态（Spring、Netty、Gson、MyBatis 等）。

## 设计目标

- **轻量化**：去除非核心功能与自研组件，仅保留调度主线。
- **稳定性优先**：继承 2.5.x 成熟的调度模型（分片、路由、失败重试、阻塞策略等）。
- **生态兼容**：基于主流开源组件构建，便于与 Spring Boot / Cloud 集成。
- **渐进迁移**：尽量兼容 2.5.x 的 API、Handler 约定与数据库表结构。
- **双 JDK 策略**：
  - `xxl-job-core` 保持 JDK 8，业务应用侧无需升级运行环境即可直接接入。
  - `xxl-job-admin` 升级到 JDK 17，使用更现代的标准库特性并与主流基础设施对齐。

## 功能亮点

- ✂️ **精简依赖**：移除 xxl 系列自研组件，基于 Netty / Gson / MyBatis / Spring 等主流开源组件构建。
- 🔧 **易集成**：客户端基于 Spring 生态，天然兼容 Spring Boot / Cloud；提供 Spring 与无框架两种接入示例。
- 🧱 **稳定调度能力**：继承 2.5.x 成熟模型，支持分片广播、多种路由策略（首个、最后一个、轮询、随机、LFU、LRU、一致性哈希、故障转移、忙碌转移）、失败重试、阻塞策略。
- ☕ **双 JDK 支持**：客户端 JDK 8 + 管理端 JDK 17，平衡生态兼容与现代化基础设施。
- 🗄️ **多数据库支持**：MySQL、PostgreSQL、Oracle、达梦均可作为调度中心存储。
- 🔍 **服务发现扩展**：支持自定义 `ServiceAddressResolver`，可集成 Nacos、Consul 等注册中心。
- ⚡ **任务自动注册**：通过 `@XxlJob` 注解即可自动注册定时任务，无需在管理端手动创建。

## 环境要求

| 模块 | 说明 | 最低 JDK |
| --- | --- | --- |
| `xxl-job-core` | 执行器客户端依赖（业务应用侧引入） | JDK 8 |
| `xxl-job-admin` | 调度中心管理端 | JDK 17 |
| `xxl-job-executor-samples` | Spring Boot 与无框架接入示例 | JDK 17 |

附加要求：

- **构建工具**：使用仓库自带的 Maven Wrapper（`./mvnw`），无需本地安装 Maven。
- **Docker**（可选）：使用 Docker Compose 路径时需要。
- **数据库**：本地开发可使用 MySQL 8.x；管理端默认指向 `127.0.0.1:3306`，可通过环境变量覆盖。

## 架构

项目是一个 Maven 多模块工程，三个模块各司其职：

| 模块 | 类型 | 包根 | 关键能力 |
| --- | --- | --- | --- |
| `xxl-job-core` | 客户端库（JDK 8） | `com.xxl.job.core.*` | 嵌入式 Netty 服务、注册/心跳、任务执行线程、回调线程、`@XxlJob` 注解 |
| `xxl-job-admin` | 调度中心（Spring Boot 3，JDK 17） | `com.xxl.job.admin.*` | 调度线程、路由策略、注册中心、MyBatis DAO、Freemarker 管理端、告警、国际化 |
| `xxl-job-executor-samples` | 示例（JDK 17） | `com.xxl.job.executor.*` | Spring Boot 接入示例 + 无框架接入示例 |

模块依赖关系：`xxl-job-admin` 依赖 `xxl-job-core`；`xxl-job-executor-samples` 依赖 `xxl-job-core`。

## 数据库支持

调度中心存储支持以下数据库，初始化脚本位于 `doc/db/`：

| 数据库 | 初始化脚本 | 驱动 |
| --- | --- | --- |
| MySQL | `tables_xxl_job.sql` | `com.mysql:mysql-connector-j` |
| PostgreSQL | `tables_xxl_job_pg.sql` | `org.postgresql:postgresql` |
| Oracle | `tables_xxl_job_oracle.sql` | `com.oracle.database.jdbc:ojdbc8` |
| 达梦 (DM) | `tables_xxl_job_dm.sql` | `com.dameng:DmJdbcDriver18` |
| H2 | `xxl-job-admin/src/test/resources/schema.sql` | 仅用于测试 |

切换数据库只需替换 JDBC 驱动与对应的 DDL 脚本，DAO 层与业务层不依赖特定方言。

## 快速开始

### A. Docker Compose 启动（推荐试用）

仓库自带了一份 e2e Compose 文件，可直接作为试用栈快速拉起 `MySQL + Admin + Sample Executor`。该栈仅用于本地体验，不是生产部署方案。

```bash
# 在仓库根目录
docker compose -f docker-compose-e2e.yml up --build xxl-job-admin mysql executor-sample
```

启动完成后：

- 管理端：[http://localhost:8080/xxl-job-admin](http://localhost:8080/xxl-job-admin)
- Actuator 健康检查：[http://localhost:9001/actuator/health/readiness](http://localhost:9001/actuator/health/readiness)
- 示例 Executor：监听 8081
- MySQL：监听 3306，账号 `root / root_pwd`，库 `xxl_job`，初始化脚本来自 `doc/db/tables_xxl_job.sql`

默认登录账号：`admin / 123456`（由 `tables_xxl_job.sql` 种子数据生成）。

### B. 源码构建并启动

```bash
# 1. 构建（使用仓库自带的 Maven Wrapper，JDK 17+）
./mvnw -B clean package -DskipTests --file pom.xml

# 2. 启动调度中心
java -jar xxl-job-admin/target/xxl-job-admin.jar
```

启动后访问 [http://localhost:8080/xxl-job-admin](http://localhost:8080/xxl-job-admin)，使用 `admin / 123456` 登录。

构建前请确保：

- 本地 `mysql` 已启动，且存在 `xxl_job` 库（执行 `doc/db/tables_xxl_job.sql`）。
- 或通过环境变量 `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` 指向其他数据库。

### C. 在业务应用中使用 Executor

在你的业务应用 `pom.xml` 中引入执行器客户端：

```xml
<dependency>
    <groupId>io.github.wuwen5.xxl-job</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.5.3</version>
</dependency>
```

配置执行器（`application.yml` / `application.properties`）：

```properties
### 调度中心地址
xxl.job.admin.addresses=http://127.0.0.1:8080/xxl-job-admin
### 调度中心 access token，与 admin 端 xxl.job.accessToken 保持一致
xxl.job.admin.accessToken=default_token
### 执行器应用名（用于注册到调度中心）
xxl.job.executor.appname=xxl-job-executor-sample
### 执行器端口（接收调度中心回调）
xxl.job.executor.port=9999
### 执行器日志目录
xxl.job.executor.logpath=applogs/xxl-job/jobhandler
```

使用 `@XxlJob` 注解编写任务：

```java
@XxlJob(value = "demoJobHandler", cron = "0/10 * * * * ?", desc = "演示任务")
public void demoJobHandler() {
    System.out.println("Hello XXL Job Lite");
}
```

完整示例参见 `xxl-job-executor-samples/xxl-job-executor-sample-springboot`（Spring Boot）与 `xxl-job-executor-samples/xxl-job-executor-sample-frameless`（无框架）。

## 配置

调度中心关键默认配置（`xxl-job-admin/src/main/resources/application.properties`）：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | Web 端口 |
| `server.servlet.context-path` | `/xxl-job-admin` | 管理端上下文路径 |
| `management.server.port` | `9001` | Actuator 端口 |
| `management.endpoints.web.exposure.include` | `health,info` | Actuator 暴露端点 |
| `management.endpoint.health.probes.enabled` | `true` | 启用 liveness / readiness 探针 |
| `xxl.job.accessToken` | `default_token` | 调度中心与执行器之间的鉴权 token，**生产环境务必修改** |
| `xxl.job.i18n` | `zh_CN` | 管理端界面语言，可选 `zh_CN` / `zh_TC` / `en` |
| `xxl.job.logretentiondays` | `30` | 任务日志保留天数 |
| `spring.datasource.url` | `jdbc:mysql://127.0.0.1:3306/xxl_job?...` | 数据源 URL，建议通过 `SPRING_DATASOURCE_URL` 环境变量覆盖 |

执行器常见配置项（参考 `xxl-job-executor-samples/xxl-job-executor-sample-springboot/src/main/resources/application.properties`）：

| 配置项 | 说明 |
| --- | --- |
| `xxl.job.admin.addresses` | 调度中心地址，多个用逗号分隔 |
| `xxl.job.admin.accessToken` | 与调度中心保持一致 |
| `xxl.job.executor.appname` | 执行器应用名 |
| `xxl.job.executor.address` | 执行器对外地址，格式 `ip:port`；为空时自动使用 `ip` + `port` 注册 |
| `xxl.job.executor.ip` | 执行器 IP，多网卡时可显式指定 |
| `xxl.job.executor.port` | 执行器端口（`0` 表示自动分配） |
| `xxl.job.executor.logpath` | 任务运行日志目录 |
| `xxl.job.executor.logretentiondays` | 日志保留天数 |

## 兼容性说明

- 兼容目标：以 `xxl-job 2.5.0` 为基准，尽量兼容常用 API、Handler 约定、数据库表结构与管理 UI 的核心交互。
- 内部实现、模块划分、默认配置与依赖在重构中可能发生变化。
- 升级到生产环境前，请在测试环境完成回归验证并备份数据。

## 开发与测试

- 使用 `./mvnw` 调用 Maven，避免本地 Maven 版本不一致。
- `./mvnw -B clean verify --file pom.xml` 会同时执行 Spotless 格式检查、单元测试与 JaCoCo 覆盖率。
- 仅执行单元测试：`./mvnw -pl xxl-job-core,xxl-job-admin test`。
- 单类测试：`./mvnw -pl xxl-job-admin -Dtest=ClassName test`。
- 端到端测试不在 Maven 流程中，需通过 `docker compose -f docker-compose-e2e.yml up --build` 触发 Playwright 套件。

## 致谢

- 感谢原作者 [@xuxueli](https://github.com/xuxueli) 与 [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job) 社区的长期贡献。
- 本项目在遵守原项目开源许可的前提下开展工作，并保留相应的致谢与许可说明。

## 许可

本项目遵循 [GNU General Public License v3](./LICENSE)。请在遵守许可条款的前提下使用、分发或衍生本项目。

## 贡献

欢迎贡献：Bug 修复、文档完善、测试用例或核心功能改进都很受欢迎。

建议流程：

1. Fork 本仓库并基于 `main` 创建 feature 分支。
2. 编写单元测试并保证局部回归通过。
3. 提交 PR，在描述中说明变更理由、兼容性影响与回归测试步骤。
