import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getProfile, updateProfile, uploadAvatar, changePassword,
  getSkills, addSkill, updateSkillList, deleteSkill,
  getHobbies, addHobby, deleteHobby,
  getGallery, uploadGallery, deleteGalleryImage,
} from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const profile = ref(null)
  const skills = ref({ canSkills: [], wantSkills: [] })
  const hobbies = ref([])
  const gallery = ref([])
  const loading = ref(false)

  async function fetchProfile(userId) {
    loading.value = true
    try {
      const res = await getProfile(userId)
      profile.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function doUpdateProfile(data) {
    await updateProfile(data)
    if (profile.value) {
      Object.assign(profile.value, data)
    }
  }

  async function doUploadAvatar(file) {
    const res = await uploadAvatar(file)
    if (profile.value) {
      profile.value.avatarUrl = res.data
    }
    return res
  }

  async function doChangePassword(data) {
    return await changePassword(data)
  }

  async function fetchSkills() {
    const res = await getSkills()
    skills.value = res.data
  }

  async function doAddSkill(data) {
    await addSkill(data)
    await fetchSkills()
  }

  async function doUpdateSkillList(data) {
    await updateSkillList(data)
    await fetchSkills()
  }

  async function doDeleteSkill(skillId) {
    await deleteSkill(skillId)
    await fetchSkills()
  }

  async function fetchHobbies() {
    const res = await getHobbies()
    hobbies.value = res.data.list || res.data || []
  }

  async function doAddHobby(data) {
    await addHobby(data)
    await fetchHobbies()
  }

  async function doDeleteHobby(hobbyId) {
    await deleteHobby(hobbyId)
    await fetchHobbies()
  }

  async function fetchGallery() {
    const res = await getGallery()
    gallery.value = res.data.list || res.data || []
  }

  async function doUploadGallery(file) {
    await uploadGallery(file)
    await fetchGallery()
  }

  async function doDeleteGalleryImage(imageId) {
    await deleteGalleryImage(imageId)
    await fetchGallery()
  }

  return {
    profile, skills, hobbies, gallery, loading,
    fetchProfile, doUpdateProfile, doUploadAvatar, doChangePassword,
    fetchSkills, doAddSkill, doUpdateSkillList, doDeleteSkill,
    fetchHobbies, doAddHobby, doDeleteHobby,
    fetchGallery, doUploadGallery, doDeleteGalleryImage,
  }
})
