import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: () => import('../views/login/index.vue') },
  { path: '/register', component: () => import('../views/register/index.vue') },
  { path: '/home', component: () => import('../views/home/index.vue') },
  { path: '/chat', component: () => import('../views/chat/index.vue') },
  { path: '/cart', component: () => import('../views/cart/index.vue') },
  { path: '/orders', component: () => import('../views/orders/index.vue') },
  { path: '/payment/result', component: () => import('../views/payment/result.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
