<script setup>
import { defineProps } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="product-card">
    <div class="image-wrapper">
      <img :src="product.imageUrl" :alt="product.title" loading="lazy" />
    </div>
    <div class="content">
      <h3 class="title">{{ product.title }}</h3>
      <div class="price-row">
        <span class="currency">¥</span>
        <span class="price">{{ product.price }}</span>
      </div>
      <div class="meta-row">
        <span class="shop">{{ product.shopName }}</span>
        <span class="sales">{{ product.sales }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.image-wrapper {
  width: 100%;
  padding-top: 100%; /* 1:1 aspect ratio roughly, can be removed for masonry */
  position: relative;
  background-color: #f7f7f7;
}

.image-wrapper img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-row {
  color: #ff4d4f;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.currency {
  font-size: 12px;
  font-weight: 500;
}

.price {
  font-size: 18px;
  font-weight: 600;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #999;
}

.shop {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 60%;
}

.sales {
  white-space: nowrap;
}
</style>
