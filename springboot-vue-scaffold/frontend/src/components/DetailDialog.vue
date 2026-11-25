<template>
  <el-dialog
    :model-value="modelValue"
    class="detail-dialog"
    :title="title"
    width="560px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="detail-grid">
      <div v-for="(item, index) in fields" :key="`${item.label}-${index}`" class="detail-row">
        <div class="detail-label">{{ item.label }}</div>
        <div class="detail-value">
          <el-avatar v-if="item.type === 'avatar'" :size="48" :src="item.src">
            {{ item.fallback || '—' }}
          </el-avatar>
          <el-image
            v-else-if="item.type === 'image' && item.src"
            :src="item.src"
            :preview-src-list="[item.src]"
            fit="cover"
            class="detail-image"
            preview-teleported
          />
          <el-tag
            v-else-if="item.type === 'tag'"
            :type="item.tagType || 'info'"
            effect="light"
            round
          >
            {{ displayText(item.value) }}
          </el-tag>
          <span v-else :class="{ empty: isEmpty(item.value) }">{{ displayText(item.value) }}</span>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '详情' },
  fields: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])

function isEmpty(value) {
  return value == null || String(value).trim() === ''
}

function displayText(value) {
  if (isEmpty(value)) return '—'
  return value
}
</script>

<style scoped>
.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: none;
  border-radius: 0;
  overflow: hidden;
  background: transparent;
  box-shadow: none;
}

.detail-row {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 12px;
  padding: 10px 4px;
  border: none;
}

.detail-row:nth-child(even) {
  background: transparent;
}

.detail-label {
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.6;
  letter-spacing: 0.02em;
}

.detail-value {
  color: #0f172a;
  font-size: 14px;
  font-weight: 550;
  line-height: 1.6;
  word-break: break-word;
}

.detail-value .empty {
  color: #cbd5e1;
  font-weight: 400;
}

.detail-image {
  width: 148px;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}
</style>
