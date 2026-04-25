# xxl-job-lite

XXL-JOB-Lite — 基于 XXL-Job 2.5 深度重构，聚焦分布式任务调度核心能力，剔除冗余扩展，保持轻量、稳定与原生使用习惯。

原项目： [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job)

[![Java CI](https://github.com/wuwen5/xxl-job-lite/workflows/Java%20CI/badge.svg)](https://github.com/wuwen5/xxl-job-lite/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=wuwen5_xxl-job-lite&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=wuwen5_xxl-job-lite)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wuwen5_xxl-job-lite&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wuwen5_xxl-job-lite)
<img src="https://img.shields.io/badge/xxl--job--core-JDK%208%2B-339933?logo=openjdk&logoColor=white" alt="xxl-job-core JDK 8+">
<img src="https://img.shields.io/badge/xxl--job--admin-JDK%2017%2B-007396?logo=openjdk&logoColor=white" alt="xxl-job-admin JDK 17+">
[![GitHub release](https://img.shields.io/github/release/wuwen5/xxl-job-lite.svg)](https://github.com/wuwen5/xxl-job-lite/releases)
[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html)


## 项目简介
xxl-job-lite 是基于 xxl-job 2.5.0 的深度重构版本。
目标是保留并强化分布式调度这一核心能力，同时简化项目结构，移除非核心组件，提升系统的可维护性、可扩展性与生态兼容性。

## 为什么要做 xxl-job-lite？
- 原项目在 3.0 之后引入了较多扩展能力（如 AI 执行器、AI 任务等），系统复杂度有所提升
- 本项目更聚焦“调度核心能力”，不引入 AI 任务扩展方向，但会考虑支持MCP、CLI的支持
- 更倾向于与主流开源生态（Spring、主流中间件）深度整合

## 设计目标
- 轻量化：去除非核心功能与内部组件
- 稳定性优先：继承 2.5.x 成熟调度模型
- 生态兼容：基于主流开源组件构建
- 渐进迁移：尽量兼容 2.5.x 使用方式
- 双 JDK 策略：
  - xxl-job-core：JDK 8（业务无感接入）
  - xxl-job-admin：JDK 17（现代化基础设施）

## 功能亮点

- ✂️ 精简依赖：移除 xxl 系列组件，基于主流开源组件构建
- 🔧 易集成：基于 Spring 生态，天然兼容 Spring Boot / Cloud
- 🧱 稳定调度能力：继承 2.5.x 成熟模型（分片、路由、失败重试等）
- ☕ 双 JDK 支持：客户端 JDK 8 + 管理端 JDK 17
- 🗄️ 多数据库支持：支持 PostgreSQL、MySQL、Oracle、达梦等主流数据库作为调度中心存储
- 🔍 服务发现扩展能力：支持自定义服务地址解析器，可用于集成注册中心（如 Nacos、Consul 等）
- ⚡ 任务自动注册：支持通过注解自动初始化任务，无需手动在控制台创建

示例

```java
@XxlJob(
    value = "testInitJobHandler",
    cron = "0/10 * * * * ?",
    desc = "自动注册的任务"
)
public void testInitJobHandler() {
    System.out.println("auto init job");
}
```

## 环境要求

| 模块 | 说明 | 最低 JDK 版本 |
|---|---|-----------|
| `xxl-job-core` | 执行器客户端依赖（业务应用侧引入） | JDK 8     |
| `xxl-job-admin` | 调度中心管理端 | JDK 17    |

- `xxl-job-core` 保持对 JDK 8 的兼容，业务应用侧无需升级运行环境即可直接接入，最大程度降低集成成本。
- `xxl-job-admin` 要求 JDK 17 及以上（与 xxl-job 3.0 保持一致），以便使用更现代的标准库特性，并与主流基础设施（容器镜像、监控组件等）对齐。


## 兼容性说明
- 兼容目标：以 xxl-job 2.5.0 为兼容目标，尽量兼容常用 API、Handler 约定、数据库表结构与管理 UI 的核心交互。
- 免责声明：尽管目标是兼容，但重构过程中对内部实现、模块划分、默认配置与依赖可能发生变化。请在升级到生产环境前在测试环境完成回归验证并备份数据。

## 快速开始

### 1. 启动调度中心

```bash
mvn -DskipTests package
java -jar xxl-job-admin.jar
```

访问地址：

```
http://localhost:8080/xxl-job-admin
```

默认账号：

```
admin / 123456
```
---

### 2. 引入执行器依赖

```xml
<dependency>
  <groupId>io.github.wuwen5.xxl-job</groupId>
  <artifactId>xxl-job-lite-core</artifactId>
</dependency>
```

---

### 3. 编写任务

```java
@XxlJob(
        value = "testInitJobHandler",
        cron = "0/10 * * * * ?",
        desc = "自动注册的任务"
)
public void demoJobHandler() {
    System.out.println("Hello XXL Job Lite");
}
```

---

## 与原项目的关系与致谢
- 本项目基于并致敬 xuxueli/xxl-job（原作者：xuxueli）。xxl-job-lite 在原项目（以 2.5.0 为基线）上进行重构，目标是提供一个更便于与主流生态集成、维护成本更低的版本。
- 原项目地址： https://github.com/xuxueli/xxl-job
- 感谢原作者与社区的贡献。本项目在遵守原项目开源许可的前提下开展工作并保留相应的致谢与许可说明。


## 许可
- 本项目遵循 GNU General Public License v3（GPLv3）。
- 请在遵守许可条款的前提下使用、分发或衍生本项目。


## 贡献
欢迎贡献：无论是 bug 修复、文档完善、测试用例或对核心功能的改进都很受欢迎。请提交 PR 或在 Issues 中详细描述你的建议。

贡献流程建议：
- Fork 本仓库并基于 `main`（或仓库约定的分支）创建 feature 分支
- 编写单元测试并保证局部回归通过
- 提交 PR，并在 PR 描述中说明变更理由、兼容性影响与回归测试步骤

