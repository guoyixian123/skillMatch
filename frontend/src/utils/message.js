/**
 * 统一消息提示封装
 * 缩短显示时长，显示关闭按钮可手动叉掉
 */
import { ElMessage as RawMessage } from 'element-plus'

// 默认配置
const defaults = {
  showClose: true,
  duration: 1500,
}

// 错误消息稍微久一点
const errorDefaults = {
  showClose: true,
  duration: 2000,
}

function wrap(type, defaultOpts) {
  return (msg, options) => {
    // 支持字符串和对象两种调用方式
    const opts = typeof msg === 'string'
      ? { message: msg, ...defaultOpts, ...options }
      : { ...defaultOpts, ...msg, ...options }
    return RawMessage[type](opts)
  }
}

export const ElMessage = Object.assign(
  // 基础调用 ElMessage({ message, type, ... })
  (options) => {
    if (typeof options === 'string') {
      options = { message: options }
    }
    return RawMessage({ ...defaults, ...options })
  },
  {
    success: wrap('success', defaults),
    warning: wrap('warning', defaults),
    error: wrap('error', errorDefaults),
    info: wrap('info', defaults),
    closeAll: RawMessage.closeAll,
  }
)
