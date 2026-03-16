<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '../api/order'
import { getPaymentStatus } from '../api/payment'
import { useSessionStore } from '../store/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const loading = ref(false)
const orderNo = ref('')
const payStatus = ref('')

const isSuccess = computed(() => payStatus.value === 'PAID')
const title = computed(() => {
  if (loading.value) return '正在确认订单状态'
  return isSuccess.value ? '支付成功' : '支付未完成'
})
const desc = computed(() => {
  if (loading.value) return '正在向后端同步订单和支付结果。'
  return isSuccess.value
    ? '订单已经完成支付，可以回首页继续刷新查看。'
    : '还没有确认到支付成功，可以返回首页点击“刷新订单”。'
})

const resolveOrderNo = () => {
  return route.query.orderNo || route.query.out_trade_no || session.lastOrderNo || ''
}

const loadStatus = async () => {
  orderNo.value = resolveOrderNo()
  if (!orderNo.value) {
    payStatus.value = 'FAILED'
    return
  }

  loading.value = true
  try {
    const [paymentRes, orderRes] = await Promise.all([
      getPaymentStatus(orderNo.value),
      getOrderDetail(orderNo.value)
    ])

    payStatus.value = paymentRes.data?.payStatus || orderRes.data?.payStatus || 'FAILED'
  } catch {
    payStatus.value = 'FAILED'
  } finally {
    loading.value = false
  }
}

onMounted(loadStatus)
</script>

<template>
  <div class="result-shell">
    <div class="result-card">
      <p class="eyebrow">Payment Result</p>
      <div class="result-icon" :class="isSuccess ? 'success' : 'failed'">
        {{ loading ? '…' : isSuccess ? '✓' : '!' }}
      </div>
      <h1>{{ title }}</h1>
      <p class="result-desc">{{ desc }}</p>
      <p v-if="orderNo" class="result-order-no">订单号：{{ orderNo }}</p>

      <div class="result-actions">
        <button class="secondary-btn" @click="loadStatus">重新查询</button>
        <button class="primary-btn" @click="router.push('/')">回到首页</button>
      </div>
    </div>
  </div>
</template>
