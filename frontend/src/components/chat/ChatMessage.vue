<script setup>
import { defineProps, defineEmits } from 'vue'
import ChatProductCard from './ChatProductCard.vue'
import ChatOrderCard from './ChatOrderCard.vue'
import ChatActions from './ChatActions.vue'

const props = defineProps({
  msg: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['add-cart', 'buy-now', 'action', 'pay-order', 'view-order'])

const handleAction = (action) => {
  emit('action', action)
}
</script>

<template>
  <div :class="['chat-message', msg.role]">
    <div :class="['avatar', msg.role === 'ai' ? 'ai-avatar' : 'user-avatar']">
      <template v-if="msg.role === 'ai'">AI</template>
      <template v-else>U</template>
    </div>
    <div class="message-content">
      <!-- Text Message -->
      <div v-if="msg.content" class="text-bubble" v-html="msg.content.replace(/\n/g, '<br>')"></div>
      
      <!-- AI Only Attachments -->
      <template v-if="msg.role === 'ai'">
        <!-- Products -->
        <div v-if="msg.products && msg.products.length > 0" class="products-container">
          <ChatProductCard 
            v-for="product in msg.products" 
            :key="product.id"
            :product="product"
            @add-cart="p => emit('add-cart', p)"
            @buy-now="p => emit('buy-now', p)"
          />
        </div>
        
        <!-- Orders -->
        <div v-if="msg.orders && msg.orders.length > 0" class="orders-container">
          <ChatOrderCard 
            v-for="order in msg.orders" 
            :key="order.orderNo"
            :order="order"
            @pay="o => emit('pay-order', o)"
            @view="o => emit('view-order', o)"
          />
        </div>
        
        <!-- Actions -->
        <ChatActions 
          v-if="msg.actions && msg.actions.length > 0" 
          :actions="msg.actions"
          @action="handleAction"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  width: min(768px, calc(100% - 32px));
  margin-bottom: 32px;
  gap: 16px;
}

.chat-message.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.avatar {
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

.user-avatar {
  background: #f4f4f4;
  color: #000;
  border: 1px solid #e5e5e5;
}

.ai-avatar {
  background: #000;
  color: #fff;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 80%;
  flex: 1;
}

.user .message-content {
  align-items: flex-end;
}

.text-bubble {
  font-size: 15px;
  line-height: 1.6;
  color: #000000;
  word-break: break-all;
}

.user .text-bubble {
  padding: 12px 16px;
  background: #f4f4f4;
  border-radius: 20px;
}

.ai .text-bubble {
  padding: 4px 0;
  background: transparent;
}

.products-container, .orders-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
</style>
