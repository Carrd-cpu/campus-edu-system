<template>
  <div class="page profile-page" v-loading="loading">
    <el-card shadow="never" class="panel profile-shell">
      <div class="profile-hero">
        <div class="hero-avatar">
          <ImageUpload
            v-model="form.avatar"
            type="avatar"
            :avatar-size="88"
            upload-text="更换头像"
            :fallback="avatarFallback"
            :clearable="false"
            :show-success="false"
            @success="onAvatarUploaded"
          />
        </div>
        <div class="hero-copy">
          <h2>{{ form.nickname || form.username || '未设置昵称' }}</h2>
          <p>@{{ form.username || '-' }}</p>
          <div class="hero-meta">
            <el-tag :type="roleTagType" effect="light" round>{{ roleLabel }}</el-tag>
            <span class="meta-dot" />
            <span>{{ form.phone || '手机号未填写' }}</span>
          </div>
        </div>
      </div>

      <div class="profile-body">
        <section class="section">
          <div class="section-head">
            <div>
              <h3>基本资料</h3>
              <p>更新昵称与联系方式</p>
            </div>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="profile-form"
          >
            <div class="form-grid cols-4">
              <el-form-item label="用户名">
                <el-input :model-value="form.username" disabled />
              </el-form-item>
              <el-form-item label="角色">
                <el-input :model-value="roleLabel" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input
                  v-model="form.nickname"
                  placeholder="请输入昵称"
                  maxlength="32"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
            </div>
          </el-form>
        </section>

        <section class="section">
          <div class="section-head">
            <div>
              <h3>修改密码</h3>
              <p>建议定期更换登录密码</p>
            </div>
            <el-button type="primary" :loading="pwdSubmitting" @click="handlePasswordSubmit">
              更新密码
            </el-button>
          </div>

          <el-form
            ref="pwdRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-position="top"
            class="profile-form"
          >
            <div class="form-grid cols-3">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="pwdForm.oldPassword"
                  type="password"
                  show-password
                  placeholder="请输入原密码"
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="pwdForm.newPassword"
                  type="password"
                  show-password
                  placeholder="至少 6 位"
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="pwdForm.confirmPassword"
                  type="password"
                  show-password
                  placeholder="再次输入新密码"
                />
              </el-form-item>
            </div>
          </el-form>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import ImageUpload from '@/components/ImageUpload.vue'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const pwdSubmitting = ref(false)
const formRef = ref()
const pwdRef = ref()

const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  avatar: '',
  role: '',
  accountType: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleLabel = computed(() => {
  if (form.accountType === 'ADMIN' || form.role === 'ADMIN') return '管理员'
  if (form.role === 'TEACHER') return '教师'
  return '普通用户'
})

const roleTagType = computed(() => {
  if (form.accountType === 'ADMIN' || form.role === 'ADMIN') return 'primary'
  if (form.role === 'TEACHER') return 'warning'
  return 'info'
})

const avatarFallback = computed(() =>
  (form.nickname || form.username || 'U').slice(0, 1).toUpperCase()
)

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$|^$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function fillForm(user) {
  form.username = user?.username || ''
  form.nickname = user?.nickname || ''
  form.phone = user?.phone || ''
  form.avatar = user?.avatar || ''
  form.role = user?.role || ''
  form.accountType = user?.accountType || ''
}

async function loadProfile() {
  loading.value = true
  try {
    const user = await userStore.fetchUser()
    fillForm(user)
  } finally {
    loading.value = false
  }
}

async function persistProfile(payload, successMsg) {
  const user = await request.put('/auth/profile', payload)
  if (!user.accountType) {
    user.accountType = user.role === 'ADMIN' ? 'ADMIN' : 'USER'
  }
  userStore.user = user
  fillForm(userStore.user)
  ElMessage.success(successMsg)
}

async function onAvatarUploaded(url) {
  form.avatar = url
  await persistProfile(
    { nickname: form.nickname, phone: form.phone, avatar: form.avatar },
    '头像已更新'
  )
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await persistProfile(
      { nickname: form.nickname, phone: form.phone, avatar: form.avatar },
      '个人信息已更新'
    )
  } finally {
    submitting.value = false
  }
}

async function handlePasswordSubmit() {
  await pwdRef.value.validate()
  pwdSubmitting.value = true
  try {
    await request.post('/auth/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已更新')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    pwdSubmitting.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  width: 100%;
  max-width: none;
}

.profile-shell :deep(.el-card__body) {
  padding: 0;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px 28px 24px;
  background:
    radial-gradient(420px 180px at 8% 0%, color-mix(in srgb, var(--app-primary) 12%, transparent), transparent 70%),
    linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
}

.hero-avatar {
  flex-shrink: 0;
}

.hero-copy {
  min-width: 0;
}

.hero-copy h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 720;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.hero-copy > p {
  margin: 6px 0 12px;
  font-size: 13px;
  color: #94a3b8;
}

.hero-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: #64748b;
}

.meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #cbd5e1;
}

.profile-body {
  padding: 8px 28px 28px;
}

.section {
  padding: 22px 0;
}

.section + .section {
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-head h3 {
  position: relative;
  margin: 0 0 4px;
  padding-left: 12px;
  font-size: 16px;
  font-weight: 720;
  color: #0f172a;
}

.section-head h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0.95em;
  border-radius: 999px;
  background: var(--app-primary);
}

.section-head p {
  margin: 0;
  padding-left: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.section-head .el-button {
  flex-shrink: 0;
  min-width: 112px;
  height: 36px;
  border-radius: 10px;
  font-weight: 650;
}

.profile-form {
  width: 100%;
  max-width: none;
}

.form-grid {
  display: grid;
  gap: 4px 20px;
  width: 100%;
}

.form-grid.cols-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.form-grid.cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.profile-form :deep(.el-form-item__label) {
  margin-bottom: 6px !important;
  padding: 0 !important;
  line-height: 1.4 !important;
  height: auto !important;
  color: #64748b;
  font-weight: 600;
  justify-content: flex-start !important;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  min-height: 38px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08) inset !important;
  background: #fff !important;
}

.profile-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.14) inset !important;
}

.profile-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--app-primary) inset,
    0 0 0 3px color-mix(in srgb, var(--app-primary) 14%, transparent) !important;
}

.profile-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f8fafc !important;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.05) inset !important;
}

@media (max-width: 1100px) {
  .form-grid.cols-4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .profile-hero {
    padding: 24px 20px 20px;
  }

  .profile-body {
    padding: 4px 20px 22px;
  }

  .form-grid.cols-4,
  .form-grid.cols-3 {
    grid-template-columns: 1fr;
  }

  .section-head {
    align-items: flex-start;
  }
}
</style>
