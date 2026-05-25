import request from '@/utils/request'

export function getProfile(userId) {
  return request.get(`/user/profile/${userId}`)
}

export function likeProfile(userId) {
  return request.post('/like', { bizId: String(userId), type: 1 })
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('avatarUrl', file)
  return request.put('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function changePassword(data) {
  return request.put('/user/password', data)
}

export function deleteAccount() {
  return request.delete('/user/account')
}

export function getSkills() {
  return request.get('/user/skills')
}

export function addSkill(data) {
  return request.post('/user/skills', data)
}

export function updateSkillList(data) {
  return request.put('/user/skills', data)
}

export function deleteSkill(skillId) {
  return request.delete(`/user/skills/${skillId}`)
}

export function getHobbies() {
  return request.get('/user/hobbies')
}

export function addHobby(data) {
  return request.post('/user/hobbies', data)
}

export function deleteHobby(hobbyId) {
  return request.delete(`/user/hobbies/${hobbyId}`)
}

export function getGallery() {
  return request.get('/user/gallery')
}

export function uploadGallery(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/gallery', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteGalleryImage(imageId) {
  return request.delete(`/user/gallery/${imageId}`)
}

export function updateLocation(data) {
  return request.put('/user/location', data)
}
