<template>
  <div class="portal-page profile" v-loading="loading">
    <header class="page-head">
      <div>
        <p class="kicker">Account</p>
        <h1>个人中心</h1>
        <p class="lead">管理头像、资料与登录密码</p>
      </div>
    </header>

    <div class="profile-layout">
      <aside class="identity-card">
        <div class="identity-avatar">
          <ImageUpload
            v-model="form.avatar"
            type="avatar"
            :avatar-size="96"
            upload-text="更换头像"
            :fallback="avatarFallback"
            :clearable="false"
            :show-success="false"
            @success="onAvatarUploaded"
          />
        </div>
        <h2>{{ form.nickname || form.username || '未设置昵称' }}</h2>
        <p class="identity-handle">@{{ form.username || '-' }}</p>
        <span class="role-pill">{{ roleLabel }}</span>

        <dl class="identity-meta">
          <div>
            <dt>手机</dt>
            <dd>{{ form.phone || '未填写' }}</dd>
          </div>
          <div>
            <dt>注册时间</dt>
            <dd>{{ createdAtText }}</dd>
          </div>
        </dl>
      </aside>

      <div class="form-stack">
        <section class="form-card">
          <div class="card-head">
            <div>
              <h3>基本资料</h3>
              <p>昵称会显示在顶部导航</p>
            </div>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="profile-form"
          >
            <div class="form-grid">
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
            <div class="card-actions">
              <el-button type="primary" class="action-btn" :loading="submitting" @click="handleSubmit">
                保存修改
              </el-button>
            </div>
          </el-form>
        </section>

        <section class="form-card">
          <div class="card-head">
            <div>
              <h3>修改密码</h3>
              <p>新密码至少 6 位</p>
            </div>
          </div>

          <el-form
            ref="pwdRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-position="top"
            class="profile-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="pwdForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入原密码"
              />
            </el-form-item>
            <div class="form-grid">
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
            <div class="card-actions">
              <el-button type="primary" class="action-btn" :loading="pwdSubmitting" @click="handlePasswordSubmit">
                更新密码
              </el-button>
            </div>
          </el-form>
        </section>
      </div>
    </div>
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
  createdAt: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleLabel = computed(() => {
  if (form.role === 'TEACHER') return '教师'
  if (form.role === 'ADMIN') return '管理员'
  return '普通用户'
})

const avatarFallback = computed(() =>
  (form.nickname || form.username || 'U').slice(0, 1).toUpperCase()
)

const createdAtText = computed(() => {
  if (!form.createdAt) return '—'
  return String(form.createdAt).replace('T', ' ').slice(0, 19)
})

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
  form.createdAt = user?.createdAt || ''
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
  fillForm(user)
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
.profile {
  --accent: #e63935;
  --ink: #1a1a1a;
  --muted: #8a8f98;
  --line: #eceef2;
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 56px;
}

.page-head {
  margin-bottom: 22px;
}

.kicker {
  margin: 0 0 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--accent);
}

.page-head h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 740;
  letter-spacing: -0.03em;
  color: var(--ink);
}

.lead {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.6;
}

.profile-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.identity-card,
.form-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 10px 28px rgba(26, 26, 26, 0.04);
}

.identity-card {
  padding: 28px 22px 24px;
  text-align: center;
  background:
    radial-gradient(220px 140px at 50% 0%, rgba(230, 57, 53, 0.1), transparent 70%),
    #fff;
}

.identity-avatar {
  display: inline-flex;
  margin-bottom: 4px;
}

.identity-card h2 {
  margin: 14px 0 4px;
  font-size: 20px;
  font-weight: 720;
  color: var(--ink);
}

.identity-handle {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--muted);
}

.role-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
  color: var(--accent);
  background: rgba(230, 57, 53, 0.08);
}

.identity-meta {
  margin: 22px 0 0;
  padding-top: 18px;
  border-top: 1px solid #f1f2f4;
  text-align: left;
  display: grid;
  gap: 14px;
}

.identity-meta div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.identity-meta dt {
  color: var(--muted);
}

.identity-meta dd {
  margin: 0;
  font-weight: 600;
  color: var(--ink);
  text-align: right;
}

.form-stack {
  display: grid;
  gap: 16px;
}

.form-card {
  padding: 22px 24px 20px;
}

.card-head {
  margin-bottom: 16px;
}

.card-head h3 {
  position: relative;
  margin: 0 0 4px;
  padding-left: 12px;
  font-size: 16px;
  font-weight: 720;
  color: var(--ink);
}

.card-head h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0.95em;
  border-radius: 999px;
  background: var(--accent);
}

.card-head p {
  margin: 0;
  padding-left: 12px;
  font-size: 13px;
  color: var(--muted);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.action-btn {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
  --el-button-hover-bg-color: #cf2f2c;
  --el-button-hover-border-color: #cf2f2c;
  --el-button-active-bg-color: #b82826;
  --el-button-active-border-color: #b82826;
  min-width: 112px;
  height: 36px;
  border-radius: 999px !important;
  font-weight: 650;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.profile-form :deep(.el-form-item__label) {
  margin-bottom: 6px !important;
  padding: 0 !important;
  line-height: 1.4 !important;
  height: auto !important;
  color: #6b7280;
  font-weight: 600;
  justify-content: flex-start !important;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 12px !important;
  min-height: 40px;
  box-shadow: 0 0 0 1px #e8eaee inset !important;
  background: #fff !important;
}

.profile-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d8dbe2 inset !important;
}

.profile-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--accent) inset,
    0 0 0 3px rgba(230, 57, 53, 0.12) !important;
}

.profile-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f7f8fa !important;
  box-shadow: 0 0 0 1px #eef0f3 inset !important;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .identity-card {
    max-width: 420px;
  }
}

@media (max-width: 640px) {
  .profile {
    padding: 20px 16px 40px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-card {
    padding: 18px 16px 16px;
  }

  .card-actions {
    justify-content: stretch;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
