<template>
  <div class="login-page">
    <!-- 主场景：左侧平台名称 + 右侧登录卡 -->
    <div class="scene">
      <section class="intro">
        <h1>民生建设项目管理平台</h1>
        <p>服务民生 · 共建美好家园</p>
      </section>

      <div class="login-wrap">
        <div class="login-card">
          <h2 class="card-title">账号登录</h2>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                class="line-input"
                placeholder="请输入账号"
                maxlength="32"
                autocomplete="username"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                class="line-input"
                type="password"
                placeholder="请输入密码"
                show-password
                autocomplete="current-password"
              />
            </el-form-item>

            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="form.captchaCode"
                  class="line-input"
                  placeholder="请输入验证码"
                  maxlength="6"
                  autocomplete="off"
                />
                <div
                  class="captcha-img"
                  title="点击刷新验证码"
                  @click="refreshCaptcha"
                >
                  <img v-if="captcha.captchaImage" :src="captcha.captchaImage" alt="验证码" />
                  <span v-else>{{ captchaLoading ? '加载中…' : '加载失败，点击重试' }}</span>
                </div>
              </div>
            </el-form-item>

            <el-button class="login-btn" :loading="loading" @click="handleLogin">
              {{ loading ? '登录中…' : '登 录' }}
            </el-button>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 底部标语 -->
    <footer class="page-foot">让民生服务更便捷，让城乡生活更有温度</footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'
import type { CaptchaVO } from '@/types/system'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

/** 验证码：图片 Base64 + 标识，点击图片刷新 */
const captcha = reactive<CaptchaVO>({
  captchaId: '',
  captchaImage: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/** 拉取验证码并清空已输入的验证码；验证码一次性使用，失败后需重新获取 */
const refreshCaptcha = async () => {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captcha.captchaId = res.data.captchaId
    captcha.captchaImage = res.data.captchaImage
    form.captchaCode = ''
  } catch {
    // 统一拦截器已提示错误信息；清空图片，避免一直停留在“加载中”
    captcha.captchaId = ''
    captcha.captchaImage = ''
  } finally {
    captchaLoading.value = false
  }
}

onMounted(() => {
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
      captchaId: captcha.captchaId,
      captchaCode: form.captchaCode.trim()
    })
    ElMessage.success('登录成功')
    // 有 redirect 参数时回到原目标页，否则进入首页
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/')
  } catch {
    // 统一拦截器已提示错误信息；验证码已一次性失效，重新获取
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
  height: 100vh;
  min-height: 600px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #eaf2fa;
  /* 政务风园林插画背景（本地静态图），叠加浅蓝色纱与侧边栏同色系，加载失败时回退为浅蓝渐变 */
  background-image: linear-gradient(180deg, rgba(234, 242, 250, 0.3) 0%, rgba(23, 81, 140, 0.1) 100%),
    url('/login-bg.png');
  background-size: cover;
  background-position: center;
}

/* ---------- 主场景 ---------- */
.scene {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
  min-height: 0;
  padding: 40px 6.5% 54px;
}

.intro {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 620px;
  margin-left: 1%;
}

.intro h1 {
  margin: 0;
  font-size: clamp(28px, 3.2vw, 46px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 2px;
  white-space: nowrap;
  color: #123a5e;
  text-shadow: 0 2px 20px rgba(255, 255, 255, 0.55);
}

.intro p {
  margin: 20px 0 0;
  font-size: clamp(17px, 1.6vw, 22px);
  font-weight: 600;
  letter-spacing: 3px;
  color: #17518c;
  text-shadow: 0 1px 14px rgba(255, 255, 255, 0.6);
}

.login-wrap {
  flex: 0 0 440px;
  display: flex;
  justify-content: flex-end;
}

/* ---------- 登录卡片 ---------- */
.login-card {
  box-sizing: border-box;
  width: 100%;
  padding: 62px 40px 58px;
  border-radius: 18px;
  /* 磨砂玻璃：半透明让背景插画透出，与页面背景融为一体 */
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.85),
    0 18px 46px rgba(23, 81, 140, 0.14);
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
}

.card-title {
  margin: 0 0 38px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #17324a;
}

/* 下划线风格输入框 */
:deep(.el-form-item) {
  margin-bottom: 14px;
}

:deep(.el-form-item.is-error) {
  margin-bottom: 28px;
}

.line-input :deep(.el-input__wrapper) {
  height: 50px;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  border-bottom: 1px solid #dbe5f0;
  transition: border-color 0.2s;
}

.line-input :deep(.el-input__wrapper:hover) {
  border-bottom-color: #b9cde3;
}

.line-input :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: #22619e;
  box-shadow: none;
}

.line-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #17324a;
}

/* 验证码行：下划线输入框 + 可点击刷新的图片 */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.captcha-row :deep(.el-input) {
  flex: 1 1 auto;
  min-width: 0;
}

.captcha-img {
  flex: 0 0 122px;
  width: 122px;
  height: 50px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: #eef4fb;
  border: 1px solid #dbe5f0;
  cursor: pointer;
  transition: border-color 0.2s;
}

.captcha-img:hover {
  border-color: #22619e;
}

.captcha-img img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-img span {
  font-size: 12px;
  color: #8fa3b3;
}

/* 胶囊渐变按钮 */
.login-btn {
  width: 100%;
  height: 50px;
  margin-top: 34px;
  border: none;
  border-radius: 999px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 6px;
  color: #fff;
  /* 与侧边栏同一套政务蓝渐变 */
  background: linear-gradient(95deg, #22619e 0%, #17518c 100%);
  box-shadow: 0 12px 24px rgba(23, 81, 140, 0.28);
  transition: filter 0.2s, transform 0.2s, box-shadow 0.2s;
}

.login-btn:hover,
.login-btn:focus {
  background: linear-gradient(95deg, #22619e 0%, #17518c 100%);
  filter: brightness(1.06);
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(23, 81, 140, 0.34);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 6px 16px rgba(23, 81, 140, 0.24);
}

/* ---------- 底部标语 ---------- */
.page-foot {
  position: relative;
  z-index: 1;
  padding: 0 24px 26px;
  text-align: center;
  font-size: 14.5px;
  letter-spacing: 2px;
  color: #2f5573;
  text-shadow: 0 1px 14px rgba(255, 255, 255, 0.7);
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .scene {
    flex-direction: column;
    justify-content: center;
    gap: 22px;
    padding: 44px 20px 32px;
  }

  .intro {
    margin-left: 0;
    max-width: 100%;
    text-align: center;
  }

  .intro h1 {
    font-size: clamp(20px, 5.2vw, 26px);
    letter-spacing: 1px;
  }

  .intro p {
    margin-top: 10px;
    font-size: 14px;
    letter-spacing: 2px;
  }

  .login-wrap {
    flex: 0 1 440px;
    width: min(440px, 100%);
    justify-content: center;
  }

  .login-card {
    background: rgba(255, 255, 255, 0.82);
  }

  .page-foot {
    font-size: 13px;
    letter-spacing: 1px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-btn {
    transition: none;
  }
}
</style>
