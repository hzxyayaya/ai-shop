import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => {
    let savedUserInfo = null
    try {
      const stored = localStorage.getItem('userInfo')
      if (stored) {
        savedUserInfo = JSON.parse(stored)
      }
    } catch (e) {
      console.error('Failed to parse user info from localStorage', e)
    }

    return {
      token: localStorage.getItem('token') || '',
      userInfo: savedUserInfo
    }
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    clearToken() {
      this.token = ''
      localStorage.removeItem('token')
      this.userInfo = null
      localStorage.removeItem('userInfo')
    },
    setUserInfo(info) {
      this.userInfo = info
      if (info) {
        localStorage.setItem('userInfo', JSON.stringify(info))
      } else {
        localStorage.removeItem('userInfo')
      }
    }
  }
})
