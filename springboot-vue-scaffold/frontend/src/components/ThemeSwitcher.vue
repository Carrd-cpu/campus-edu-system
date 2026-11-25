<template>
  <el-dropdown trigger="click" popper-class="admin-user-dropdown" @command="themeStore.setTheme">
    <div class="toolbar-item theme-item" title="切换主题">
      <el-icon :size="16"><Brush /></el-icon>
      <span>主题</span>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in THEME_PRESETS"
          :key="item.key"
          :command="item.key"
          :class="{ active: themeStore.currentKey === item.key }"
        >
          <span class="color-dot" :style="{ background: item.active }" />
          <span class="theme-name">{{ item.label }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { useThemeStore, THEME_PRESETS } from '@/stores/theme'

const themeStore = useThemeStore()
</script>

<style scoped>
.color-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
  vertical-align: -1px;
  flex-shrink: 0;
}

.theme-name {
  flex: 1;
}
</style>
