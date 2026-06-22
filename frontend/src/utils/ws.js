/**
 * WebSocket 连接管理器
 * - 自动认证（token 从 localStorage 读取）
 * - 自动重连（指数退避，最大 30 秒）
 * - 心跳保活（每 25 秒 ping）
 * - 事件分发（chat / notification / pong）
 */

const WS_PATH = '/ws/chat'
const PING_INTERVAL = 25000        // 心跳间隔 25s
const RECONNECT_BASE = 1000        // 重连基础间隔 1s
const RECONNECT_MAX = 30000        // 最大重连间隔 30s
const RECONNECT_DECAY = 1.5        // 退避系数

class WsManager {
  constructor() {
    this.ws = null
    this.listeners = new Map()     // type -> Set<callback>
    this.reconnectTimer = null
    this.pingTimer = null
    this.reconnectDelay = RECONNECT_BASE
    this.manualClose = false       // 区分主动断开和异常断开
    this._connected = false
  }

  /** 注册事件监听 */
  on(type, callback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type).add(callback)
    return () => this.listeners.get(type)?.delete(callback)  // 返回取消函数
  }

  /** 触发事件 */
  _emit(type, data) {
    this.listeners.get(type)?.forEach(fn => {
      try { fn(data) } catch (e) { console.error(`[WS] 事件处理错误 (${type}):`, e) }
    })
    // 也触发通配符 *
    this.listeners.get('*')?.forEach(fn => {
      try { fn(type, data) } catch (e) { console.error('[WS] 通配符处理错误:', e) }
    })
  }

  /** 建立连接 */
  connect() {
    if (this.ws && (this.ws.readyState === WebSocket.CONNECTING || this.ws.readyState === WebSocket.OPEN)) {
      return // 已连接或正在连接
    }

    const token = localStorage.getItem('token')
    if (!token) {
      console.warn('[WS] 无 token，跳过连接')
      return
    }

    // 构建 ws:// 或 wss:// URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = `${protocol}//${host}${WS_PATH}?token=${encodeURIComponent(token)}`

    this.manualClose = false

    try {
      this.ws = new WebSocket(url)
    } catch (e) {
      console.error('[WS] 创建连接失败:', e)
      this._scheduleReconnect()
      return
    }

    this.ws.onopen = () => {
      console.log('%c[WS] ✅ 连接成功', 'color: #059669; font-weight: bold;', url)
      this._connected = true
      this.reconnectDelay = RECONNECT_BASE
      this._startPing()
      this._emit('open')
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        const type = msg.type || 'unknown'
        console.log('%c[WS] 📩 收到消息:', 'color: #8B5CF6; font-weight: bold;', type, msg)
        this._emit(type, msg)
      } catch (e) {
        console.warn('[WS] 消息解析失败:', event.data)
      }
    }

    this.ws.onclose = (event) => {
      console.log(`%c[WS] ❌ 连接关闭: code=${event.code}`, 'color: #DC2626; font-weight: bold;', event.reason)
      this._connected = false
      this._stopPing()
      this._emit('close', event)

      if (!this.manualClose) {
        this._scheduleReconnect()
      }
    }

    this.ws.onerror = (event) => {
      console.error('[WS] ⚠️ 连接错误', event)
      this._emit('error', event)
    }
  }

  /** 主动断开 */
  disconnect() {
    this.manualClose = true
    this._stopPing()
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
    if (this.ws) {
      this.ws.close(1000, '主动断开')
      this.ws = null
    }
    this._connected = false
    console.log('[WS] 已主动断开')
  }

  /** 发送消息 */
  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const payload = typeof data === 'string' ? data : JSON.stringify(data)
      this.ws.send(payload)
      return true
    }
    console.warn('[WS] 发送失败：连接未就绪')
    return false
  }

  /** 是否已连接 */
  get connected() {
    return this._connected
  }

  /** 启动心跳 */
  _startPing() {
    this._stopPing()
    this.pingTimer = setInterval(() => {
      this.send({ type: 'ping' })
    }, PING_INTERVAL)
  }

  /** 停止心跳 */
  _stopPing() {
    if (this.pingTimer) {
      clearInterval(this.pingTimer)
      this.pingTimer = null
    }
  }

  /** 指数退避重连 */
  _scheduleReconnect() {
    clearTimeout(this.reconnectTimer)
    const delay = Math.min(this.reconnectDelay, RECONNECT_MAX)
    console.log(`[WS] ${delay / 1000}秒后重连...`)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectDelay *= RECONNECT_DECAY
      this.connect()
    }, delay)
  }
}

// 全局单例
const ws = new WsManager()
export default ws
