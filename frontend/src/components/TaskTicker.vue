<template>
  <div class="reward-ticker" aria-live="polite">
    <div v-for="ev in events" :key="ev.id" class="reward-ev">
      <span class="coin" v-if="ev.gold">🪙 +{{ ev.gold }}</span>
      <span class="gem" v-if="ev.diamonds">💎 +{{ ev.diamonds }}</span>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue'
const props = defineProps({ events: { type: Array, default: () => [] } })
const events = props.events
</script>

<style scoped>
.reward-ticker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}
.reward-ev {
  display: inline-block;
  background: #161616;
  border: 1px solid #2a2a2a;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 14px;
  animation: pop 220ms ease-out;
}
@keyframes pop {
  from { transform: translateY(6px); opacity: 0 }
  to { transform: none; opacity: 1 }
}
.coin { color: #f8d66d; margin-right: 8px }
.gem { color: #7ecbff }
</style>
