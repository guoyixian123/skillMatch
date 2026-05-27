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

export function togglePostLike(postId) {
  return request.post('/like', { bizId: String(postId), type: 2 })
}

export function unlikePost(postId) {
  return request.delete('/like', { data: { bizId: String(postId), type: 2 } })
}

export function getComments(postId, params) {
  return request.get(`/community/posts/${postId}/comments`, { params })
}

export function createComment(postId, body) {
  return request.post(`/community/posts/${postId}/comments`, body, {
    headers: { 'Content-Type': 'text/plain' },
  })
}

export function deleteComment(postId, commentId) {
  return request.delete(`/community/posts/${postId}/comments/${commentId}`)
}
