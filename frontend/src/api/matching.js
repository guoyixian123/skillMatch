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

export function getMatchExplanation(userId) {
  return request.get(`/matching/users/${userId}/explain`, { noError: true })
}

export function searchUsers(params) {
  return request.get('/matching/search', { params })
}

export function getDiscoverUsers(params) {
  return request.get('/matching/discover', { params })
}

export function getMatchScore(userId) {
  return request.get(`/matching/users/${userId}/score`, { noError: true })
}

export function getAiSuggestion(userId) {
  return request.get(`/matching/discover/${userId}/ai-suggestion`, { noError: true })
}
