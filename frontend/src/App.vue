<template>
  <div style="padding:20px;font-family:Arial, sans-serif;display:flex;gap:20px;">
    <div>
      <h1>Zuul 游戏 Demo（起始场景）</h1>
      <GameCanvas @update="onUpdate" />
    </div>

    <aside style="width:320px;">
      <h3>信息</h3>
      <div><strong>状态：</strong> {{ gameStatus }}</div>
      <div style="margin-top:8px;"><strong>消息：</strong> {{ message }}</div>

      <h3 style="margin-top:16px;">房间物品</h3>
      <ul>
        <li v-for="it in roomItems" :key="it.name">{{ it.name }} ({{ it.weight }} kg)</li>
        <li v-if="roomItems.length===0" style="color:#888">（无）</li>
      </ul>

      <div style="margin-top:16px;">
        <button @click="resetGame">重置游戏</button>
      </div>
    </aside>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import GameCanvas from './components/GameCanvas.vue'

export default {
  components: { GameCanvas },
  setup() {
    const gameStatus = ref(null)
    const message = ref('')
    const data = ref(null)
    const roomItems = ref([])

    onMounted(async () => {
      try {
        const res = await fetch('/api/game')
        const j = await res.json()
        gameStatus.value = j.status
        message.value = j.message
        data.value = j.data
        // extract inventory if present
        if (j.data && j.data.items) {
          roomItems.value = j.data.items
        }
        // notify canvas / other listeners of initial game state
        try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
      } catch (e) {
        message.value = '无法连接后端: ' + e.message
      }
    })

    const onUpdate = (payload) => {
      if (!payload) return
      message.value = payload.message || ''
      gameStatus.value = payload.status || ''
      data.value = payload.data || null
      if (payload.data && payload.data.items) {
        roomItems.value = payload.data.items
      }
    }

    const resetGame = async () => {
      const res = await fetch('/api/reset', { method: 'POST' })
      const j = await res.json()
      onUpdate(j)
      // notify canvas / other listeners of reset
      try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
    }

    return { gameStatus, message, data, onUpdate, roomItems, resetGame }
  }
}
</script>

