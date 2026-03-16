<script setup>
import { defineProps, defineEmits, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    required: false
  },
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'add-cart', 'buy-now'])

const handleClose = () => {
  emit('close')
}

const handleAddCart = () => {
  if (props.product) {
    emit('add-cart', props.product)
  }
}

const handleBuyNow = () => {
  if (props.product) {
    emit('buy-now', props.product)
  }
}

const handleKeydown = (e) => {
  if (props.show && e.key === 'Escape') {
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
  <div v-if="show && product" class="modal-overlay" @click="handleClose">
    <div class="modal-wrapper" @click.stop>
      <button class="close-btn" @click="handleClose">×</button>
      
      <div class="modal-content">
        <div class="image-section">
          <img :src="product.imageUrl" :alt="product.title" />
        </div>
        
        <div class="info-section">
          <div class="category-tag" v-if="product.category">{{ product.category }}</div>
          <h2 class="title">{{ product.title }}</h2>
          
          <div class="price-row">
            <span class="currency">¥</span>
            <span class="price">{{ product.price }}</span>
          </div>
          
          <div class="meta-row">
            <div class="shop-info">
              <span class="label">店铺：</span>
              <span class="value">{{ product.shopName }}</span>
            </div>
            <div class="sales-info">
              <span class="label">销量：</span>
              <span class="value">{{ product.sales }}</span>
            </div>
          </div>
          
          <div class="action-buttons">
            <button class="btn btn-cart" @click="handleAddCart">加入购物车</button>
            <button class="btn btn-buy" @click="handleBuyNow">立即购买</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-wrapper {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 800px;
  position: relative;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  animation: modal-pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
}

@keyframes modal-pop {
  0% { transform: scale(0.9); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  font-size: 20px;
  line-height: 1;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #333;
}

.modal-content {
  display: flex;
  flex-direction: row;
  height: 500px;
}

.image-section {
  flex: 1;
  background-color: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-section img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-section {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.category-tag {
  display: inline-block;
  padding: 4px 8px;
  background-color: #f0f5ff;
  color: #1677ff;
  font-size: 12px;
  border-radius: 4px;
  margin-bottom: 12px;
  align-self: flex-start;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  line-height: 1.5;
  margin: 0 0 24px 0;
}

.price-row {
  color: #ff4d4f;
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 32px;
}

.currency {
  font-size: 16px;
  font-weight: 500;
}

.price {
  font-size: 32px;
  font-weight: 600;
}

.meta-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: auto;
}

.shop-info, .sales-info {
  display: flex;
  font-size: 14px;
}

.label {
  color: #999;
  width: 60px;
}

.value {
  color: #333;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 24px;
}

.btn {
  flex: 1;
  padding: 14px 0;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
}

.btn:hover {
  opacity: 0.9;
}

.btn-cart {
  background-color: #ffe4e6;
  color: #ff4d4f;
}

.btn-buy {
  background-color: #ff4d4f;
  color: white;
}

@media (max-width: 768px) {
  .modal-content {
    flex-direction: column;
    height: auto;
    max-height: 90vh;
    overflow-y: auto;
  }
  
  .image-section {
    height: 300px;
    flex: none;
  }
  
  .info-section {
    padding: 24px;
  }
}
</style>
