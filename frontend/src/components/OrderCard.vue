<script setup>
import { defineProps, defineEmits, computed } from 'vue'

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['pay', 'delete'])

const formattedTime = computed(() => {
  if (!props.order.createdAt) return ''
  let timeValue = props.order.createdAt
  // Support if backend returns epoch seconds as float/int
  if (typeof timeValue === 'number' && timeValue < 99999999999) {
    // Determine if it's seconds by checking digits scale. Current seconds epoch is ~1.7 billion.
    if (timeValue < 10000000000) {
      timeValue = timeValue * 1000
    }
  } else if (typeof timeValue === 'string' && !isNaN(Number(timeValue))) {
    let num = Number(timeValue)
    if (num < 10000000000) num *= 1000
    timeValue = num
  }
  const date = new Date(timeValue)
  return date.toLocaleString()
})

const statusLabel = computed(() => {
  switch (props.order.payStatus) {
    case 'PAID': return '已支付'
    case 'UNPAID': return '待支付'
    case 'CANCELLED': return '已取消'
    default: return props.order.payStatus
  }
})

const statusClass = computed(() => {
  return props.order.payStatus ? props.order.payStatus.toLowerCase() : ''
})

const handlePay = () => {
  emit('pay', props.order)
}

const handleDelete = () => {
  emit('delete', props.order)
}
</script>

<template>
  <div class="order-card">
    <div class="card-header">
      <div class="header-left">
        <span class="order-no">订单号：{{ order.orderNo }}</span>
        <span class="time">{{ formattedTime }}</span>
      </div>
      <div class="header-right">
        <span :class="['status', statusClass]">{{ statusLabel }}</span>
      </div>
    </div>
    
    <div class="item-list">
      <div 
        v-for="item in order.items" 
        :key="item.productId" 
        class="order-item"
      >
        <div class="item-img">
          <img :src="item.imageUrl" :alt="item.title" />
        </div>
        <div class="item-info">
          <div class="title">{{ item.title }}</div>
          <div class="price-qty">
            <span class="price">¥{{ item.price }}</span>
            <span class="qty">x{{ item.quantity }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="card-footer">
      <div class="total">
        总计: <span class="amount">¥{{ order.totalAmount }}</span>
      </div>
      <div class="actions" v-if="order.payStatus === 'UNPAID'">
        <button class="delete-btn" @click="handleDelete">删除订单</button>
        <button class="pay-btn" @click="handlePay">立即支付</button>
      </div>
      <div class="actions" v-else>
        <button class="delete-btn" @click="handleDelete">删除订单</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-no {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.time {
  font-size: 12px;
  color: #999;
}

.status {
  font-size: 14px;
  font-weight: 500;
}

.status.paid {
  color: #52c41a;
}

.status.unpaid {
  color: #fa8c16;
}

.status.cancelled {
  color: #999;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.order-item {
  display: flex;
  gap: 12px;
  background: #fafafa;
  padding: 8px;
  border-radius: 4px;
}

.item-img {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.title {
  font-size: 14px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-qty {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.price {
  color: #ff4d4f;
  font-weight: 500;
}

.qty {
  color: #999;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.total {
  font-size: 14px;
  color: #333;
}

.amount {
  font-size: 18px;
  font-weight: 600;
  color: #ff4d4f;
}

.pay-btn {
  background: #ff4d4f;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 16px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.pay-btn:hover {
  opacity: 0.9;
}

.actions {
  display: flex;
  gap: 10px;
}

.delete-btn {
  background: #f1f5f9;
  color: #334155;
  border: none;
  padding: 8px 16px;
  border-radius: 16px;
  font-size: 14px;
  cursor: pointer;
}

.delete-btn:hover {
  background: #e2e8f0;
}
</style>
