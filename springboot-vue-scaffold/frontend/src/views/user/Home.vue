<template>
  <div class="portal-page home">
    <section v-if="banners.length" class="banner-wrap">
      <el-carousel height="420px" :interval="5000" arrow="hover" indicator-position="outside">
        <el-carousel-item v-for="item in banners" :key="item.id">
          <div class="banner-slide" :style="{ backgroundImage: `url(${mediaUrl(item.cover)})` }">
            <div class="banner-mask" />
            <div class="banner-copy">
              <h2>{{ item.title }}</h2>
              <p v-if="item.subtitle">{{ item.subtitle }}</p>
              <a
                v-if="item.linkUrl"
                class="banner-cta"
                :href="item.linkUrl"
                @click.prevent="goBanner(item.linkUrl)"
              >
                {{ item.ctaText || '了解更多' }}
              </a>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="welcome">
      <div class="welcome-text">
        <p class="kicker">用户中心</p>
        <h1>你好，{{ displayName }}</h1>
        <p class="lead">
          {{
            isLoggedIn
              ? '欢迎回来。你可以在个人中心更新资料，或从下方入口继续。'
              : '登录后可管理个人资料。也可以先浏览首页，或前往管理后台。'
          }}
        </p>
      </div>
      <div class="welcome-actions">
        <router-link v-if="isLoggedIn" class="btn" to="/user/profile">进入个人中心</router-link>
        <template v-else>
          <router-link class="btn" to="/login">去登录</router-link>
          <router-link class="btn ghost" to="/register">注册账号</router-link>
        </template>
      </div>
    </section>

    <section class="entries">
      <router-link
        v-for="item in entries"
        :key="item.to"
        class="entry"
        :to="item.to"
      >
        <span class="entry-icon" :style="{ background: item.bg, color: item.color }">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
        </span>
        <span class="entry-body">
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </span>
        <el-icon class="entry-arrow" :size="16"><ArrowRight /></el-icon>
      </router-link>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Key, Lock, User, EditPen } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { mediaUrl } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const banners = ref([])
const displayName = computed(() => userStore.user?.nickname || userStore.user?.username || '用户')
const isLoggedIn = computed(() => !!userStore.token)

const entries = computed(() => {
  if (isLoggedIn.value) {
    return [
      {
        to: '/user/profile',
        title: '个人中心',
        desc: '修改昵称、头像和密码',
        icon: User,
        bg: 'rgba(230, 57, 53, 0.1)',
        color: '#e63935'
      },
      {
        to: '/user/profile',
        title: '账号安全',
        desc: '修改登录密码',
        icon: Key,
        bg: 'rgba(37, 99, 235, 0.1)',
        color: '#2563eb'
      },
      {
        to: '/admin/login',
        title: '管理后台',
        desc: '管理员账号从这里进入',
        icon: Lock,
        bg: 'rgba(15, 23, 42, 0.08)',
        color: '#334155'
      }
    ]
  }
  return [
    {
      to: '/login',
      title: '用户登录',
      desc: '使用普通账号进入个人中心',
      icon: User,
      bg: 'rgba(230, 57, 53, 0.1)',
      color: '#e63935'
    },
    {
      to: '/register',
      title: '注册账号',
      desc: '填写信息开通新用户',
      icon: EditPen,
      bg: 'rgba(37, 99, 235, 0.1)',
      color: '#2563eb'
    },
    {
      to: '/admin/login',
      title: '管理后台',
      desc: '管理员账号从这里进入',
      icon: Lock,
      bg: 'rgba(15, 23, 42, 0.08)',
      color: '#334155'
    }
  ]
})

function goBanner(link) {
  const url = String(link || '').trim()
  if (!url) return
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank', 'noopener')
    return
  }
  router.push(url.startsWith('/') ? url : `/${url}`)
}

async function loadBanners() {
  try {
    const data = await request.get('/user/banners', { silent: true })
    banners.value = Array.isArray(data) ? data.filter((item) => item?.cover) : []
  } catch {
    banners.value = []
  }
}

onMounted(loadBanners)
</script>

<style scoped>
.home {
  padding-top: 12px;
  padding-bottom: 40px;
}

.banner-wrap {
  margin-bottom: 28px;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
}

.banner-wrap :deep(.el-carousel__container) {
  border-radius: 20px;
}

.banner-wrap :deep(.el-carousel__indicators--outside) {
  margin-top: 14px;
}

.banner-wrap :deep(.el-carousel__indicator button) {
  width: 18px;
  height: 4px;
  border-radius: 999px;
  background: #d4d4d8;
  opacity: 1;
}

.banner-wrap :deep(.el-carousel__indicator.is-active button) {
  width: 28px;
  background: #e63935;
}

.banner-slide {
  position: relative;
  height: 420px;
  background: #1c1917 center / cover no-repeat;
}

.banner-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    rgba(15, 23, 42, 0.78) 0%,
    rgba(15, 23, 42, 0.42) 48%,
    rgba(15, 23, 42, 0.16) 100%
  );
}

.banner-copy {
  position: absolute;
  left: 48px;
  right: 48px;
  bottom: 52px;
  z-index: 1;
  max-width: 560px;
  color: #fff;
}

.banner-copy h2 {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.2;
  text-shadow: 0 2px 16px rgba(0, 0, 0, 0.28);
}

.banner-copy p {
  margin: 12px 0 0;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.9);
}

.banner-cta {
  display: inline-flex;
  align-items: center;
  margin-top: 20px;
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  background: #fff;
  color: #171717;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.2s ease, background 0.2s ease;
}

.banner-cta:hover {
  background: #f3f4f6;
  transform: translateY(-1px);
}

.welcome {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 8px 4px 8px;
  margin-bottom: 22px;
}

.kicker {
  margin: 0 0 8px;
  color: #e63935;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.welcome h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: -0.03em;
}

.lead {
  margin: 8px 0 0;
  max-width: 36em;
  color: #6b7280;
  line-height: 1.7;
}

.welcome-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

.btn {
  display: inline-flex;
  align-items: center;
  height: 40px;
  padding: 0 18px;
  border-radius: 10px;
  background: #e63935;
  color: #fff;
  font-weight: 600;
  text-decoration: none;
}

.btn.ghost {
  background: transparent;
  color: #e63935;
  border: 1px solid rgba(230, 57, 53, 0.35);
}

.entries {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.entry {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 92px;
  padding: 18px 16px;
  border-radius: 16px;
  border: 1px solid #eceef2;
  background: #fff;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.entry:hover {
  border-color: rgba(230, 57, 53, 0.28);
  box-shadow: 0 10px 24px rgba(26, 35, 48, 0.06);
  transform: translateY(-2px);
}

.entry-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.entry-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.entry-body strong {
  font-size: 15px;
  font-weight: 650;
  color: #1a1a1a;
}

.entry-body small {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.4;
}

.entry-arrow {
  margin-left: auto;
  color: #c4c4c4;
}

.entry:hover .entry-arrow {
  color: #e63935;
}

@media (max-width: 860px) {
  .welcome {
    flex-direction: column;
    align-items: flex-start;
  }

  .entries {
    grid-template-columns: 1fr;
  }

  .banner-wrap :deep(.el-carousel),
  .banner-wrap :deep(.el-carousel__container),
  .banner-slide {
    height: 260px !important;
  }

  .banner-copy {
    left: 22px;
    right: 22px;
    bottom: 28px;
  }

  .banner-copy h2 {
    font-size: 24px;
  }
}
</style>
