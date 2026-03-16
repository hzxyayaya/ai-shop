import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../store/session'

const routes = [
  {
    path: '/',
    component: () => import('../pages/DemoHome.vue')
  },
  {
    path: '/payment/result',
    component: () => import('../pages/PaymentResult.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.path === '/payment/result') {
    return true
  }
  if (!session.token) {
    return true
  }
  return true
})

export default router
