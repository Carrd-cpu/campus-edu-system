import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '@/api/request'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => user.value?.nickname || user.value?.username || '同学')
  const isAdmin = computed(() => user.value?.accountType === 'ADMIN' || user.value?.role === 'ADMIN')

  async function login(form) {
    const data = await request.post('/auth/login', form)
    applySession(data)
    return data
  }

  async function register(form) {
    const data = await request.post('/auth/register', form)
    applySession(data)
    return data
  }

  function applySession(data) {
    token.value = data.token
    localStorage.setItem('token', data.token)
    user.value = {
      id: data.userId ?? data.id,
      username: data.username,
      nickname: data.nickname,
      role: data.role,
      avatar: data.avatar,
      accountType: data.accountType || (data.role === 'ADMIN' ? 'ADMIN' : 'USER')
    }
  }

  async function fetchUser() {
    const profile = await request.get('/auth/me', { skipAuthRedirect: true, silent: true })
    if (!profile.accountType) {
      profile.accountType = profile.role === 'ADMIN' ? 'ADMIN' : 'USER'
    }
    user.value = profile
    return user.value
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  return {
    user,
    token,
    isLoggedIn,
    displayName,
    isAdmin,
    login,
    register,
    fetchUser,
    logout
  }
})
