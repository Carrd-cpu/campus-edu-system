import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/user/home'
  },
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/UserLogin.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'UserRegister',
    component: () => import('@/views/UserRegister.vue'),
    meta: { public: true }
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    meta: { public: true },
    redirect: '/user/home',
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: () => import('@/views/user/Home.vue'),
        meta: { public: true, title: '用户首页' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { requiresAuth: true, requiresUser: true, title: '个人中心' }
      }
    ]
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, admin: true }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/admin/home',
    meta: { admin: true, requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'home',
        name: 'AdminHome',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', admin: true }
      },
      {
        path: 'admins',
        name: 'AdminAdmins',
        component: () => import('@/views/Admins.vue'),
        meta: { title: '管理员管理', admin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', admin: true }
      },
      {
        path: 'menus',
        name: 'AdminMenus',
        component: () => import('@/views/Menus.vue'),
        meta: { title: '菜单管理', admin: true }
      },
      {
        path: 'banners',
        name: 'AdminBanners',
        component: () => import('@/views/system/Banners.vue'),
        meta: { title: '轮播图管理', admin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/Logs.vue'),
        meta: { title: '系统日志', admin: true }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', admin: true }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', admin: true }
      },
      {
        path: 'colleges',
        name: 'Colleges',
        component: () => import('@/views/edu/Colleges.vue'),
        meta: { title: '学院管理', admin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (to.path.startsWith('/admin') && from.path.startsWith('/admin')) {
      const main = document.querySelector('.layout .main')
      if (main) main.scrollTop = 0
      return false
    }
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

function isAdminAccount(user) {
  return user?.accountType === 'ADMIN' || user?.role === 'ADMIN'
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  const isAdminLogin = to.path === '/admin/login'
  const isUserLogin = to.path === '/login'
  const isUserRegister = to.path === '/register'
  const isAdminRoute = to.path.startsWith('/admin') && !isAdminLogin

  // 已登录访问登录/注册页时跳转
  if (to.meta.public && !to.meta.requiresAuth) {
    if (token && (isAdminLogin || isUserLogin || isUserRegister)) {
      if (!userStore.user) {
        try {
          await userStore.fetchUser()
        } catch {
          userStore.logout()
          return true
        }
      }
      if (isAdminLogin && isAdminAccount(userStore.user)) {
        return '/admin/home'
      }
      if ((isUserLogin || isUserRegister) && !isAdminAccount(userStore.user)) {
        return '/user/home'
      }
    }
    return true
  }

  // 管理后台 / 需要登录的用户页
  if (to.meta.requiresAuth || isAdminRoute) {
    if (!token) {
      const loginPath = to.meta.requiresUser ? '/login' : '/admin/login'
      return { path: loginPath, query: { redirect: to.fullPath } }
    }

    if (!userStore.user) {
      try {
        await userStore.fetchUser()
      } catch {
        userStore.logout()
        const loginPath = to.meta.requiresUser ? '/login' : '/admin/login'
        return { path: loginPath, query: { redirect: to.fullPath } }
      }
    }

    if (to.meta.requiresAdmin || isAdminRoute) {
      if (!isAdminAccount(userStore.user)) {
        userStore.logout()
        return { path: '/admin/login' }
      }
    }

    if (to.meta.requiresUser && isAdminAccount(userStore.user)) {
      return '/admin/home'
    }
  }

  return true
})

export default router
