import request from '@/utils/request'

export function getRecommendedUsers(params) {
  return request.get('/matching/users', { params })
}

export function getUserCard(userId) {
  return request.get(`/matching/users/${userId}/card`)
}

export function getUserProfile(userId) {
  return request.get(`/matching/users/${userId}/profile`)
}

/** 获取城市用户分布（总数，不区分真人/机器人） */
export function getCityDistribution() {
  return request.get('/matching/distribution')
}
