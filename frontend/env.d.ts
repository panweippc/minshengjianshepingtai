/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// ExcelJS 浏览器构建版本（UMD）类型声明
declare module 'exceljs/dist/exceljs.min.js' {
  import ExcelJS from 'exceljs'
  export default ExcelJS
}

// 全局属性类型声明：时间统一只展示年月日
declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $formatDate: (value: unknown, fallback?: string) => string
    $formatDateCell: (row: unknown, column: unknown, cellValue: unknown) => string
  }
}
