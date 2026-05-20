import request from '@/utils/request'

export function getPosts(params) {
  return request.get('/community/posts', { params })
}

export function createPost(data) {
  return request.post('/community/posts', data)
}

export function getPostDetail(postId) {
  return request.get(`/community/posts/${postId}`)
}

export function updatePost(postId, data) {
  return request.put(`/community/posts/${postId}`, data)
}

export function deletePost(postId) {
  return request.delete(`/community/posts/${postId}`)
}

export function toggleLike(postId) {
  return request.post(`/community/posts/${postId}/like`)
}

export function getComments(postId, params) {
  return request.get(`/community/posts/${postId}/comments`, { params })
}

export function createComment(postId, data) {
  return request.post(`/community/posts/${postId}/comments`, data)
}

export function deleteComment(postId, commentId) {
  return request.delete(`/community/posts/${postId}/comments/${commentId}`)
}
