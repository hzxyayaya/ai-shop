<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPaymentStatus } from '../../services/payment'
import { getOrderDetail } from '../../services/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orderNo = ref('')
const payStatus = ref('')
const statusMessage = ref('')
const orderDetail = ref(null)

const RESULT_MESSAGES = {
  PENDING: '支付结果正在确认中，请稍后刷新或前往订单页查看最新状态。',
  UNAUTHORIZED: '当前页面未读取到登录态，通常是支付宝回跳到了另一个本地域名。请回到原支付页面使用同一域名重新打开订单页确认。',
  QUERY_FAILED: '支付已提交，但当前页面未能完成状态查询。请稍后刷新或前往订单页查看。'
}

const isSuccess = computed(() => payStatus.value === 'PAID')
const title = computed(() => {
  if (loading.value) return '正在确认支付结果'
  if (payStatus.value === 'PENDING') return '支付结果确认中'
  return isSuccess.value ? '支付成功' : '支付失败'
})
const description = computed(() => {
  if (loading.value) return '正在从服务端同步订单与支付状态，请稍候。'
  if (isSuccess.value) {
    return '您的订单已支付完成，我们会尽快为您处理。'
  }
  if (statusMessage.value) {
    return statusMessage.value
  }
  return '支付结果尚未成功确认，请返回订单页重试或稍后刷新查看。'
})

const orderItems = computed(() => orderDetail.value?.items || [])

const formatCurrency = (value) => {
  const amount = Number(value)
  if (Number.isNaN(amount)) {
    return '--'
  }
  return amount.toFixed(2)
}

const resolvePayStatusLabel = (status) => {
  if (status === 'PAID') return '已支付'
  if (status === 'UNPAID') return '待支付'
  if (status === 'PENDING') return '确认中'
  if (status === 'FAILED') return '失败'
  return status || '--'
}

const resolveOrderNo = () => {
  return route.query.orderNo || route.query.out_trade_no || ''
}

const isUnauthorizedError = (error) => {
  return error?.response?.status === 401 || error?.status === 401
}

const resolveFallbackStatus = () => {
  const tradeStatus = route.query.trade_status
  if (tradeStatus === 'TRADE_SUCCESS' || tradeStatus === 'TRADE_FINISHED') {
    statusMessage.value = RESULT_MESSAGES.PENDING
    return 'PENDING'
  }
  if (route.query.status === 'success') {
    statusMessage.value = RESULT_MESSAGES.PENDING
    return 'PENDING'
  }
  statusMessage.value = RESULT_MESSAGES.QUERY_FAILED
  return 'FAILED'
}

const loadStatus = async () => {
  orderNo.value = resolveOrderNo()
  if (!orderNo.value) {
    payStatus.value = resolveFallbackStatus()
    return
  }

  loading.value = true
  try {
    const [paymentResult, orderResult] = await Promise.allSettled([
      getPaymentStatus(orderNo.value, { skipAuthRedirect: true }),
      getOrderDetail(orderNo.value, { skipAuthRedirect: true })
    ])
    const paymentRes = paymentResult.status === 'fulfilled' ? paymentResult.value : null
    const orderRes = orderResult.status === 'fulfilled' ? orderResult.value : null

    if (orderRes?.code === 0 && orderRes.data) {
      orderDetail.value = orderRes.data
    }

    if (paymentRes?.code === 0 && paymentRes.data?.payStatus) {
      payStatus.value = paymentRes.data.payStatus
    }

    if ((!payStatus.value || payStatus.value === 'UNPAID') && orderRes?.code === 0 && orderRes.data?.payStatus) {
      payStatus.value = orderRes.data.payStatus
    }

    if (!payStatus.value) {
      const paymentError = paymentResult.status === 'rejected' ? paymentResult.reason : null
      const orderError = orderResult.status === 'rejected' ? orderResult.reason : null
      if (isUnauthorizedError(paymentError) || isUnauthorizedError(orderError)) {
        payStatus.value = 'PENDING'
        statusMessage.value = RESULT_MESSAGES.UNAUTHORIZED
      } else {
        payStatus.value = resolveFallbackStatus()
      }
    }
  } catch (error) {
    if (isUnauthorizedError(error)) {
      payStatus.value = 'PENDING'
      statusMessage.value = RESULT_MESSAGES.UNAUTHORIZED
    } else {
      payStatus.value = resolveFallbackStatus()
    }
  } finally {
    loading.value = false
  }
}

const handleViewOrder = () => {
  router.push('/orders')
}

const handleGoHome = () => {
  router.push('/home')
}

onMounted(() => {
  loadStatus()
})
</script>

<template>
  <div class="result-layout">
    <header class="header">
      <button class="back-btn" @click="handleGoHome">← 首页</button>
      <h2>支付结果</h2>
      <div class="spacer"></div>
    </header>

    <main class="result-content">
      <div class="status-card">
        <div class="icon-wrapper" :class="{ success: isSuccess, failed: !isSuccess }">
          {{ loading ? '…' : isSuccess ? '✓' : '✗' }}
        </div>
        
        <h3 class="title">{{ title }}</h3>
        <p v-if="orderNo" class="order-no">订单号：{{ orderNo }}</p>
        <p class="desc">{{ description }}</p>

        <section v-if="orderDetail" class="summary-panel">
          <div class="summary-row">
            <span>支付状态</span>
            <strong>{{ resolvePayStatusLabel(orderDetail.payStatus || payStatus) }}</strong>
          </div>
          <div class="summary-row">
            <span>订单总金额</span>
            <strong>¥{{ formatCurrency(orderDetail.totalAmount) }}</strong>
          </div>
          <div class="summary-row products-row">
            <span>商品列表</span>
          </div>
          <div v-if="orderItems.length > 0" class="item-list">
            <div v-for="item in orderItems" :key="`${orderDetail.orderNo}_${item.productId}`" class="item-card">
              <img :src="item.imageUrl" :alt="item.title" class="item-image" />
              <div class="item-main">
                <p class="item-title">{{ item.title }}</p>
                <p class="item-meta">x{{ item.quantity }} · {{ item.shopName || '--' }}</p>
              </div>
              <div class="item-amount">¥{{ formatCurrency(item.amount ?? item.price) }}</div>
            </div>
          </div>
          <p v-else class="empty-items">暂无商品明细</p>
        </section>

        <div class="actions">
          <button class="btn btn-primary" @click="handleViewOrder">查看订单</button>
          <button class="btn btn-secondary" @click="handleGoHome">返回首页</button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.result-layout {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.back-btn {
  background: none;
  border: none;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  padding: 8px;
}

.spacer {
  width: 50px;
}

.result-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.status-card {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 400px;
  width: 100%;
}

.icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: bold;
  color: white;
  margin-bottom: 24px;
}

.success {
  background-color: #52c41a;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.failed {
  background-color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
}

.desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin: 0 0 32px 0;
}

.order-no {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 16px;
  width: 100%;
}

.summary-panel {
  width: 100%;
  margin: 10px 0 20px;
  padding: 14px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  background: #fafafa;
  text-align: left;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #444;
  margin-bottom: 10px;
}

.products-row {
  margin-bottom: 8px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-card {
  display: grid;
  grid-template-columns: 52px 1fr auto;
  gap: 10px;
  align-items: center;
  border: 1px solid #efefef;
  background: #fff;
  border-radius: 8px;
  padding: 8px;
}

.item-image {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 6px;
  background: #f5f5f5;
}

.item-main {
  min-width: 0;
}

.item-title {
  margin: 0;
  font-size: 13px;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: #888;
}

.item-amount {
  font-size: 13px;
  color: #111;
  font-weight: 600;
}

.empty-items {
  margin: 0;
  font-size: 13px;
  color: #888;
}

.btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
}

.btn:hover {
  opacity: 0.9;
}

.btn-primary {
  background-color: #1677ff;
  color: white;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #d9d9d9;
}
</style>
