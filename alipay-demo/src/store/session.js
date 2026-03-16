import { defineStore } from 'pinia'

export const useSessionStore = defineStore('alipay-demo-session', {
  state: () => ({
    token: localStorage.getItem('alipay-demo-token') || '',
    user: readJson('alipay-demo-user'),
    lastOrderNo: localStorage.getItem('alipay-demo-last-order-no') || ''
  }),
  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('alipay-demo-token', token)
      localStorage.setItem('alipay-demo-user', JSON.stringify(user))
    },
    clearSession() {
      this.token = ''
      this.user = null
      this.lastOrderNo = ''
      localStorage.removeItem('alipay-demo-token')
      localStorage.removeItem('alipay-demo-user')
      localStorage.removeItem('alipay-demo-last-order-no')
    },
    setLastOrderNo(orderNo) {
      this.lastOrderNo = orderNo || ''
      if (orderNo) {
        localStorage.setItem('alipay-demo-last-order-no', orderNo)
      } else {
        localStorage.removeItem('alipay-demo-last-order-no')
      }
    }
  }
})

function readJson(key) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}
