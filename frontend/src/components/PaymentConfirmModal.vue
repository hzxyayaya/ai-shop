<script setup>
import { computed, defineEmits, defineProps, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: null
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'confirm'])

const orderItems = computed(() => props.order?.items || [])

const formatCurrency = (value) => {
  const amount = Number(value)
  if (Number.isNaN(amount)) {
    return value ?? '--'
  }
  return amount.toFixed(2)
}

const handleClose = () => {
  if (props.submitting) return
  emit('close')
}

const handleConfirm = () => {
  if (!props.order || props.submitting) return
  emit('confirm', props.order)
}

const handleKeydown = (event) => {
  if (props.show && event.key === 'Escape') {
    handleClose()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div v-if="show && order" class="modal-overlay" @click="handleClose">
    <div class="modal-card" @click.stop>
      <button class="close-btn" @click="handleClose">×</button>
      <div class="eyebrow">PAYMENT CONFIRM</div>
      <h2 class="title">确认订单后跳转支付宝</h2>

      <div class="summary-grid">
        <div class="summary-item">
          <span class="summary-label">订单号</span>
          <span class="summary-value">{{ order.orderNo }}</span>
        </div>
        <div class="summary-item amount-box">
          <span class="summary-label">应付金额</span>
          <span class="summary-amount">¥{{ formatCurrency(order.totalAmount) }}</span>
        </div>
      </div>

      <div class="item-list">
        <div
          v-for="item in orderItems"
          :key="`${order.orderNo}_${item.productId}`"
          class="item-card"
        >
          <img :src="item.imageUrl" :alt="item.title" class="item-image" />
          <div class="item-content">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <span>{{ item.shopName || 'AI Shop' }}</span>
              <span>x{{ item.quantity || 1 }}</span>
            </div>
          </div>
          <div class="item-price">¥{{ formatCurrency(item.amount ?? item.price) }}</div>
        </div>
      </div>

      <div class="notice">
        跳转支付宝后请核对订单标题和金额。完整订单明细以当前确认页和订单页为准。
      </div>

      <div class="actions">
        <button class="ghost-btn" @click="handleClose" :disabled="submitting">再看看</button>
        <button class="primary-btn" @click="handleConfirm" :disabled="submitting">
          {{ submitting ? '正在跳转...' : '确认并去支付' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.32);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 24px;
}

.modal-card {
  width: min(720px, 100%);
  background: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%);
  border-radius: 24px;
  padding: 28px;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.18);
  position: relative;
}

.close-btn {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #eef2f7;
  color: #475569;
  font-size: 24px;
  cursor: pointer;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.16em;
  color: #94a3b8;
  margin-bottom: 10px;
}

.title {
  margin: 0 0 20px;
  font-size: 28px;
  color: #0f172a;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
  margin-bottom: 18px;
}

.summary-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  font-size: 12px;
  color: #64748b;
}

.summary-value {
  font-size: 16px;
  color: #0f172a;
  word-break: break-all;
}

.amount-box {
  background: linear-gradient(135deg, #fff1f2 0%, #ffffff 100%);
}

.summary-amount {
  font-size: 30px;
  font-weight: 700;
  color: #e11d48;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.item-card {
  display: grid;
  grid-template-columns: 76px 1fr auto;
  gap: 14px;
  align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 12px;
}

.item-image {
  width: 76px;
  height: 76px;
  object-fit: cover;
  border-radius: 14px;
  background: #f8fafc;
}

.item-title {
  font-size: 15px;
  line-height: 1.5;
  color: #0f172a;
  margin-bottom: 8px;
}

.item-meta {
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: #64748b;
}

.item-price {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.notice {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.6;
}

.actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.ghost-btn,
.primary-btn {
  border: none;
  border-radius: 999px;
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.ghost-btn {
  background: #e2e8f0;
  color: #0f172a;
}

.primary-btn {
  background: #0f172a;
  color: #ffffff;
}

.ghost-btn:disabled,
.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .modal-card {
    padding: 22px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .item-card {
    grid-template-columns: 64px 1fr;
  }

  .item-price {
    grid-column: 2;
  }

  .actions {
    flex-direction: column-reverse;
  }
}
</style>
