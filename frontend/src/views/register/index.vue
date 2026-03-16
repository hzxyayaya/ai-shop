<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../services/auth'

const router = useRouter()

const registerForm = ref({
  username: '',
  email: '',
  password: ''
})

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    errorMessage.value = '请填写完整的注册信息'
    return
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.value.email.trim())) {
    errorMessage.value = '请输入合法的邮箱地址'
    return
  }

  if (registerForm.value.password.length < 8) {
    errorMessage.value = '密码长度不能少于 8 位'
    return
  }
  
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  
  try {
    const res = await register(registerForm.value)
    if (res.code === 0) {
      successMessage.value = '注册成功，即将跳转登录页...'
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      errorMessage.value = res.message || '注册失败'
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
      <h2 class="auth-title">AI Shop 注册</h2>
      
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
      
      <div v-if="successMessage" class="success-message">
        {{ successMessage }}
      </div>

      <form @submit.prevent="handleRegister" class="auth-form" novalidate>
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            id="username"
            type="text" 
            v-model="registerForm.username" 
            placeholder="请输入用户名"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="email">邮箱</label>
          <input 
            id="email"
            type="text" 
            v-model="registerForm.email" 
            placeholder="请输入邮箱"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            id="password"
            type="password" 
            v-model="registerForm.password" 
            placeholder="请输入不少于8位的密码"
            required
            minlength="8"
          />
        </div>
        
        <button type="submit" class="submit-btn" :disabled="loading || !!successMessage">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="auth-footer">
        已有账号？ <router-link to="/login" class="link">去登录</router-link>
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

.success-message {
  padding: 10px;
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
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
