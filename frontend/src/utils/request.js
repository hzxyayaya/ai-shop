import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// Request Interceptor
request.interceptors.request.use(
  config => {
    // Attempt to get token from localStorage
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response Interceptor
request.interceptors.response.use(
  response => {
    // You can handle unified response code here based on the API spec
    return response.data
  },
  error => {
    if (error.response?.status === 401 && !error.config?.skipAuthRedirect) {
      localStorage.removeItem('token')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(error.response.data || error)
    }
    return Promise.reject(error)
  }
)

export default request
