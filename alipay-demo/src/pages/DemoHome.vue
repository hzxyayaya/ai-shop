<script setup>
import { computed, onMounted, ref } from 'vue'
import { login, getMe } from '../api/auth'
import { getProducts } from '../api/product'
import { buyNow, getOrderDetail, getOrders } from '../api/order'
import { createPayment, getPaymentStatus } from '../api/payment'
import { useSessionStore } from '../store/session'

const session = useSessionStore()

const loginForm = ref({
  account: '',
  password: ''
})
const loginLoading = ref(false)
const loginError = ref('')

const products = ref([])
const productsLoading = ref(false)
const orders = ref([])
const ordersLoading = ref(false)
const actionMessage = ref('')
const payFormContainer = ref(null)
const buyingProductId = ref(null)
const productQuantities = ref({})

const userName = computed(() => session.user?.nickname || session.user?.username || '未登录')

const setActionMessage = (message) => {
  actionMessage.value = message
}

const resolvePaymentErrorMessage = (message) => {
  if (message?.includes('alipay sandbox config missing')) {
    return '后端还没有配好支付宝沙箱参数，请先设置 ALIPAY_APP_ID、ALIPAY_PRIVATE_KEY、ALIPAY_PUBLIC_KEY。'
  }
  return message || '支付请求失败'
}

const ensureQuantity = (productId) => {
  if (!productQuantities.value[productId]) {
    productQuantities.value[productId] = 1
  }
  return Number(productQuantities.value[productId]) || 1
}

const loadProducts = async () => {
  productsLoading.value = true
  try {
    const res = await getProducts({ page: 1, pageSize: 12, sortBy: 'price', sortOrder: 'asc' })
    if (res.code === 0) {
      products.value = res.data?.list || []
      for (const product of products.value) {
        ensureQuantity(product.id)
      }
    } else {
      setActionMessage(res.message || '商品加载失败')
    }
  } catch (error) {
    setActionMessage(error.message || '商品加载失败')
  } finally {
    productsLoading.value = false
  }
}

const loadOrders = async () => {
  if (!session.token) {
    orders.value = []
    return
  }
  ordersLoading.value = true
  try {
    const res = await getOrders({ page: 1, pageSize: 20 })
    if (res.code === 0) {
      orders.value = res.data?.list || []
    } else {
      setActionMessage(res.message || '订单加载失败')
    }
  } catch (error) {
    setActionMessage(error.message || '订单加载失败')
  } finally {
    ordersLoading.value = false
  }
}

const refreshOrder = async (orderNo) => {
  try {
    const [paymentRes, orderRes] = await Promise.all([
      getPaymentStatus(orderNo),
      getOrderDetail(orderNo)
    ])

    const nextPayStatus = paymentRes.code === 0
      ? paymentRes.data?.payStatus
      : orderRes.data?.payStatus

    orders.value = orders.value.map((order) => {
      if (order.orderNo !== orderNo) {
        return order
      }
      return {
        ...order,
        ...(orderRes.code === 0 && orderRes.data ? orderRes.data : {}),
        payStatus: nextPayStatus || order.payStatus
      }
    })
    setActionMessage(`订单 ${orderNo} 已刷新`)
  } catch (error) {
    setActionMessage(error.message || '刷新订单失败')
  }
}

const submitPayForm = (payForm) => {
  payFormContainer.value.innerHTML = payForm
  const form = payFormContainer.value.querySelector('form')
  if (!form) {
    throw new Error('支付宝表单解析失败')
  }
  form.submit()
}

const handleCreatePayment = async (orderNo) => {
  try {
    const res = await createPayment({ orderNo })
    if (res.code === 0 && res.data?.payForm) {
      session.setLastOrderNo(orderNo)
      submitPayForm(res.data.payForm)
      return
    }
    setActionMessage(resolvePaymentErrorMessage(res.message))
  } catch (error) {
    setActionMessage(resolvePaymentErrorMessage(error?.message))
  }
}

const handleBuyNow = async (product) => {
  const quantity = Math.max(1, ensureQuantity(product.id))
  buyingProductId.value = product.id
  try {
    const res = await buyNow({
      productId: product.id,
      quantity
    })
    if (res.code === 0 && res.data?.orderNo) {
      session.setLastOrderNo(res.data.orderNo)
      setActionMessage(`订单 ${res.data.orderNo} 创建成功`)
      await loadOrders()
      await handleCreatePayment(res.data.orderNo)
    } else {
      setActionMessage(res.message || '创建订单失败')
    }
  } catch (error) {
    setActionMessage(error.message || '创建订单失败')
  } finally {
    buyingProductId.value = null
  }
}

const handleLogin = async () => {
  if (!loginForm.value.account || !loginForm.value.password) {
    loginError.value = '请输入账号和密码'
    return
  }
  loginLoading.value = true
  loginError.value = ''
  try {
    const res = await login(loginForm.value)
    if (res.code !== 0 || !res.data?.token) {
      loginError.value = res.message || '登录失败'
      return
    }
    session.setSession(res.data.token, res.data.user)
    setActionMessage('登录成功，已加载购买演示数据')
    await Promise.all([loadProducts(), loadOrders()])
  } catch (error) {
    loginError.value = error.message || '登录失败'
  } finally {
    loginLoading.value = false
  }
}

const handleLogout = () => {
  session.clearSession()
  orders.value = []
  setActionMessage('已退出当前演示账号')
}

onMounted(async () => {
  if (session.token) {
    try {
      const me = await getMe()
      if (me.code === 0 && me.data) {
        session.setSession(session.token, me.data)
      }
    } catch {
      session.clearSession()
    }
  }

  await loadProducts()
  if (session.token) {
    await loadOrders()
  }
})
</script>

<template>
  <div class="demo-shell">
    <aside class="demo-side">
      <div class="brand-block">
        <p class="eyebrow">Alipay Sandbox</p>
        <h1>订单购买 Demo</h1>
        <p class="side-copy">
          这里只保留“登录、立即购买、发起支付宝支付、刷新订单状态”这条链路。
        </p>
      </div>

      <div class="panel">
        <div class="panel-head">
          <h2>账号</h2>
          <button v-if="session.token" class="ghost-btn" @click="handleLogout">退出</button>
        </div>

        <template v-if="!session.token">
          <label class="field-label" for="account">用户名或邮箱</label>
          <input id="account" v-model="loginForm.account" class="text-input" placeholder="orion" />

          <label class="field-label" for="password">密码</label>
          <input id="password" v-model="loginForm.password" class="text-input" type="password" placeholder="12345678" />

          <button class="primary-btn" :disabled="loginLoading" @click="handleLogin">
            {{ loginLoading ? '登录中...' : '登录后购买' }}
          </button>

          <p v-if="loginError" class="error-text">{{ loginError }}</p>
        </template>

        <template v-else>
          <div class="user-card">
            <p class="mini-label">当前用户</p>
            <strong>{{ userName }}</strong>
            <span>{{ session.user?.email || '无邮箱信息' }}</span>
          </div>
          <button class="secondary-btn" @click="loadOrders">刷新订单列表</button>
        </template>
      </div>

      <div class="panel note-panel">
        <h2>演示说明</h2>
        <ul class="note-list">
          <li>点击商品卡片上的“立即购买并支付”会调用 `/api/orders/buy-now`。</li>
          <li>下单成功后立刻调用 `/api/payments/create`，自动跳转支付宝沙箱。</li>
          <li>支付回跳后打开 `/payment/result`，也可以回首页手动刷新订单。</li>
        </ul>
      </div>
    </aside>

    <main class="demo-main">
      <section class="hero-card">
        <div>
          <p class="eyebrow">Purchase Flow</p>
          <h2>最小闭环</h2>
        </div>
        <div class="hero-status">
          <span>商品</span>
          <span>下单</span>
          <span>支付</span>
          <span>刷新</span>
        </div>
      </section>

      <p v-if="actionMessage" class="action-banner">{{ actionMessage }}</p>

      <section class="section-block">
        <div class="section-head">
          <h2>可购买商品</h2>
          <button class="ghost-btn" @click="loadProducts">刷新商品</button>
        </div>

        <div v-if="productsLoading" class="state-card">商品加载中...</div>
        <div v-else class="product-grid">
          <article v-for="product in products" :key="product.id" class="product-card">
            <img :src="product.imageUrl" :alt="product.title" class="product-image" />
            <div class="product-body">
              <p class="product-category">{{ product.category }}</p>
              <h3>{{ product.title }}</h3>
              <p class="product-meta">{{ product.shopName }}</p>
              <p class="product-sales">{{ product.sales }}</p>
              <div class="product-footer">
                <strong>¥{{ product.price }}</strong>
                <input
                  v-model.number="productQuantities[product.id]"
                  class="qty-input"
                  type="number"
                  min="1"
                />
              </div>
              <button
                class="primary-btn"
                :disabled="!session.token || buyingProductId === product.id"
                @click="handleBuyNow(product)"
              >
                {{ buyingProductId === product.id ? '下单中...' : '立即购买并支付' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-head">
          <h2>我的订单</h2>
          <button class="ghost-btn" :disabled="!session.token || ordersLoading" @click="loadOrders">
            {{ ordersLoading ? '刷新中...' : '刷新列表' }}
          </button>
        </div>

        <div v-if="!session.token" class="state-card">登录后才会显示订单。</div>
        <div v-else-if="ordersLoading && orders.length === 0" class="state-card">订单加载中...</div>
        <div v-else-if="orders.length === 0" class="state-card">暂无订单，先完成一次购买。</div>
        <div v-else class="order-list">
          <article v-for="order in orders" :key="order.orderNo" class="order-card">
            <div class="order-top">
              <div>
                <p class="mini-label">订单号</p>
                <strong>{{ order.orderNo }}</strong>
              </div>
              <div class="status-chip" :class="order.payStatus?.toLowerCase()">
                {{ order.payStatus }}
              </div>
            </div>

            <div class="order-items">
              <div v-for="item in order.items || []" :key="`${order.orderNo}-${item.productId}`" class="order-item">
                <span>{{ item.title }}</span>
                <span>x{{ item.quantity }}</span>
              </div>
            </div>

            <div class="order-bottom">
              <div>
                <p class="mini-label">总金额</p>
                <strong>¥{{ order.totalAmount }}</strong>
              </div>
              <div class="order-actions">
                <button class="secondary-btn" @click="refreshOrder(order.orderNo)">刷新订单</button>
                <button
                  v-if="order.payStatus === 'UNPAID'"
                  class="primary-btn"
                  @click="handleCreatePayment(order.orderNo)"
                >
                  去支付
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <div ref="payFormContainer" style="display: none"></div>
  </div>
</template>
