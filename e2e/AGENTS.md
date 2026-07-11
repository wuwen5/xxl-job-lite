# E2E 测试指南（Playwright）

## 运行方式

- **Docker Compose（完整栈，MySQL）**：`docker compose -f compose.yml -f compose.mysql.yml -f compose.e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests`
- **Docker Compose（完整栈，PostgreSQL）**：`docker compose -f compose.yml -f compose.postgres.yml -f compose.e2e.yml up --build --abort-on-container-exit --exit-code-from e2e-tests`
- **本地运行**（需要 admin 已在 `localhost:8080` 运行）：`cd e2e && npx playwright test`
- 单个文件：`npx playwright test tests/login.spec.ts`
- 单个用例：`npx playwright test -g "should login successfully"`
- 查看报告：`npx playwright show-report`

## 用 playwright-cli 辅助编写测试

项目已配置 `playwright-cli` skill，可用于探索页面 UI 并生成测试代码。典型流程：

1. 启动浏览器：`playwright-cli open http://localhost:8080/`
2. 获取页面快照：`playwright-cli snapshot`（返回带 ref 的 YAML，如 `e17`、`e36`）
3. 用 ref 交互：`playwright-cli fill e17 "admin"`、`playwright-cli click e36`
4. 导航到目标页面后再次 `snapshot`，观察元素结构和角色
5. 根据快照中的 `ref`、`role`、`placeholder` 编写测试选择器

**选择器优先级**：`getByRole` > `getByPlaceholder` > `getByText` > `locator(css)`。避免使用易变的 CSS class，优先用语义角色。**例外**：Element Plus 的弹窗组件（`el-dialog`、`el-message-box`）必须用 `.el-dialog` / `.el-message-box` CSS 选择器，见下方说明。

**重要：不要通过延长超时时间来修复失败的测试。** 如果测试超时，问题几乎一定是选择器定位错误——元素不存在、被遮挡、或匹配了错误的目标。用 `playwright-cli snapshot` 检查实际 DOM 结构来修正选择器。

## Element Plus 组件陷阱

管理端 UI 使用 **Element Plus**（Vue 3 组件库），不是原生 HTML 表单元素。编写测试时不要用 `select`、`option` 等原生标签。

| 组件 | 错误写法 | 正确写法 |
|------|---------|---------|
| 下拉选择器 | `page.locator('select').first()` | `page.locator('.el-select').first()` |
| 选择选项 | `selectOption({ label: 'xxx' })` | `.click()` → `getByRole('option', { name: 'xxx' }).click()` |
| 分页组件 | `.dataTables_info`, `.paginate_button` | `.el-pagination` + `getByRole('button', { name: '下一页' })` |
| 每页条数 | `page.locator('select').filter({ hasText: '10' })` | `.el-pagination .el-select` → 点击后选 `getByRole('option')` |
| 弹窗/对话框 | `page.locator('#someModal')` | `page.locator('.el-dialog')` 或 `page.locator('.el-message-box')` |
| 搜索框定位 | `page.getByPlaceholder('AppName')` | `page.getByRole('textbox', { name: 'AppName' })` |
| 操作成功提示 | `getByText('新增成功')` 等 | `getByText('操作成功')`（统一提示） |

**关键区别**：Element Plus 的下拉选项在点击 combobox 后才渲染到 DOM（teleport 到 body），所以必须先 click 打开，再通过 `getByRole('option')` 选择。

### 高频错误模式（反复出现，务必逐条检查）

以下错误在 `trigger-log.spec.ts` 和 `executor-management.spec.ts` 中反复出现，是新测试最容易踩的坑：

1. **`getByPlaceholder` 在 Element Plus 表单上永远超时**：Element Plus 的 `<input>` 没有 HTML `placeholder` 属性，标签文本来自相邻 `<div>` 而非 placeholder。必须用 `getByRole('textbox', { name: '标签名' })`。
2. **jQuery 式 ID 选择器已不存在**：`#addModal`、`#updateModal`、`#layui-layer1` 等 ID 在 Vue 3 + Element Plus 重构后全部消失。弹窗用 `.el-dialog` / `.el-message-box` CSS 选择器（见下方说明）。
3. **成功提示文案是统一的**：所有操作（新增、编辑、删除等）返回的都是 `操作成功`，不是 `新增成功` / `更新成功` / `删除成功`。
4. **操作列的按钮是 `button` 不是 `link`**：Element Plus 表格操作列渲染为 `<button>`，不是 `<a>` 标签。不要用 `getByRole('link', ...)`。
5. **`getByRole('dialog', ...)` 在 Element Plus 弹窗上超时挂起**：Element Plus 的 `el-dialog` 虽然在 accessibility tree 中有 `role="dialog"`，但 Playwright 的 `getByRole` 匹配时会卡住（Teleport 渲染到 body、`aria-modal` 等原因）。必须用 `page.locator('.el-dialog')`。同理，`ElMessageBox.confirm` 渲染为 `.el-message-box` 而非 `.el-dialog`，也要用 CSS 选择器。
6. **radio-group 点击会被遮挡**：Element Plus 的 `el-radio` 内部 `<span class="el-radio__inner">` 会拦截 pointer events，导致 `getByRole('radio', { name: '...' })` 点击重试。用 `.locator('.el-radio').filter({ hasText: 'xxx' })` 点击容器绕过。

## 测试架构约定

- `fixtures.ts` 导出自定义 `test`，自动完成登录流程（填充用户名密码 → 点击登录 → 等待 menubar 可见）。需要登录态的测试直接 `import { test, expect } from './fixtures'`。
- 登录页测试（`login.spec.ts`）直接 `import { test, expect } from '@playwright/test'`，不使用 fixture。
- `baseURL` 为 `http://localhost:8080`，路径直接写 `/login`、`/dashboard` 等。
- CI 中 Maven 构建用 `-Dmaven.test.skip=true`；e2e 流水线和单元测试流水线完全独立。
