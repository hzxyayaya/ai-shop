<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  actions: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['action'])
</script>

<template>
  <div class="chat-actions" v-if="actions && actions.length > 0">
    <button 
      v-for="(action, index) in actions" 
      :key="index"
      class="action-btn"
      @click="emit('action', action)"
    >
      {{ action.label }}
    </button>
  </div>
</template>

<style scoped>
.chat-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.action-btn {
  background: white;
  border: 1px solid #1677ff;
  color: #1677ff;
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e6f4ff;
}
</style>
