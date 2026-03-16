<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCart, updateCartQuantity, updateCartChecked, deleteCartItem } from '../../services/cart'
import { checkout, getOrderDetail } from '../../services/order'
import { createPayment } from '../../services/payment'
import CartItem from '../../components/CartItem.vue'
import PaymentConfirmModal from '../../components/PaymentConfirmModal.vue'

const router = useRouter()
const cartItems = ref([])
const loading = ref(false)
const checkoutLoading = ref(false)
const payFormContainer = ref(null)
const paymentConfirmVisible = ref(false)
const pendingPaymentOrder = ref(null)
const paymentSubmitting = ref(false)

const fetchCart = async () => {
  loading.value = true
  try {
    const res = await getCart()
    if (res.code === 0 && res.data) {
      cartItems.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (err) {
    console.error('Failed to load cart', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCart()
})

// Computations
const selectedCount = computed(() => {
  return cartItems.value.filter(item => item.checked).length
})

const totalAmount = computed(() => {
  const total = cartItems.value
    .filter(item => item.checked)
    .reduce((sum, item) => sum + (item.price * item.quantity), 0)
  return total.toFixed(2)
})

const isAllChecked = computed(() => {
  if (cartItems.value.length === 0) return false
  return cartItems.value.every(item => item.checked)
})

// Actions
const handleUpdateQuantity = async (id, quantity) => {
  try {
    const res = await updateCartQuantity(id, { quantity })
    if (res.code === 0) {
      const item = cartItems.value.find(i => i.id === id)
      if (item) item.quantity = quantity
    } else {
      alert(res.message || '更新数量失败')
    }
  } catch (err) {
    alert('网络错误')
  }
}

const handleUpdateChecked = async (id, checked) => {
  try {
    const res = await updateCartChecked(id, { checked })
    if (res.code === 0) {
      const item = cartItems.value.find(i => i.id === id)
      if (item) item.checked = checked
    } else {
      alert(res.message || '更新状态失败')
    }
  } catch (err) {
    alert('网络错误')
  }
}

const handleDelete = async (id) => {
  if (!confirm('确定要删除该商品吗？')) return
  try {
    const res = await deleteCartItem(id)
    if (res.code === 0) {
      cartItems.value = cartItems.value.filter(i => i.id !== id)
    } else {
      alert(res.message || '删除失败')
    }
  } catch (err) {
    alert('网络错误')
  }
}

const toggleAll = async (e) => {
  const targetChecked = e.target.checked
  const itemsToUpdate = cartItems.value.filter(item => item.checked !== targetChecked)
  
  if (itemsToUpdate.length === 0) return
  
  // Ideally there's a bulk operation, but we loop here since standard API usually targets IDs
  loading.value = true
  try {
    await Promise.all(itemsToUpdate.map(item => updateCartChecked(item.id, { checked: targetChecked })))
    // Refresh fully to ensure synchronization
    await fetchCart()
  } catch (err) {
    alert('批量更新失败')
    loading.value = false
  }
}

const handleCheckout = async () => {
  if (selectedCount.value === 0 || checkoutLoading.value) return
  
  const cartItemIds = cartItems.value
    .filter(item => item.checked)
    .map(item => item.id)
  
  checkoutLoading.value = true
  try {
    const res = await checkout({ cartItemIds })
    if (res.code === 0) {
      const orderNo = res.data?.orderNo
      await fetchCart()
      if (!orderNo) {
        alert('结算成功，但未获取到订单号')
        router.push('/orders')
        return
      }
      await openPaymentConfirm(orderNo)
    } else {
      alert(res.message || '结算失败')
    }
  } catch (err) {
    alert('网络错误')
  } finally {
    checkoutLoading.value = false
  }
}

const resolvePaymentErrorMessage = (message) => {
  if (message?.includes('alipay sandbox config missing')) {
    return '支付服务尚未完成支付宝沙箱配置，请先在后端配置 ALIPAY_APP_ID、ALIPAY_PRIVATE_KEY、ALIPAY_PUBLIC_KEY。'
  }
  return message || '获取支付信息失败'
}

const submitPayForm = (payForm) => {
  payFormContainer.value.innerHTML = payForm
  const form = payFormContainer.value.querySelector('form')
  if (!form) {
    throw new Error('pay form parse failed')
  }
  form.submit()
}

const openPaymentConfirm = async (orderNo) => {
  const detailRes = await getOrderDetail(orderNo)
  if (detailRes.code !== 0 || !detailRes.data) {
    throw new Error(detailRes.message || '获取订单详情失败')
  }
  pendingPaymentOrder.value = detailRes.data
  paymentConfirmVisible.value = true
}

const closePaymentConfirm = () => {
  if (paymentSubmitting.value) return
  paymentConfirmVisible.value = false
  pendingPaymentOrder.value = null
}

const confirmPayment = async (order) => {
  paymentSubmitting.value = true
  try {
    const res = await createPayment({ orderNo: order.orderNo })
    if (res.code === 0 && res.data?.payForm) {
      closePaymentConfirm()
      submitPayForm(res.data.payForm)
      return
    }
    alert(resolvePaymentErrorMessage(res.message))
    closePaymentConfirm()
    router.push('/orders')
  } catch (err) {
    alert(resolvePaymentErrorMessage(err?.message))
  } finally {
    paymentSubmitting.value = false
  }
}
</script>

<template>
  <div class="cart-layout">
    <header class="header">
      <button class="back-btn" @click="router.push('/home')">← 返回首页</button>
      <h2>购物车</h2>
      <div class="spacer"></div>
    </header>

    <main class="main-content">
      <div v-if="loading && cartItems.length === 0" class="loading-state">
        加载中...
      </div>
      
      <div v-else-if="cartItems.length === 0" class="empty-state">
        <div class="empty-icon">🛒</div>
        <p>购物车空空如也</p>
        <button class="go-shop-btn" @click="router.push('/home')">去逛逛</button>
      </div>

      <div v-else class="cart-list">
        <CartItem 
          v-for="item in cartItems" 
          :key="item.id"
          :item="item"
          @update-quantity="handleUpdateQuantity"
          @update-checked="handleUpdateChecked"
          @delete="handleDelete"
        />
      </div>
    </main>

    <!-- Bottom Checkout Bar -->
    <div class="checkout-bar" v-if="cartItems.length > 0">
      <div class="left-section">
        <label class="check-all">
          <input type="checkbox" :checked="isAllChecked" @change="toggleAll" :disabled="loading" />
          <span>全选</span>
        </label>
      </div>
      
      <div class="right-section">
        <div class="total-info">
          <span class="label">合计:</span>
          <span class="currency">¥</span>
          <span class="amount">{{ totalAmount }}</span>
        </div>
        <button 
          class="checkout-btn" 
          :disabled="selectedCount === 0 || checkoutLoading"
          @click="handleCheckout"
        >
          {{ checkoutLoading ? '结算中...' : `结算(${selectedCount})` }}
        </button>
      </div>
    </div>
  </div>

  <PaymentConfirmModal
    :show="paymentConfirmVisible"
    :order="pendingPaymentOrder"
    :submitting="paymentSubmitting"
    @close="closePaymentConfirm"
    @confirm="confirmPayment"
  />

  <div ref="payFormContainer" style="display: none;"></div>
</template>

<style scoped>
.cart-layout {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
  padding-bottom: 80px; /* Space for fixed bottom bar */
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
  width: 70px;
}

.main-content {
  flex: 1;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}

.cart-list {
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

.checkout-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 72px;
  background: white;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
  max-width: 800px;
  margin: 0 auto;
  border-top-left-radius: 16px;
  border-top-right-radius: 16px;
}

@media (min-width: 800px) {
  .checkout-bar {
    border-radius: 12px;
    bottom: 24px;
    box-shadow: 0 4px 24px rgba(0,0,0,0.1);
  }
  .cart-layout {
    padding-bottom: 40px;
  }
}

.left-section {
  display: flex;
  align-items: center;
}

.check-all {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.check-all input {
  width: 18px;
  height: 18px;
  accent-color: #1677ff;
  cursor: pointer;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.total-info {
  display: flex;
  align-items: baseline;
}

.label {
  font-size: 14px;
  color: #333;
  margin-right: 8px;
}

.currency {
  font-size: 14px;
  color: #ff4d4f;
  margin-right: 2px;
}

.amount {
  font-size: 24px;
  font-weight: 600;
  color: #ff4d4f;
}

.checkout-btn {
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 24px;
  height: 40px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.checkout-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.checkout-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
