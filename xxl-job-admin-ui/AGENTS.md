# AGENTS.md - xxl-job-admin-ui

## 项目概述

XXL-JOB 管理后台前端，Vue 3 + TypeScript + Vite 技术栈。

## 开发命令

```bash
pnpm dev      # 启动开发服务器 (端口 5173)
pnpm build    # 类型检查 + 构建生产版本
pnpm lint     # ESLint 检查并自动修复
pnpm format   # Prettier 格式化 src/
pnpm preview  # 预览生产构建
```

## 关键架构约定

### 自动导入 (重要)

项目使用 `unplugin-auto-import` 和 `unplugin-vue-components`：

- **Vue/Pinia/Router/VueUse API** 自动导入，无需手动 `import { ref, computed, ... } from 'vue'`
- **Element Plus 组件** 自动按需导入，无需手动注册
- 类型声明文件：`src/auto-imports.d.ts`、`src/components.d.ts`
- 如果 IDE 报未找到导入，运行 `npm run dev` 触发类型声明生成

### API 层

- Axios 实例：`src/utils/request.ts`
- 基础路径：`/admin-api/v1`（通过 Vite proxy 转发到 `localhost:8080`）
- 响应格式：`{ code: 200, content: T, msg: string }`，拦截器自动解包 `content`
- 分页格式：`{ recordsTotal, recordsFiltered, data: T[] }`
- 认证：Bearer token 存储在 localStorage，401 自动跳转登录

### 路由与权限

- 路由守卫在 `src/router/index.ts`，未登录重定向到 `/login`
- 需要认证的路由省略 `meta.requiresAuth`，无需认证的路由设置 `meta.requiresAuth: false`
- NProgress 显示路由切换进度

### 国际化

- vue-i18n，语言文件在 `src/i18n/`（zh-CN、zh-TW、en）
- 默认语言：zh-CN，存储在 localStorage `locale` key
- 组件内使用 `const { t } = useI18n()`

### 样式

- SCSS 预处理器，全局变量文件：`src/assets/styles/variables.scss`（自动注入每个组件）
- 全局样式：`src/assets/styles/global.scss`
- 使用 scoped SCSS

### 状态管理

- Pinia stores 在 `src/stores/`
- `auth.ts`：token + userInfo，token 存 localStorage

## 目录结构

```
src/
├── api/          # API 接口定义（每个模块一个文件）
├── components/   # 公共组件（Layout、CronInput）
├── composables/  # 组合式函数（当前为空）
├── i18n/         # 国际化语言包
├── router/       # 路由配置
├── stores/       # Pinia 状态管理
├── utils/        # 工具函数（request.ts）
└── views/        # 页面视图（按功能模块划分）
```

## 注意事项

- 构建输出到 `dist/`，已加入 `.gitignore`
- 开发服务器代理 `/admin-api/v1` 到后端 `http://localhost:8080`
- Element Plus 图标全局注册在 `main.ts`
- TypeScript 严格模式开启，`noUnusedLocals` 和 `noUnusedParameters` 为 true
