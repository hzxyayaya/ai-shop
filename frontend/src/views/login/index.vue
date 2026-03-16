<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { login } from '../../services/auth'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({
  account: '',
  password: ''
})

const loading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!loginForm.value.account || !loginForm.value.password) {
    errorMessage.value = '请输入账号和密码'
    return
  }
  
  loading.value = true
  errorMessage.value = ''
  
  try {
    const res = await login(loginForm.value)
    if (res.code === 0) {
      // 登录成功
      const { token, user } = res.data
      userStore.setToken(token)
      userStore.setUserInfo(user)
      router.push('/home')
    } else {
      errorMessage.value = res.message || '登录失败'
    }
  } catch (error) {
    errorMessage.value = error.message || '网络或服务器错误'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2 class="auth-title">AI Shop 登录</h2>
      
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="form-group">
          <label for="account">用户名/邮箱</label>
          <input 
            id="account"
            type="text" 
            v-model="loginForm.account" 
            placeholder="请输入您的账号"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            id="password"
            type="password" 
            v-model="loginForm.password" 
            placeholder="请输入密码"
            required
          />
        </div>
        
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="auth-footer">
        还没有账号？ <router-link to="/register" class="link">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.auth-card {
  background: white;
  width: 100%;
  max-width: 400px;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.auth-title {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: #555;
  font-weight: 500;
}

.form-group input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
  outline: none;
}

.form-group input:focus {
  border-color: #1677ff;
}

.submit-btn {
  margin-top: 8px;
  padding: 12px;
  background-color: #1677ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.submit-btn:hover {
  opacity: 0.9;
}

.submit-btn:disabled {
  background-color: #a0c5e8;
  cursor: not-allowed;
}

.error-message {
  padding: 10px;
  background-color: #fff1f0;
  color: #f5222d;
  border: 1px solid #ffa39e;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
  text-align: center;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.link {
  color: #1677ff;
  text-decoration: none;
  font-weight: 500;
}

.link:hover {
  text-decoration: underline;
}
</style>
