<template>
  <div id="app-root">
    <!-- 启动界面 -->
    <Transition name="menu-leave">
      <MainMenu
        v-if="screen === 'menu'"
        @start-game="startNewGame"
        @load-game="loadGame"
      />
    </Transition>

    <!-- 游戏界面 -->
    <Transition name="game-enter">
      <div v-if="screen === 'game'" class="game-view" :class="{ 'game-view-visible': screen === 'game' }">
        <div class="game-container" style="padding:20px;font-family:Arial, sans-serif;display:flex;gap:20px;">
          <div>
            <h1 style="color:#e8d8b0;margin:0 0 10px;font-size:20px;letter-spacing:2px;">Zuul 游戏 Demo（起始场景）</h1>
            <GameCanvas @update="onUpdate" />
          </div>

          <aside style="width:320px;">
            <h3 style="color:#d4c4a0;">信息</h3>
            <div style="color:#ccc;"><strong style="color:#d4c4a0;">状态：</strong> {{ gameStatus }}</div>
            <div style="margin-top:8px;color:#ccc;"><strong style="color:#d4c4a0;">消息：</strong> {{ message }}</div>

            <h3 style="margin-top:16px;color:#d4c4a0;">房间物品</h3>
            <ul style="color:#bbb;">
              <li v-for="it in roomItems" :key="it.name">{{ it.name }} ({{ it.weight }} kg)</li>
              <li v-if="roomItems.length===0" style="color:#666;">（无）</li>
            </ul>

            <div style="margin-top:16px;display:flex;gap:8px;">
              <button @click="resetGame" style="background:#3a2a18;color:#d4c4a0;border:1px solid rgba(200,160,80,0.3);padding:6px 12px;border-radius:4px;cursor:pointer;">重置游戏</button>
              <button @click="backToMenu" style="background:#2a1a0e;color:#d4c4a0;border:1px solid rgba(200,160,80,0.2);padding:6px 12px;border-radius:4px;cursor:pointer;">返回菜单</button>
            </div>
          </aside>
        </div>
      </div>
    </Transition>

    <!-- 全屏过渡遮罩 -->
    <Transition name="transition-flash">
      <div v-if="transitioning" class="transition-overlay"></div>
    </Transition>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import GameCanvas from './components/GameCanvas.vue'
import MainMenu from './components/MainMenu.vue'

export default {
  components: { GameCanvas, MainMenu },
  setup() {
    const screen = ref('menu')
    const gameStatus = ref(null)
    const message = ref('')
    const data = ref(null)
    const roomItems = ref([])
    const transitioning = ref(false)

    // 启动游戏（新游戏）
    async function startNewGame() {
      // 过渡动画
      transitioning.value = true
      await new Promise(r => setTimeout(r, 600))

      // 重置后端
      try {
        await fetch('/api/reset', { method: 'POST' })
      } catch (e) {
        // ignore
      }

      // 切换到游戏
      screen.value = 'game'
      transitioning.value = false

      // 加载游戏数据
      await loadGameData()
    }

    // 继续冒险（读取存档，当前直接加载游戏）
    async function loadGame() {
      transitioning.value = true
      await new Promise(r => setTimeout(r, 600))

      screen.value = 'game'
      transitioning.value = false

      await loadGameData()
    }

    // 加载后端数据
    async function loadGameData() {
      try {
        const res = await fetch('/api/game')
        const j = await res.json()
        gameStatus.value = j.status
        message.value = j.message
        data.value = j.data
        if (j.data && j.data.items) {
          roomItems.value = j.data.items
        }
        try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
      } catch (e) {
        message.value = '无法连接后端: ' + e.message
      }
    }

    // 返回主菜单（带过渡动画）
    async function backToMenu() {
      // 过渡动画
      transitioning.value = true
      await new Promise(r => setTimeout(r, 600))

      // 在切换前停止 Phaser 游戏轮询，避免销毁时产生视觉错位
      try {
        window.dispatchEvent(new CustomEvent('game:pause'))
      } catch (e) {}

      screen.value = 'menu'
      gameStatus.value = null
      message.value = ''
      data.value = null
      roomItems.value = []
      transitioning.value = false
    }

    function onUpdate(payload) {
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
      try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
    }

    return {
      screen,
      gameStatus,
      message,
      data,
      onUpdate,
      roomItems,
      resetGame,
      startNewGame,
      loadGame,
      backToMenu,
      transitioning,
    }
  }
}
</script>

<style>
/* 全局样式调整 */
body {
  margin: 0;
  padding: 0;
  background: #0d0804;
}
</style>

<style scoped>
#app-root {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background: #0d0804;
  padding-top: 60px;
}

/* 游戏视图 */
.game-view {
  opacity: 0;
  transition: opacity 0.8s ease;
}

.game-view-visible {
  opacity: 1;
}

.game-container h1 {
  color: #e8d8b0;
  margin: 0 0 10px;
  font-size: 20px;
  letter-spacing: 2px;
  border-bottom: 1px solid rgba(200, 160, 80, 0.15);
  padding-bottom: 8px;
}

/* ============ 菜单离开动画 ============ */
.menu-leave-leave-active {
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.menu-leave-leave-from {
  opacity: 1;
  transform: scale(1);
}

.menu-leave-leave-to {
  opacity: 0;
  transform: scale(1.05);
}

/* ============ 游戏进入动画 ============ */
.game-enter-enter-active {
  transition: opacity 0.8s ease 0.2s;
}

.game-enter-enter-from {
  opacity: 0;
}

.game-enter-enter-to {
  opacity: 1;
}

/* ============ 游戏离开动画 ============ */
.game-enter-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.game-enter-leave-from {
  opacity: 1;
  transform: scale(1);
}

.game-enter-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

/* ============ 过渡遮罩 ============ */
.transition-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #0d0804;
  pointer-events: none;
}

.transition-flash-enter-active {
  transition: opacity 0.4s ease;
}

.transition-flash-leave-active {
  transition: opacity 0.6s ease 0.15s;
}

.transition-flash-enter-from,
.transition-flash-leave-to {
  opacity: 0;
}

.transition-flash-enter-to,
.transition-flash-leave-from {
  opacity: 1;
}
</style>
