/** 全局 Toast — 供 axios 拦截器等非组件环境使用 */
let toastInstance = null

export function setToastInstance(t) { toastInstance = t }

export const toast = {
  success(d, s = '成功') { toastInstance?.add({ severity: 'success', summary: s, detail: d, life: 3000 }) },
  info(d, s = '提示') { toastInstance?.add({ severity: 'info', summary: s, detail: d, life: 3000 }) },
  warn(d, s = '警告') { toastInstance?.add({ severity: 'warn', summary: s, detail: d, life: 4000 }) },
  error(d, s = '错误') { toastInstance?.add({ severity: 'error', summary: s, detail: d, life: 5000 }) },
}
