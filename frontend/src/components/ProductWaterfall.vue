<script setup>
import { defineProps, defineEmits, onMounted, onUnmounted, ref } from 'vue'
import ProductCard from './ProductCard.vue'

const props = defineProps({
  products: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  hasMore: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['load-more', 'click-product'])
const observerTarget = ref(null)
let observer = null

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && !props.loading && props.hasMore) {
        emit('load-more')
      }
    },
    { threshold: 0.1 }
  )
  if (observerTarget.value) {
    observer.observe(observerTarget.value)
  }
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})

const handleProductClick = (product) => {
  emit('click-product', product)
}
</script>

<template>
  <div class="waterfall-container">
    <div v-if="products.length === 0 && !loading" class="empty-state">
      暂无商品数据
    </div>
    
    <div class="waterfall-grid">
      <div 
        v-for="product in products" 
        :key="product.id"
        class="waterfall-item"
      >
        <ProductCard :product="product" @click="handleProductClick(product)" />
      </div>
    </div>

    <!-- Infinite scroll trigger -->
    <div ref="observerTarget" class="loading-trigger">
      <div v-if="loading" class="loading-text">加载中...</div>
      <div v-else-if="!hasMore && products.length > 0" class="end-text">已经到底啦</div>
    </div>
  </div>
</template>

<style scoped>
.waterfall-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.waterfall-grid {
  /* Using CSS columns for a simple waterfall effect without JS calculation */
  column-count: 2;
  column-gap: 16px;
}

@media (min-width: 768px) {
  .waterfall-grid {
    column-count: 3;
    column-gap: 20px;
  }
}

@media (min-width: 1024px) {
  .waterfall-grid {
    column-count: 4;
    column-gap: 24px;
  }
}

@media (min-width: 1280px) {
  .waterfall-grid {
    column-count: 5;
    column-gap: 24px;
  }
}

.waterfall-item {
  break-inside: avoid;
  margin-bottom: 16px;
  /* Prevent cutting cards across columns */
  page-break-inside: avoid;
}

@media (min-width: 768px) {
  .waterfall-item {
    margin-bottom: 20px;
  }
}

@media (min-width: 1024px) {
  .waterfall-item {
    margin-bottom: 24px;
  }
}

.loading-trigger {
  text-align: center;
  padding: 30px 0;
  color: #999;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 100px 0;
  color: #999;
  font-size: 16px;
}
</style>
