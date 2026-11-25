<template>
  <AuthSplit
    tone="admin"
    :cover="systemStore.adminLoginCoverSrc"
    title="管理员登录"
    lead="使用后台账号继续"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="auth-form"
      @keyup.enter="handleLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          size="large"
          show-password
        />
      </el-form-item>
      <el-button
        type="primary"
        size="large"
        class="auth-submit"
        :loading="loading"
        @click="handleLogin"
      >
        登录
      </el-button>
    </el-form>

    <template #extra>
      <p class="auth-hint">演示账号 admin / 123456</p>
      <p class="auth-links">
        普通用户请前往
        <router-link to="/login">用户登录</router-link>
        <span class="dot">·</span>
        <router-link to="/user/home">先逛逛首页</router-link>
      </p>
    </template>
  </AuthSplit>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSystemStore } from '@/stores/system'
import AuthSplit from '@/components/AuthSplit.vue'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await userStore.login(form)
    if (data?.accountType !== 'ADMIN' && data?.role !== 'ADMIN') {
      userStore.logout()
      ElMessage.error('仅管理员账号可登录后台')
      return
    }
    ElMessage.success('登录成功')
    router.push('/admin/home')
  } finally {
    loading.value = false
  }
}
</script>
