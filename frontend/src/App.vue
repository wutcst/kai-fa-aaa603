<template>
  <div style="padding:20px;font-family:Arial, sans-serif;">
    <h1>Zuul 简易前端（Vue）</h1>

    <div><strong>status:</strong> {{ gameStatus }}</div>
    <div><strong>message:</strong> {{ message }}</div>

    <div style="margin-top:12px;">
      <button @click="sendHelp">帮助 (help)</button>
    </div>

    <div style="margin-top:20px;">
      <h3>当前房间 data（调试）</h3>
      <pre style="background:#f6f6f6;padding:10px;white-space:pre-wrap;">{{ prettyData }}</pre>
    </div>

    <div style="margin-top:12px;color:#666;">说明：此为最小 demo，后续会实现完整交互式 UI。</div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'

export default {
  setup() {
    const gameStatus = ref(null)
    const message = ref('')
    const data = ref(null)

    onMounted(async () => {
      try {
        const res = await fetch('/api/game')
        const j = await res.json()
        gameStatus.value = j.status
        message.value = j.message
        data.value = j.data
      } catch (e) {
        message.value = '无法连接后端: ' + e.message
      }
    })

    const sendHelp = async () => {
      const res = await fetch('/api/command', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: 'help' })
      })
      const j = await res.json()
      message.value = j.message
      data.value = j.data
    }

    const prettyData = computed(() => JSON.stringify(data.value, null, 2))

    return { gameStatus, message, data, sendHelp, prettyData }
  }
}
</script>

