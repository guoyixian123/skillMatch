import request from '@/utils/request'

/** 管理员独立登录 */
export function adminLogin(data) {
  return request.post('/admin/login', data)
}
