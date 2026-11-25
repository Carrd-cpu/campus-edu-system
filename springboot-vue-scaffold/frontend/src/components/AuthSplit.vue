<template>
  <div class="auth-page" :class="{ 'is-admin': tone === 'admin' }">
    <section class="auth-cover">
      <img class="auth-cover-img" :src="cover" alt="" />
      <div class="auth-cover-mask" />
      <router-link class="auth-brand" to="/user/home">
        <BrandLogo size="lg" />
        <span>{{ systemStore.displayName }}</span>
      </router-link>
    </section>

    <section class="auth-side">
      <div class="auth-card">
        <header class="auth-card-head">
          <h2>{{ title }}</h2>
          <p>{{ lead }}</p>
        </header>
        <slot />
        <div class="auth-foot">
          <slot name="extra" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useSystemStore } from '@/stores/system'
import BrandLogo from '@/components/BrandLogo.vue'

defineProps({
  cover: { type: String, required: true },
  tone: { type: String, default: 'user' },
  title: { type: String, default: '' },
  lead: { type: String, default: '' }
})

const systemStore = useSystemStore()
</script>
