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
      <div v-if="screen === 'game'" class="game-view">
        <GameCanvas
          @update="onUpdate"
          @reset-game="resetGame"
          @back-to-menu="backToMenu"
        />
      </div>
    </Transition>

    <!-- 全屏过渡遮罩 -->
    <Transition name="transition-flash">
      <div v-if="transitioning" class="transition-overlay"></div>
    </Transition>
  </div>
</template>

<script>
import { ref } from 'vue'
import GameCanvas from './components/GameCanvas.vue'
import MainMenu from './components/MainMenu.vue'

// 过渡动画时长常量（毫秒）
const TRANSITION_MASK_IN = 800
const TRANSITION_MASK_PAUSE = 400
const TRANSITION_PHASER_MOUNT = 400
const TRANSITION_MENU_RENDER = 800

export default {
  components: { GameCanvas, MainMenu },
  setup() {
    const screen = ref('menu')
    const gameStatus = ref(null)
    const message = ref('')
    const roomItems = ref([])
    const transitioning = ref(false)

    // 将后端响应统一应用到组件状态
    function applyGameResponse(j) {
      if (!j) return
      gameStatus.value = j.status ?? null
      message.value = j.message ?? ''
      roomItems.value = (j.data && j.data.items) ? j.data.items : []
    }

    // 通知 GameCanvas 组件更新
    function notifyGameCanvas(j) {
      try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
    }

    // 场景过渡：封装从遮罩到游戏画面完整呈现的流程
    async function transitionToGame(fnBeforeEnter) {
      transitioning.value = true
      await new Promise(r => setTimeout(r, TRANSITION_MASK_IN))

      if (fnBeforeEnter) {
        await fnBeforeEnter()
      }

      // 切换到游戏界面（遮罩仍在覆盖）
      screen.value = 'game'

      // 等待 Phaser 挂载 + 首次渲染完成后再移除遮罩
      await new Promise(r => setTimeout(r, TRANSITION_PHASER_MOUNT))
      await loadGameData()
      await new Promise(r => requestAnimationFrame(r))
      transitioning.value = false
    }

    // 启动游戏（新游戏）
    async function startNewGame() {
      await transitionToGame(async () => {
        try {
          await fetch('/api/reset', { method: 'POST' })
        } catch (e) {
          // ignore
        }
      })
    }

    // 继续冒险（读取存档，当前未实现数据库，直接加载游戏）
    async function loadGame() {
      await transitionToGame(null)
    }

    // 加载后端数据
    async function loadGameData() {
      try {
        const res = await fetch('/api/game')
        const j = await res.json()
        applyGameResponse(j)
        notifyGameCanvas(j)
      } catch (e) {
        message.value = '无法连接后端: ' + e.message
      }
    }

    // 返回主菜单（带过渡动画）
    async function backToMenu() {
      transitioning.value = true
      await new Promise(r => setTimeout(r, TRANSITION_MASK_PAUSE))

      try {
        window.dispatchEvent(new CustomEvent('game:pause'))
      } catch (e) {}

      screen.value = 'menu'
      gameStatus.value = null
      message.value = ''
      roomItems.value = []

      await new Promise(r => setTimeout(r, TRANSITION_MENU_RENDER))
      transitioning.value = false
    }

    function onUpdate(payload) {
      applyGameResponse(payload)
    }

    const resetGame = async () => {
      const res = await fetch('/api/reset', { method: 'POST' })
      const j = await res.json()
      applyGameResponse(j)
      notifyGameCanvas(j)
    }

    return {
      screen,
      gameStatus,
      message,
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

/* ============ 游戏视图 ============ */
.game-view {
  display: flex;
  flex-direction: column;
  align-items: center;
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
  transition: opacity 0.8s ease 0.2s;
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
