<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { getMe } from '../../services/auth'
import { getProducts, searchProducts } from '../../services/product'
import { addToCart } from '../../services/cart'
import { buyNow as buyNowApi } from '../../services/order'
import { getOrderDetail } from '../../services/order'
import { createPayment } from '../../services/payment'
import ProductWaterfall from '../../components/ProductWaterfall.vue'
import ProductFocusModal from '../../components/ProductFocusModal.vue'
import PaymentConfirmModal from '../../components/PaymentConfirmModal.vue'

const router = useRouter()
const userStore = useUserStore()

// Standard Pagination & Search State
const keyword = ref('')
const products = ref([])
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const hasMore = ref(true)
const isSearchMode = ref(false)

// Modal State
const showModal = ref(false)
const selectedProduct = ref(null)
const payFormContainer = ref(null)
const paymentConfirmVisible = ref(false)
const pendingPaymentOrder = ref(null)
const paymentSubmitting = ref(false)

// Actions
const loadProducts = async (reset = false) => {
  if (loading.value || (!hasMore.value && !reset)) return
  
  if (reset) {
    page.value = 1
    products.value = []
    hasMore.value = true
  }
  
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      sortBy: 'price',
      sortOrder: 'asc'
    }
    
    let res
    if (isSearchMode.value && keyword.value.trim() !== '') {
      res = await searchProducts({ ...params, keyword: keyword.value.trim() })
    } else {
      res = await getProducts(params)
    }
    
    if (res.code === 0) {
      const newList = res.data.list || []
      products.value = [...products.value, ...newList]
      
      if (newList.length < pageSize.value) {
        hasMore.value = false
      } else {
        page.value++
      }
    }
  } catch (err) {
    console.error('Failed to load products:', err)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  isSearchMode.value = keyword.value.trim() !== ''
  loadProducts(true)
}

const handleKeydown = (e) => {
  if (e.key === 'Enter') {
    handleSearch()
  }
}

const handleProductClick = (product) => {
  selectedProduct.value = product
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const ensureLoggedIn = () => {
  if (userStore.token) {
    return true
  }
  alert('请先登录')
  router.push('/login')
  return false
}

const addCart = async (product) => {
  if (!ensureLoggedIn()) return
  try {
    const res = await addToCart({
      productId: product.id,
      quantity: 1
    })
    if (res.code === 0) {
      alert('加入购物车成功！')
      closeModal()
    } else {
      alert(res.message || '加入失败')
    }
  } catch (error) {
    alert(error.message || '网络错误')
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
    const paymentRes = await createPayment({ orderNo: order.orderNo })
    if (paymentRes.code === 0 && paymentRes.data?.payForm) {
      closePaymentConfirm()
      submitPayForm(paymentRes.data.payForm)
      return
    }

    alert(resolvePaymentErrorMessage(paymentRes.message))
    closePaymentConfirm()
    router.push('/orders')
  } catch (error) {
    alert(resolvePaymentErrorMessage(error?.message))
  } finally {
    paymentSubmitting.value = false
  }
}

const buyNow = async (product) => {
  if (!ensureLoggedIn()) return
  try {
    const res = await buyNowApi({
      productId: product.id,
      quantity: 1
    })
    if (res.code === 0) {
      const orderNo = res.data?.orderNo
      closeModal()
      if (!orderNo) {
        alert('下单成功，但未获取到订单号')
        router.push('/orders')
        return
      }
      await openPaymentConfirm(orderNo)
    } else {
      alert(res.message || '下单失败')
    }
  } catch (error) {
    alert(resolvePaymentErrorMessage(error?.message))
  }
}

// Lifecycle
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    getMe()
      .then(res => {
        if (res.code === 0) {
          userStore.setUserInfo(res.data)
        }
      })
      .catch(() => {})
  }
  loadProducts()
})
</script>

<template>
  <div class="home-layout">
    <header class="navbar">
      <div class="navbar-content">
        <div class="logo">
          <span class="logo-text">AI Shop</span>
        </div>
        
        <div class="search-container">
          <input 
            type="text" 
            v-model="keyword" 
            placeholder="搜索商品..." 
            class="search-input"
            @keydown="handleKeydown"
          />
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>
        
        <nav class="nav-links">
          <router-link to="/chat" class="nav-item ai-chat">AI 对话</router-link>
          <router-link to="/cart" class="nav-item">购物车</router-link>
          <router-link to="/orders" class="nav-item">订单</router-link>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <ProductWaterfall 
        :products="products"
        :loading="loading"
        :hasMore="hasMore"
        @load-more="loadProducts(false)"
        @click-product="handleProductClick"
      />
    </main>

    <ProductFocusModal 
      :show="showModal" 
      :product="selectedProduct" 
      @close="closeModal"
      @add-cart="addCart"
      @buy-now="buyNow"
    />

    <PaymentConfirmModal
      :show="paymentConfirmVisible"
      :order="pendingPaymentOrder"
      :submitting="paymentSubmitting"
      @close="closePaymentConfirm"
      @confirm="confirmPayment"
    />

    <div ref="payFormContainer" style="display: none;"></div>
  </div>
</template>

<style scoped>
.home-layout {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: 64px;
  display: flex;
  align-items: center;
}

.navbar-content {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #1677ff;
  letter-spacing: -0.5px;
}

.search-container {
  display: flex;
  flex: 1;
  max-width: 500px;
  margin: 0 40px;
  height: 40px;
}

.search-input {
  flex: 1;
  border: 2px solid #1677ff;
  border-right: none;
  border-radius: 20px 0 0 20px;
  padding: 0 20px;
  font-size: 14px;
  outline: none;
  transition: box-shadow 0.2s;
}

.search-input:focus {
  box-shadow: inset 0 1px 4px rgba(0,0,0,0.05);
}

.search-btn {
  background-color: #1677ff;
  color: white;
  border: none;
  border-radius: 0 20px 20px 0;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.search-btn:hover {
  opacity: 0.9;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-item {
  color: #333;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-item:hover {
  color: #1677ff;
}

.ai-chat {
  color: #722ed1;
  background-color: #f9f0ff;
  padding: 6px 16px;
  border-radius: 16px;
  border: 1px solid #d3adf7;
}

.ai-chat:hover {
  color: #531dab;
  background-color: #f3e8ff;
}

.main-content {
  flex: 1;
  width: 100%;
}
</style>
