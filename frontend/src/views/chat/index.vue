<script setup>
import { computed, ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { deleteChatSession, postChatStream } from '../../services/chat'
import { addToCart } from '../../services/cart'
import { buyNow, getOrderDetail } from '../../services/order'
import { createPayment } from '../../services/payment'
import { useUserStore } from '../../stores/user'
import ChatMessage from '../../components/chat/ChatMessage.vue'
import PaymentConfirmModal from '../../components/PaymentConfirmModal.vue'

const router = useRouter()
const userStore = useUserStore()
const CHAT_SESSIONS_KEY_PREFIX = 'chatSessions:'
const CHAT_ACTIVE_SESSION_KEY_PREFIX = 'chatSessionId:'
const CHAT_MESSAGES_PREFIX = 'chatMessages:'

const sessionId = ref('')
const inputText = ref('')
const loading = ref(false)
const messages = ref([])
const sessions = ref([])
const chatContainer = ref(null)
const payFormContainer = ref(null)
const sidebarOpen = ref(false)
const pendingAiMessageId = ref('')
const paymentConfirmVisible = ref(false)
const pendingPaymentOrder = ref(null)
const paymentSubmitting = ref(false)

const resolveUserScope = () => {
  const user = userStore.userInfo
  if (user?.id !== undefined && user?.id !== null) {
    return `u_${user.id}`
  }
  if (user?.account) {
    return `a_${user.account}`
  }
  if (user?.email) {
    return `e_${user.email}`
  }
  return 'guest'
}

const getSessionsStorageKey = () => `${CHAT_SESSIONS_KEY_PREFIX}${resolveUserScope()}`
const getActiveSessionStorageKey = () => `${CHAT_ACTIVE_SESSION_KEY_PREFIX}${resolveUserScope()}`
const getMessagesStorageKey = (targetSessionId) => `${CHAT_MESSAGES_PREFIX}${resolveUserScope()}:${targetSessionId}`

const createMessage = (role, content, extra = {}) => ({
  id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  role,
  content,
  ...extra
})

const buildWelcomeMessage = () =>
  createMessage(
    'ai',
    '你好！我是 AI 导购助手，有什么可以帮您的？您可以让我帮忙找商品、推荐商品或者查看订单。'
  )

const createSessionRecord = (title = '新会话') => ({
  id: `session_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
  title,
  preview: '开始新的购物对话',
  updatedAt: Date.now()
})

const formatSessionTime = timestamp => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  if (sameDay) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString([], { month: '2-digit', day: '2-digit' })
}

const persistSessions = () => {
  localStorage.setItem(getSessionsStorageKey(), JSON.stringify(sessions.value))
}

const persistMessages = (targetSessionId) => {
  localStorage.setItem(getMessagesStorageKey(targetSessionId), JSON.stringify(messages.value))
}

const loadMessages = (targetSessionId) => {
  const raw = localStorage.getItem(getMessagesStorageKey(targetSessionId))
  if (!raw) {
    return [buildWelcomeMessage()]
  }
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : [buildWelcomeMessage()]
  } catch {
    return [buildWelcomeMessage()]
  }
}

const syncActiveSession = (targetSessionId) => {
  sessionId.value = targetSessionId
  sessionStorage.setItem(getActiveSessionStorageKey(), targetSessionId)
}

const upsertSessionMeta = (targetSessionId, updater) => {
  const index = sessions.value.findIndex(item => item.id === targetSessionId)
  if (index < 0) return
  const current = sessions.value[index]
  sessions.value[index] = {
    ...current,
    ...updater(current),
    updatedAt: Date.now()
  }
  sessions.value = [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt)
  persistSessions()
}

const switchSession = async (targetSessionId) => {
  syncActiveSession(targetSessionId)
  messages.value = loadMessages(targetSessionId)
  sidebarOpen.value = false
  await forceScrollToBottom()
}

const createNewSession = async () => {
  const newSession = createSessionRecord()
  sessions.value = [newSession, ...sessions.value]
  persistSessions()
  messages.value = [buildWelcomeMessage()]
  persistMessages(newSession.id)
  await switchSession(newSession.id)
}

const deleteSession = async (targetSessionId) => {
  const target = sessions.value.find(item => item.id === targetSessionId)
  if (!target) return
  if (!confirm(`确定删除会话“${target.title}”吗？`)) return

  try {
    const res = await deleteChatSession(targetSessionId)
    if (res && res.code !== 0) {
      alert(res.message || '删除会话失败')
      return
    }
  } catch (error) {
    if (error?.response?.status !== 404) {
      alert(error?.response?.data?.message || error?.message || '删除会话失败')
      return
    }
  }

  sessions.value = sessions.value.filter(item => item.id !== targetSessionId)
  persistSessions()
  localStorage.removeItem(getMessagesStorageKey(targetSessionId))

  if (sessionId.value === targetSessionId) {
    if (sessions.value.length === 0) {
      await createNewSession()
      return
    }
    await switchSession(sessions.value[0].id)
    return
  }
}

const activeSession = computed(() =>
  sessions.value.find(item => item.id === sessionId.value) ?? null
)

const initChatStateForCurrentUser = () => {
  let storedSessions = []
  try {
    storedSessions = JSON.parse(localStorage.getItem(getSessionsStorageKey()) || '[]')
  } catch {
    storedSessions = []
  }
  sessions.value = Array.isArray(storedSessions) ? storedSessions : []

  let storedSessionId = sessionStorage.getItem(getActiveSessionStorageKey())
  if (!storedSessionId || !sessions.value.some(item => item.id === storedSessionId)) {
    if (sessions.value.length === 0) {
      const initialSession = createSessionRecord('默认会话')
      sessions.value = [initialSession]
      persistSessions()
      localStorage.setItem(
        getMessagesStorageKey(initialSession.id),
        JSON.stringify([buildWelcomeMessage()])
      )
      storedSessionId = initialSession.id
    } else {
      storedSessionId = sessions.value[0].id
    }
  }

  syncActiveSession(storedSessionId)
  messages.value = loadMessages(storedSessionId)
  forceScrollToBottom()
}

onMounted(() => {
  initChatStateForCurrentUser()
})

watch(
  () => `${userStore.userInfo?.id || ''}:${userStore.token || ''}`,
  () => {
    initChatStateForCurrentUser()
  }
)

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const forceScrollToBottom = async () => {
  await scrollToBottom()
  requestAnimationFrame(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
  setTimeout(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  }, 120)
}

const ensureLoggedIn = () => {
  const token = localStorage.getItem('token')
  if (token) {
    return true
  }
  alert('请先登录')
  router.push('/login')
  return false
}

const sendMessage = async () => {
  if (!ensureLoggedIn()) return
  if (!inputText.value.trim() || loading.value) return
  
  const userText = inputText.value.trim()
  inputText.value = ''
  
  // Add user message
  messages.value.push(createMessage('user', userText))
  persistMessages(sessionId.value)
  upsertSessionMeta(sessionId.value, current => ({
    title: current.title === '新会话' || current.title === '默认会话'
      ? userText.slice(0, 12)
      : current.title,
    preview: userText.slice(0, 24)
  }))
  
  forceScrollToBottom()
  loading.value = true
  const aiMessage = createMessage('ai', '')
  pendingAiMessageId.value = aiMessage.id
  messages.value.push(aiMessage)
  persistMessages(sessionId.value)
  
  try {
    await postChatStream({
      sessionId: sessionId.value,
      message: userText
    }, {
      onEvent: event => {
        const targetMessage = messages.value.find(item => item.id === pendingAiMessageId.value)
        if (!targetMessage) return

        if (event.type === 'status' || event.type === 'stage') {
          if (!targetMessage.content) {
            targetMessage.content = event.message || ''
          } else if (event.type === 'stage') {
            targetMessage.content = event.message || targetMessage.content
          }
        } else if (event.type === 'message_delta') {
          if (targetMessage.content.startsWith('正在') || targetMessage.content.startsWith('识别到')) {
            targetMessage.content = ''
          }
          targetMessage.content += event.delta || ''
        } else if (event.type === 'complete' && event.response) {
          targetMessage.content = event.response.message || targetMessage.content || '已完成处理'
          targetMessage.products = event.response.products || []
          targetMessage.orders = event.response.orders || []
          targetMessage.actions = event.response.actions || []
          persistMessages(sessionId.value)
          upsertSessionMeta(sessionId.value, () => ({
            preview: (event.response.message || '已收到 AI 回复').slice(0, 24)
          }))
          const payNowAction = (event.response.actions || []).find(action => action.type === 'PAY_NOW')
          if (event.response.intent === 'BUY_NOW' && payNowAction?.targetId) {
            handlePayNow(payNowAction.targetId)
          }
        } else if (event.type === 'error') {
          targetMessage.content = event.message || '抱歉，系统出现了一些问题，请稍后再试。'
          persistMessages(sessionId.value)
        }

        forceScrollToBottom()
      }
    })
  } catch (error) {
    const targetMessage = messages.value.find(item => item.id === pendingAiMessageId.value)
    if (targetMessage) {
      targetMessage.content = '网络错误，无法连接到 AI 助手，请稍后再试。'
    } else {
      messages.value.push(createMessage('ai', '网络错误，无法连接到 AI 助手，请稍后再试。'))
    }
    persistMessages(sessionId.value)
  } finally {
    pendingAiMessageId.value = ''
    loading.value = false
    persistMessages(sessionId.value)
    forceScrollToBottom()
  }
}

// Action Handlers
const handleAddCart = async (productOrTargetId) => {
  if (!ensureLoggedIn()) return
  const productId = typeof productOrTargetId === 'object' ? productOrTargetId.id : productOrTargetId
  try {
    const res = await addToCart({ productId, quantity: 1 })
    if (res.code === 0) alert('已成功加入购物车')
    else alert(res.message || '加入购物车失败')
  } catch (e) {
    alert('网络错误')
  }
}

const handleBuyNow = async (productOrTargetId) => {
  if (!ensureLoggedIn()) return
  const productId = typeof productOrTargetId === 'object' ? productOrTargetId.id : productOrTargetId
  try {
    const res = await buyNow({ productId, quantity: 1 })
    if (res.code === 0) {
      const orderNo = res.data?.orderNo
      if (!orderNo) {
        alert('下单成功，但未获取到订单号')
        return
      }
      await openPaymentConfirm(orderNo)
      return
    }
    alert(res.message || '下单失败')
  } catch (e) {
    alert(resolvePaymentErrorMessage(e?.message))
  }
}

const submitPayForm = (payForm) => {
  payFormContainer.value.innerHTML = payForm
  const form = payFormContainer.value.querySelector('form')
  if (!form) {
    throw new Error('pay form parse failed')
  }
  form.submit()
}

const resolvePaymentErrorMessage = (message) => {
  if (message?.includes('alipay sandbox config missing')) {
    return '支付服务尚未完成支付宝沙箱配置，请先在后端配置 ALIPAY_APP_ID、ALIPAY_PRIVATE_KEY、ALIPAY_PUBLIC_KEY。'
  }
  return message || '获取支付信息失败'
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
  } catch (e) {
    alert(resolvePaymentErrorMessage(e?.message))
  } finally {
    paymentSubmitting.value = false
  }
}

const handlePayNow = async (orderNo) => {
  if (!ensureLoggedIn()) return
  try {
    await openPaymentConfirm(orderNo)
  } catch (e) {
    alert(resolvePaymentErrorMessage(e?.message))
  }
}

const handleAction = async (action) => {
  switch (action.type) {
    case 'ADD_TO_CART':
      await handleAddCart(action.targetId)
      break
    case 'BUY_NOW':
      await handleBuyNow(action.targetId)
      break
    case 'GO_CART':
      router.push('/cart')
      break
    case 'PAY_NOW':
      await handlePayNow(action.targetId)
      break
    case 'VIEW_ORDER':
      router.push('/orders')
      break
    case 'GO_HOME':
      router.push('/home')
      break
    default:
      console.warn('Unknown action type:', action.type)
  }
}

const handlePayOrder = async (order) => {
  await handlePayNow(order.orderNo)
}

const handleViewOrder = (order) => {
  router.push('/orders')
}

watch(
  () => messages.value.length,
  () => {
    forceScrollToBottom()
  }
)
</script>

<template>
  <div class="chat-page">
    <!-- Sidebar -->
    <aside :class="['session-sidebar', { open: sidebarOpen }]">
      <div class="sidebar-topbar">
        <button class="session-toggle" @click="sidebarOpen = !sidebarOpen">关闭</button>
      </div>

      <div class="sidebar-header">
        <button class="brand-btn" @click="router.push('/home')">← 返回首页</button>
        <button class="new-session-btn" @click="createNewSession">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 4V20M4 12H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
      </div>

      <div class="sidebar-section-label">今天</div>
      <div class="session-list">
        <div
          v-for="item in sessions"
          :key="item.id"
          :class="['session-item', { active: item.id === sessionId }]"
          @click="switchSession(item.id)"
        >
          <div class="session-item-row">
            <div class="session-copy">
              <div class="session-title">{{ item.title }}</div>
              <div class="session-preview">{{ item.preview || '开始新的购物对话' }}</div>
            </div>
            <div class="session-meta">
              <span class="session-time">{{ formatSessionTime(item.updatedAt) }}</span>
              <button class="session-delete-btn" @click.stop="deleteSession(item.id)">×</button>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Chat Area -->
    <div class="chat-panel">
      <header class="chat-header">
        <button class="session-open-btn" @click="sidebarOpen = true">☰</button>
        <div class="chat-header-main">
          <h1 class="header-title">AI 导购助手</h1>
        </div>
      </header>

      <div class="chat-container" ref="chatContainer">
        <ChatMessage 
          v-for="msg in messages" 
          :key="msg.id" 
          :msg="msg"
          @add-cart="handleAddCart"
          @buy-now="handleBuyNow"
          @action="handleAction"
          @pay-order="handlePayOrder"
          @view-order="handleViewOrder"
        />
        
        <div v-if="loading" class="chat-message ai loading-msg">
          <div class="avatar ai-avatar">AI</div>
          <div class="message-content">
            <div class="text-bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <button class="attach-btn">+</button>
          <input 
            v-model="inputText" 
            type="text" 
            placeholder="有问题，尽管问" 
            @keydown.enter="sendMessage"
            :disabled="loading"
          />
          <button class="send-btn" @click="sendMessage" :disabled="!inputText.trim() || loading">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 19V5M12 5L5 12M12 5L19 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
        <div class="input-footer">AI 助手可能会犯错。请核实重要信息。</div>
      </div>

      <PaymentConfirmModal
        :show="paymentConfirmVisible"
        :order="pendingPaymentOrder"
        :submitting="paymentSubmitting"
        @close="closePaymentConfirm"
        @confirm="confirmPayment"
      />
    </div>

    <div ref="payFormContainer" style="display: none;"></div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  width: 100%;
  background: #ffffff;
  color: #000000;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  text-align: left;
  overflow: hidden;
}

.session-sidebar {
  width: 260px;
  min-width: 260px;
  background: #f9f9f9;
  border-right: none;
  padding: 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
  box-sizing: border-box;
  overflow: hidden;
}

.sidebar-topbar {
  display: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand-btn {
  background: transparent;
  border: none;
  color: #000000;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 8px;
}

.brand-btn:hover {
  background: #ececec;
}

.new-session-btn {
  background: transparent;
  color: #000000;
  border: none;
  border-radius: 8px;
  padding: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-session-btn:hover {
  background: #ececec;
}

.sidebar-section-label {
  padding: 0 8px;
  font-size: 12px;
  font-weight: 500;
  color: #666666;
  margin-top: 10px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  overflow-x: hidden;
  min-width: 0;
}

.session-item {
  width: 100%;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 14px;
  padding: 12px 12px;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
  box-sizing: border-box;
  min-width: 0;
}

.session-item:hover {
  background: #eef2f6;
}

.session-item.active {
  background: #e8f0ff;
}

.session-title {
  font-size: 14px;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
}

.session-item-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.session-copy {
  min-width: 0;
  flex: 1;
}

.session-preview {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.session-time {
  font-size: 11px;
  color: #94a3b8;
}

.session-delete-btn {
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  color: #94a3b8;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, color 0.15s;
}

.session-item:hover .session-delete-btn,
.session-item.active .session-delete-btn {
  opacity: 1;
}

.session-delete-btn:hover {
  background: #dbe2ea;
  color: #334155;
}

.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  position: relative;
  overflow: hidden;
}

.chat-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: #ffffff;
  flex-shrink: 0;
}

.session-open-btn {
  display: none;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.session-open-btn:hover {
  background: #f4f4f4;
}

.chat-header-main {
  display: flex;
  align-items: center;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #000000;
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: #666666;
}

.chat-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chat-container::-webkit-scrollbar {
  width: 8px;
}

.chat-container::-webkit-scrollbar-track {
  background: transparent;
}

.chat-container::-webkit-scrollbar-thumb {
  background-color: #e5e5e5;
  border-radius: 4px;
}

.chat-container::-webkit-scrollbar-thumb:hover {
  background-color: #cccccc;
}

.input-area {
  padding: 16px 24px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, #ffffff 20%);
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 768px;
  width: 100%;
  padding: 8px 12px;
  background: #f4f4f4;
  border-radius: 24px;
  border: 1px solid transparent;
}

.input-wrapper:focus-within {
  border-color: #e5e5e5;
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}

.attach-btn {
  background: transparent;
  border: none;
  font-size: 20px;
  color: #000000;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attach-btn:hover {
  background: #e5e5e5;
}

input {
  flex: 1;
  padding: 8px 4px;
  border: none;
  background: transparent;
  font-size: 15px;
  outline: none;
  color: #000000;
}

input::placeholder {
  color: #8e8e8e;
}

.send-btn {
  width: 32px;
  height: 32px;
  background: #000000;
  color: #ffffff;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 0.2s;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.8;
}

.send-btn:disabled {
  background: #e5e5e5;
  color: #a3a3a3;
  cursor: not-allowed;
}

.input-footer {
  font-size: 12px;
  color: #666666;
  margin-top: 8px;
  text-align: center;
}

.loading-msg {
  display: flex;
  width: min(768px, calc(100% - 32px));
  margin-bottom: 32px;
  gap: 16px;
}
.loading-msg .avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
}
.loading-msg .ai-avatar {
  background: #000;
  color: #fff;
}
.loading-msg .message-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* Typing animation */
.typing span {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #666;
  margin: 0 2px;
  animation: typing 1s infinite;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

@media (max-width: 768px) {
  .session-sidebar {
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 20;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
    box-shadow: 4px 0 12px rgba(0,0,0,0.1);
  }

  .session-sidebar.open {
    transform: translateX(0);
  }

  .session-open-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}
</style>

