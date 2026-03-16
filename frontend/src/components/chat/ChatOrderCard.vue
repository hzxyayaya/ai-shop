<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  order: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['view', 'pay'])
</script>

<template>
  <div class="chat-order-card">
    <div class="header">
      <span class="order-no">订单号: {{ order.orderNo }}</span>
      <span :class="['status', order.payStatus.toLowerCase()]">
        {{ order.payStatus === 'PAID' ? '已支付' : order.payStatus === 'UNPAID' ? '待支付' : '已取消' }}
      </span>
    </div>
    
    <div class="items" v-if="order.items && order.items.length > 0">
      <div class="item" v-for="item in order.items.slice(0, 2)" :key="item.productId">
        <img :src="item.imageUrl" :alt="item.title" class="item-img" />
        <div class="item-info">
          <div class="title">{{ item.title }}</div>
          <div class="price">¥{{ item.price }} x {{ item.quantity }}</div>
        </div>
      </div>
      <div v-if="order.items.length > 2" class="more-items">...等共 {{ order.items.length }} 件商品</div>
    </div>
    
    <div class="footer">
      <div class="total">
        总计: <span class="amount">¥{{ order.totalAmount }}</span>
      </div>
      <div class="actions">
        <button v-if="order.payStatus === 'UNPAID'" class="btn btn-pay" @click="emit('pay', order)">立即支付</button>
        <button class="btn btn-view" @click="emit('view', order)">查看详情</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-order-card {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
  max-width: 320px;
  font-size: 13px;
}

.header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.order-no {
  color: #666;
  font-family: monospace;
}

.status {
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

.items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.item {
  display: flex;
  gap: 8px;
}

.item-img {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
  background: #f5f5f5;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.title {
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price {
  color: #999;
  font-size: 12px;
}

.more-items {
  color: #999;
  font-size: 12px;
  text-align: center;
  padding: 4px 0;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.total {
  color: #333;
}

.amount {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn-pay {
  background: #ff4d4f;
  color: white;
}

.btn-view {
  background: white;
  border-color: #d9d9d9;
  color: #333;
}
</style>
