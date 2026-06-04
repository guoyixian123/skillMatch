import request from '@/utils/request'

export function listUsers(params)     { return request.get('/admin/users', { params }) }
export function getUserDetail(id)    { return request.get(`/admin/users/${id}`) }
export function createUser(data)     { return request.post('/admin/users', data) }
export function updateUser(id, data) { return request.put(`/admin/users/${id}`, data) }
export function updateUserStatus(data) { return request.put('/admin/users/status', data) }
export function batchCreateRobots(data) { return request.post('/admin/robots/batch', data) }
export function deleteRobot(id)      { return request.delete(`/admin/robots/${id}`) }
export function batchDeleteRobots(ids) { return request.delete('/admin/robots/batch', { data: { userIds: ids } }) }
export function getDashboard()       { return request.get('/admin/dashboard') }
export function getDailyStats(days)  { return request.get('/admin/daily-stats', { params: { days } }) }

// ===== 管理员管理（仅 ROOT） =====
export function listAdmins()           { return request.get('/admin/admins') }
export function addAdmin(data)         { return request.post('/admin/admins', data) }
export function toggleAdminStatus(id)  { return request.put(`/admin/admins/${id}/status`) }
export function removeAdmin(id)        { return request.delete(`/admin/admins/${id}`) }

// 标签数据（供新增用户/机器人下拉选择）
export function getSkillTags() { return request.get('/tags/skills') }
export function getHobbyTags() { return request.get('/tags/hobbies') }

// ===== 标签管理 =====
export function listSkillTags()          { return request.get('/admin/tags/skills') }
export function createSkillTag(data)     { return request.post('/admin/tags/skills', data) }
export function updateSkillTag(id, data) { return request.put(`/admin/tags/skills/${id}`, data) }
export function deleteSkillTag(id)       { return request.delete(`/admin/tags/skills/${id}`) }
export function listHobbyTags()          { return request.get('/admin/tags/hobbies') }
export function createHobbyTag(data)     { return request.post('/admin/tags/hobbies', data) }
export function updateHobbyTag(id, data) { return request.put(`/admin/tags/hobbies/${id}`, data) }
export function deleteHobbyTag(id)       { return request.delete(`/admin/tags/hobbies/${id}`) }

// 头像上传
export function uploadAvatar(file) {
  const fd = new FormData(); fd.append('avatarUrl', file)
  return request.put('/user/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
