<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update-checked', 'update-quantity', 'delete'])

const handleCheckedChange = (e) => {
  emit('update-checked', props.item.id, e.target.checked)
}

const handleQuantityMinus = () => {
  if (props.item.quantity > 1) {
    emit('update-quantity', props.item.id, props.item.quantity - 1)
  }
}

const handleQuantityPlus = () => {
  emit('update-quantity', props.item.id, props.item.quantity + 1)
}

const handleQuantityBlur = (e) => {
  const val = parseInt(e.target.value)
  if (isNaN(val) || val < 1) {
    // Reset to current logic if invalid
    e.target.value = props.item.quantity
  } else if (val !== props.item.quantity) {
    emit('update-quantity', props.item.id, val)
  }
}

const handleDelete = () => {
  emit('delete', props.item.id)
}
</script>

<template>
  <div class="cart-item">
    <div class="checkbox-col">
      <input type="checkbox" :checked="item.checked" @change="handleCheckedChange" />
    </div>
    
    <div class="product-info-col">
      <div class="product-img">
        <img :src="item.imageUrl" :alt="item.title" />
      </div>
      <div class="product-detail">
        <div class="title">{{ item.title }}</div>
        <div class="price">
          <span class="currency">¥</span><span>{{ item.price }}</span>
        </div>
      </div>
    </div>
    
    <div class="actions-col">
      <div class="quantity-control">
        <button class="qty-btn" @click="handleQuantityMinus" :disabled="item.quantity <= 1">-</button>
        <input class="qty-input" type="number" :value="item.quantity" @blur="handleQuantityBlur" />
        <button class="qty-btn" @click="handleQuantityPlus">+</button>
      </div>
      
      <button class="delete-btn" @click="handleDelete">删除</button>
    </div>
  </div>
</template>

<style scoped>
.cart-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.checkbox-col {
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkbox-col input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #1677ff;
  cursor: pointer;
}

.product-info-col {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 0 16px;
}

.product-img {
  width: 100px;
  height: 100px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-detail {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px 0;
}

.title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price {
  color: #ff4d4f;
  font-size: 18px;
  font-weight: 600;
}

.currency {
  font-size: 14px;
  margin-right: 2px;
}

.actions-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 24px;
  width: 120px;
}

.quantity-control {
  display: flex;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
}

.qty-btn {
  width: 28px;
  height: 28px;
  background: #fafafa;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  font-size: 16px;
}

.qty-btn:hover:not(:disabled) {
  background: #f0f0f0;
  color: #1677ff;
}

.qty-btn:disabled {
  color: #bfbfbf;
  cursor: not-allowed;
}

.qty-input {
  width: 40px;
  height: 28px;
  border: none;
  border-left: 1px solid #d9d9d9;
  border-right: 1px solid #d9d9d9;
  text-align: center;
  font-size: 14px;
  color: #333;
  outline: none;
}

.qty-input::-webkit-inner-spin-button,
.qty-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.delete-btn {
  background: none;
  border: none;
  color: #999;
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s;
}

.delete-btn:hover {
  color: #ff4d4f;
}

@media (max-width: 768px) {
  .cart-item {
    flex-wrap: wrap;
    position: relative;
    padding: 16px;
  }
  
  .actions-col {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    margin-top: 16px;
    padding-left: 40px; /* Align with image */
  }
}
</style>
