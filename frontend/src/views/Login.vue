<template>
  <div class="login-page">
    <!-- 背景光斑 -->
    <div class="bg" aria-hidden="true">
      <span class="blob b1" />
      <span class="blob b2" />
      <span class="blob b3" />
      <span class="dot-grid" />
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
        <div class="card-brand">
          <span class="logo-badge">
            <el-icon :size="22"><Platform /></el-icon>
          </span>
          <div class="brand-text">
            <strong>民生建设项目管理平台</strong>
          </div>
        </div>

      <div class="card-title">
        <h1>欢迎回来</h1>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入登录账号"
            prefix-icon="User"
            maxlength="32"
            clearable
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入登录密码"
            prefix-icon="Lock"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="请输入验证码"
              prefix-icon="Key"
              maxlength="4"
              clearable
              autocomplete="off"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              alt="验证码"
              title="看不清？点击刷新"
              @click="refreshCaptcha"
            />
            <div v-else class="captcha-img captcha-placeholder" title="点击加载" @click="refreshCaptcha">
              加载中
            </div>
          </div>
        </el-form-item>

        <div class="form-extra">
          <el-checkbox v-model="remember">记住账号</el-checkbox>
        </div>

        <el-button
          class="login-btn"
          type="primary"
          :loading="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中…' : '登 录' }}
        </el-button>
      </el-form>
    </div>

    <p class="page-foot">© 2026 民生建设项目管理平台</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'

/** 记住账号的本地存储键 */
const REMEMBER_KEY = 'login_username'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(false)
/** 验证码标识，由后端下发 */
const captchaId = ref('')
/** 验证码图片（Base64） */
const captchaImage = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/** 获取/刷新验证码，验证码每次使用后即失效，需重新获取 */
const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.captchaImage
  } catch {
    // 统一拦截器已提示错误信息
  }
}

onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    form.username = saved
    remember.value = true
  }
  refreshCaptcha()
})

const handleLogin = async () => {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const username = form.username.trim()
    await store.login({
      username,
      password: form.password,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode.trim()
    })
    if (remember.value) {
      localStorage.setItem(REMEMBER_KEY, username)
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    ElMessage.success('登录成功')
    // 有 redirect 参数时回到原目标页，否则进入首页
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
  } catch {
    // 统一拦截器已提示错误信息；验证码已一次性使用，需刷新
    form.captchaCode = ''
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  box-sizing: border-box;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px 56px;
  overflow: hidden;
  background: #f6f8fb;
}

/* ---------- 背景装饰 ---------- */
.bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  animation: blob-drift 18s ease-in-out infinite alternate;
}

.blob.b1 {
  width: 560px;
  height: 560px;
  top: -220px;
  left: -160px;
  background: rgba(147, 197, 253, 0.5);
}

.blob.b2 {
  width: 520px;
  height: 520px;
  right: -180px;
  bottom: -220px;
  background: rgba(196, 181, 253, 0.42);
  animation-delay: -6s;
}

.blob.b3 {
  width: 340px;
  height: 340px;
  top: 18%;
  right: 12%;
  background: rgba(125, 211, 252, 0.32);
  animation-delay: -12s;
}

/* 光斑缓慢漂移，营造呼吸感 */
@keyframes blob-drift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(24px, -18px, 0) scale(1.06);
  }
}

/* 极淡的点阵纹理 */
.dot-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(30, 64, 120, 0.1) 1px, transparent 1px);
  background-size: 26px 26px;
  -webkit-mask-image: radial-gradient(ellipse at 50% 45%, #000 10%, transparent 68%);
  mask-image: radial-gradient(ellipse at 50% 45%, #000 10%, transparent 68%);
}

/* ---------- 卡片 ---------- */
.login-card {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  width: 100%;
  max-width: 432px;
  padding: 38px 42px 30px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow:
    0 30px 80px rgba(15, 40, 90, 0.12),
    0 2px 8px rgba(15, 40, 90, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  animation: card-in 0.55s cubic-bezier(0.22, 0.68, 0.36, 1) both;
}

/* 卡片入场：轻微上浮 + 淡入 */
@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.card-brand {
  display: flex;
  align-items: center;
  gap: 13px;
  padding-bottom: 22px;
  border-bottom: 1px solid #eef2f8;
}

.logo-badge {
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(140deg, #3a7ce6 0%, #18418f 100%);
  box-shadow: 0 8px 18px rgba(24, 65, 143, 0.26);
}

.brand-text strong {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #16243c;
  letter-spacing: 0.6px;
}

.card-title {
  margin: 28px 0 26px;
}

.card-title h1 {
  margin: 0 0 9px;
  font-size: 27px;
  font-weight: 600;
  color: #101d33;
  letter-spacing: 0.5px;
}

/* 验证码行：输入框占满剩余宽度，右侧图片点击可刷新 */
.captcha-row {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 10px;
}

.captcha-row :deep(.el-input) {
  flex: 1;
}

.captcha-img {
  flex: none;
  width: 120px;
  height: 40px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #a8b1bf;
  font-size: 12px;
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 24px;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 15.5px;
  font-weight: 500;
  letter-spacing: 4px;
  background: linear-gradient(95deg, #2f6fd8 0%, #1a4bb0 100%);
  box-shadow: 0 10px 24px rgba(26, 75, 176, 0.28);
  transition: filter 0.2s, transform 0.2s, box-shadow 0.2s;
}

.login-btn:hover,
.login-btn:focus {
  background: linear-gradient(95deg, #2f6fd8 0%, #1a4bb0 100%);
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(26, 75, 176, 0.34);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 6px 16px rgba(26, 75, 176, 0.24);
}

.page-foot {
  position: relative;
  z-index: 1;
  margin: 28px 0 0;
  font-size: 12px;
  color: #aab4c2;
  text-align: center;
}

/* 输入框：白底、细边框、聚焦光圈 */
:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  height: 48px;
  padding: 0 14px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 0 0 1px #e4eaf3 inset;
  transition: box-shadow 0.2s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #ccd9e9 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2f6fd8 inset, 0 0 0 4px rgba(47, 111, 216, 0.12);
}

:deep(.el-input__prefix) {
  color: #a3aec1;
}

/* ---------- 响应式 ---------- */
@media (max-width: 520px) {
  .login-page {
    padding: 24px 16px 48px;
  }

  .login-card {
    padding: 32px 24px 26px;
  }

  .card-title h1 {
    font-size: 23px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .blob {
    animation: none;
  }

  .login-card {
    animation: none;
  }

  .login-btn {
    transition: none;
  }
}
</style>
