/**
 * 全局 Toast 工具 — 解决非 Vue 组件环境（如 axios 拦截器）中调用 toast 的问题
 * 在 App.vue 中通过 useToast() 注入实例后即可全局使用
 */

let toastInstance = null

/** 由 App.vue 在 setup 中调用，注入 toast 实例 */
export function setToastInstance(toast) {
  toastInstance = toast
}

/** 全局 toast 调用 */
export const toast = {
  success(detail, summary = '成功') {
    toastInstance?.add({ severity: 'success', summary, detail, life: 3000 })
  },
  info(detail, summary = '提示') {
    toastInstance?.add({ severity: 'info', summary, detail, life: 3000 })
  },
  warn(detail, summary = '警告') {
    toastInstance?.add({ severity: 'warn', summary, detail, life: 4000 })
  },
  error(detail, summary = '错误') {
    toastInstance?.add({ severity: 'error', summary, detail, life: 5000 })
  },
}
