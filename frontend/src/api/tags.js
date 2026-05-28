import request from '@/utils/request'

export function getSkillTags() {
  return request.get('/tags/skills')
}

export function getHobbyTags() {
  return request.get('/tags/hobbies')
}
