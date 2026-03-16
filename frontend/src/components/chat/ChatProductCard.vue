<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['add-cart', 'buy-now'])
</script>

<template>
  <div class="chat-product-card">
    <div class="image-box">
      <img :src="product.imageUrl" :alt="product.title" />
    </div>
    <div class="info-box">
      <div class="title">{{ product.title }}</div>
      <div class="price-row">
        <span class="currency">¥</span>
        <span class="price">{{ product.price }}</span>
      </div>
      <div class="meta-row">
        <span>{{ product.shopName }}</span>
        <span>{{ product.sales }}</span>
      </div>
      <div class="actions">
        <button class="btn btn-cart" @click.stop="emit('add-cart', product)">加购</button>
        <button class="btn btn-buy" @click.stop="emit('buy-now', product)">购买</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-product-card {
  display: flex;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
  margin-top: 8px;
  max-width: 300px;
}

.image-box {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  background: #f5f5f5;
}

.image-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-box {
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.price-row {
  color: #ff4d4f;
  margin-bottom: 4px;
}

.currency {
  font-size: 11px;
}

.price {
  font-size: 14px;
  font-weight: 600;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #999;
  margin-bottom: 8px;
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}

.btn {
  flex: 1;
  padding: 4px 0;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn:hover {
  opacity: 0.8;
}

.btn-cart {
  background: #ffe4e6;
  color: #ff4d4f;
}

.btn-buy {
  background: #ff4d4f;
  color: white;
}
</style>
