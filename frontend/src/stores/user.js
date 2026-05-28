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
    const res = await updateProfile(data)
    if (profile.value) {
      Object.assign(profile.value, data)
    }
    return res
  }

  async function doUploadAvatar(file) {
    const res = await uploadAvatar(file)
    const newUrl = res.data
    if (profile.value) {
      profile.value.avatarUrl = newUrl
    }
    // 同步更新 authStore 中的头像，导航栏和 localStorage 都需要
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()
    if (newUrl && authStore.user) {
      authStore.setUser({ ...authStore.user, avatarUrl: newUrl })
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
    const res = await addSkill(data)
    await fetchSkills()
    return res
  }

  async function doUpdateSkillList(data) {
    const res = await updateSkillList(data)
    await fetchSkills()
    return res
  }

  async function doDeleteSkill(skillId) {
    const res = await deleteSkill(skillId)
    await fetchSkills()
    return res
  }

  async function fetchHobbies() {
    const res = await getHobbies()
    hobbies.value = res.data.list || res.data || []
  }

  async function doAddHobby(data) {
    const res = await addHobby(data)
    await fetchHobbies()
    return res
  }

  async function doDeleteHobby(hobbyId) {
    const res = await deleteHobby(hobbyId)
    await fetchHobbies()
    return res
  }

  async function fetchGallery() {
    const res = await getGallery()
    gallery.value = res.data.list || res.data || []
  }

  async function doUploadGallery(file) {
    const res = await uploadGallery(file)
    await fetchGallery()
    return res
  }

  async function doDeleteGalleryImage(imageId) {
    const res = await deleteGalleryImage(imageId)
    await fetchGallery()
    return res
  }

  return {
    profile, skills, hobbies, gallery, loading,
    fetchProfile, doUpdateProfile, doUploadAvatar, doChangePassword,
    fetchSkills, doAddSkill, doUpdateSkillList, doDeleteSkill,
    fetchHobbies, doAddHobby, doDeleteHobby,
    fetchGallery, doUploadGallery, doDeleteGalleryImage,
  }
})
