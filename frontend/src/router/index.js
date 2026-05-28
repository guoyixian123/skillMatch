import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    redirect: '/discover',
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('@/views/DiscoverView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/views/CommunityView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/community/:postId',
    name: 'PostDetail',
    component: () => import('@/views/PostDetailView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/NotificationsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/friends',
    name: 'Friends',
    component: () => import('@/views/FriendsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/chat/:friendId',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'MyProfile',
    component: () => import('@/views/MyProfileView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile/edit',
    name: 'EditProfile',
    component: () => import('@/views/EditProfileView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile/skills',
    name: 'SkillManager',
    component: () => import('@/views/SkillManagerView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile/hobbies',
    name: 'HobbyManager',
    component: () => import('@/views/HobbyManagerView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile/gallery',
    name: 'GalleryManager',
    component: () => import('@/views/GalleryManagerView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/profile/password',
    name: 'ChangePassword',
    component: () => import('@/views/ChangePasswordView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/user/:userId',
    name: 'UserProfile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.guest && authStore.isLoggedIn) {
    next('/discover')
  } else {
    next()
  }
})

export default router
