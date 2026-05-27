const STYLES = [
  'micah',
  'open-peeps',
]

/**
 * 根据 seed 返回一个固定的随机头像 URL（同一 seed 永远返回相同头像）
 * @param {string} seed - 通常传入 userId 或 name，确保每个用户的默认头像唯一且稳定
 * @returns {string} 头像图片 URL
 */
export function getDefaultAvatar(seed) {
  const style = STYLES[hashCode(seed) % STYLES.length]
  return `https://api.dicebear.com/9.x/${style}/svg?seed=${encodeURIComponent(seed || 'default')}`
}

function hashCode(str) {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash) + str.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}
