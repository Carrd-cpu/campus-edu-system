import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import request from '@/api/request'
import { mediaUrl } from '@/utils/format'

const DEFAULT_NAME = '基础脚手架'
const DEFAULT_LOGO = '/logo.svg'
const DEFAULT_LOGIN_COVER =
  'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1400&q=80'
const DEFAULT_REGISTER_COVER =
  'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1400&q=80'
const DEFAULT_ADMIN_LOGIN_COVER =
  'https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1400&q=80'

export const useSystemStore = defineStore('system', () => {
  const systemName = ref(localStorage.getItem('system-name') || DEFAULT_NAME)
  const systemLogo = ref(localStorage.getItem('system-logo') || '')
  const loginCover = ref(localStorage.getItem('login-cover') || '')
  const registerCover = ref(localStorage.getItem('register-cover') || '')
  const adminLoginCover = ref(localStorage.getItem('admin-login-cover') || '')
  const loaded = ref(false)

  const displayName = computed(() => {
    const name = String(systemName.value || '').trim()
    return name || DEFAULT_NAME
  })

  const logoSrc = computed(() => {
    const custom = String(systemLogo.value || '').trim()
    if (!custom) return DEFAULT_LOGO
    return mediaUrl(custom) || DEFAULT_LOGO
  })

  const hasCustomLogo = computed(() => !!String(systemLogo.value || '').trim())

  const loginCoverSrc = computed(() => resolveCover(loginCover.value, DEFAULT_LOGIN_COVER))
  const registerCoverSrc = computed(() => resolveCover(registerCover.value, DEFAULT_REGISTER_COVER))
  const adminLoginCoverSrc = computed(() => resolveCover(adminLoginCover.value, DEFAULT_ADMIN_LOGIN_COVER))

  function resolveCover(value, fallback) {
    const custom = String(value || '').trim()
    if (!custom) return fallback
    return mediaUrl(custom) || fallback
  }

  function applyDocumentTitle() {
    if (typeof document !== 'undefined') {
      document.title = displayName.value
    }
  }

  function applyFavicon() {
    if (typeof document === 'undefined') return
    let link = document.querySelector("link[rel='icon']")
    if (!link) {
      link = document.createElement('link')
      link.setAttribute('rel', 'icon')
      document.head.appendChild(link)
    }
    const src = logoSrc.value
    link.setAttribute('type', src.endsWith('.svg') ? 'image/svg+xml' : 'image/png')
    link.setAttribute('href', src.includes('?') ? src : `${src}?t=${Date.now()}`)
  }

  function applyName(name) {
    const next = String(name || '').trim() || DEFAULT_NAME
    systemName.value = next
    localStorage.setItem('system-name', next)
    applyDocumentTitle()
  }

  function persistOptional(key, value, refValue) {
    const next = String(value || '').trim()
    refValue.value = next
    if (next) localStorage.setItem(key, next)
    else localStorage.removeItem(key)
  }

  function applyLogo(logo) {
    persistOptional('system-logo', logo, systemLogo)
    applyFavicon()
  }

  function applyCovers({ loginCover: login, registerCover: register, adminLoginCover: admin } = {}) {
    persistOptional('login-cover', login, loginCover)
    persistOptional('register-cover', register, registerCover)
    persistOptional('admin-login-cover', admin, adminLoginCover)
  }

  async function fetchConfig() {
    try {
      const data = await request.get('/user/system-config', { silent: true })
      applyName(data?.systemName)
      applyLogo(data?.systemLogo)
      applyCovers(data)
    } catch {
      applyDocumentTitle()
      applyFavicon()
    } finally {
      loaded.value = true
    }
  }

  async function saveConfig({
    systemName: name,
    systemLogo: logo,
    loginCover: login,
    registerCover: register,
    adminLoginCover: admin
  }) {
    const data = await request.put('/admin/system-config', {
      systemName: name,
      systemLogo: logo == null ? systemLogo.value : logo,
      loginCover: login == null ? loginCover.value : login,
      registerCover: register == null ? registerCover.value : register,
      adminLoginCover: admin == null ? adminLoginCover.value : admin
    })
    applyName(data?.systemName || name)
    applyLogo(data?.systemLogo ?? logo ?? '')
    applyCovers({
      loginCover: data?.loginCover ?? login ?? '',
      registerCover: data?.registerCover ?? register ?? '',
      adminLoginCover: data?.adminLoginCover ?? admin ?? ''
    })
    return data
  }

  async function saveSystemName(name) {
    return saveConfig({ systemName: name, systemLogo: systemLogo.value })
  }

  watch(displayName, () => applyDocumentTitle(), { immediate: true })
  watch(logoSrc, () => applyFavicon(), { immediate: true })

  return {
    systemName,
    systemLogo,
    loginCover,
    registerCover,
    adminLoginCover,
    displayName,
    logoSrc,
    hasCustomLogo,
    loginCoverSrc,
    registerCoverSrc,
    adminLoginCoverSrc,
    loaded,
    fetchConfig,
    saveConfig,
    saveSystemName,
    applyName,
    applyLogo
  }
})
