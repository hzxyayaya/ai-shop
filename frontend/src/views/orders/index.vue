<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrders, getOrderDetail, deleteOrder } from '../../services/order'
import { createPayment } from '../../services/payment'
import OrderCard from '../../components/OrderCard.vue'
import PaymentConfirmModal from '../../components/PaymentConfirmModal.vue'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const payFormContainer = ref(null)
const paymentConfirmVisible = ref(false)
const pendingPaymentOrder = ref(null)
const paymentSubmitting = ref(false)

const resolvePaymentErrorMessage = (message) => {
  if (message?.includes('alipay sandbox config missing')) {
    return '支付服务尚未完成支付宝沙箱配置，请先在后端配置 ALIPAY_APP_ID、ALIPAY_PRIVATE_KEY、ALIPAY_PUBLIC_KEY。'
  }
  return message || '获取支付信息失败'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrders({ page: 1, pageSize: 50 })
    if (res.code === 0 && res.data) {
      orders.value = res.data.list || []
    }
  } catch (err) {
    console.error('Failed to load orders', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrders()
})

const handlePay = async (order) => {
  try {
    const detailRes = await getOrderDetail(order.orderNo)
    if (detailRes.code !== 0 || !detailRes.data) {
      alert(detailRes.message || '获取订单详情失败')
      return
    }
    pendingPaymentOrder.value = detailRes.data
    paymentConfirmVisible.value = true
  } catch (err) {
    alert(resolvePaymentErrorMessage(err?.message))
  }
}

const handleDelete = async (order) => {
  if (!confirm(`确定删除订单 ${order.orderNo} 吗？`)) return
  try {
    const res = await deleteOrder(order.orderNo)
    if (!res || res.code === 0) {
      orders.value = orders.value.filter(item => item.orderNo !== order.orderNo)
      return
    }
    alert(res.message || '删除订单失败')
  } catch (err) {
    if (err?.response?.status === 404) {
      orders.value = orders.value.filter(item => item.orderNo !== order.orderNo)
      return
    }
    alert(err?.response?.data?.message || err?.message || '删除订单失败')
  }
}

const closePaymentConfirm = () => {
  if (paymentSubmitting.value) return
  paymentConfirmVisible.value = false
  pendingPaymentOrder.value = null
}

const submitPayForm = (payFormHtml) => {
  payFormContainer.value.innerHTML = payFormHtml
  const form = payFormContainer.value.querySelector('form')
  if (!form) {
    throw new Error('解析支付表单失败')
  }
  form.submit()
}

const confirmPayment = async (order) => {
  paymentSubmitting.value = true
  try {
    const res = await createPayment({ orderNo: order.orderNo })
    if (res.code === 0 && res.data && res.data.payForm) {
      closePaymentConfirm()
      submitPayForm(res.data.payForm)
    } else {
      alert(resolvePaymentErrorMessage(res.message))
    }
  } catch (err) {
    alert(resolvePaymentErrorMessage(err?.message))
  } finally {
    paymentSubmitting.value = false
  }
}
</script>

<template>
  <div class="orders-layout">
    <header class="header">
      <button class="back-btn" @click="router.push('/home')">← 返回首页</button>
      <h2>我的订单</h2>
      <div class="spacer"></div>
    </header>

    <main class="main-content">
      <div v-if="loading && orders.length === 0" class="loading-state">
        加载中...
      </div>
      
      <div v-else-if="orders.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <p>暂无订单记录</p>
        <button class="go-shop-btn" @click="router.push('/home')">去逛逛</button>
      </div>

      <div v-else class="orders-list">
        <OrderCard 
          v-for="order in orders" 
          :key="order.orderNo"
          :order="order"
          @pay="handlePay"
          @delete="handleDelete"
        />
      </div>
    </main>

    <PaymentConfirmModal
      :show="paymentConfirmVisible"
      :order="pendingPaymentOrder"
      :submitting="paymentSubmitting"
      @close="closePaymentConfirm"
      @confirm="confirmPayment"
    />

    <!-- Hidden container for gateway injection -->
    <div ref="payFormContainer" style="display: none;"></div>
  </div>
</template>

<style scoped>
.orders-layout {
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
  width: 70px; /* Balance header */
}

.main-content {
  flex: 1;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}

.orders-list {
  display: flex;
  flex-direction: column;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.go-shop-btn {
  margin-top: 16px;
  background: #1677ff;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 32px;
  font-size: 14px;
  cursor: pointer;
}
</style>
