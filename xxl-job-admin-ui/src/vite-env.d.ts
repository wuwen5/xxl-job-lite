/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'nprogress'
declare module 'codemirror'
declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const zhCn: any
  export default zhCn
}
