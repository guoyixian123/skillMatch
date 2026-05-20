import request from '@/utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function refreshToken() {
  return request.post('/auth/refresh')
}

export function logout() {
  return request.post('/auth/logout')
}
