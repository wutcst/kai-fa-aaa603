<template>
  <div id="app-root">
    <!-- 启动界面 -->
    <Transition name="menu-leave">
      <MainMenu v-if="screen === 'menu'" @start-game="startNewGame" @load-game="loadGame" />
    </Transition>

    <!-- 游戏界面 -->
    <Transition name="game-enter">
      <div v-if="screen === 'game'" class="game-view">
        <GameCanvas
          @reset-game="resetGame"
          @back-to-menu="backToMenu"
          @show-save-slots="showSaveSlots"
        />
      </div>
    </Transition>

    <!-- ========== 存档槽位选择弹窗 ========== -->
    <Transition name="fade">
      <div v-if="slotDialog.visible" class="slot-overlay" @click.self="closeSlotDialog">
        <div class="slot-panel">
          <h2 class="slot-title">{{ slotDialog.title }}</h2>
          <div class="slot-list">
            <div
              v-for="slot of SAVE_SLOTS"
              :key="'slot-' + slot"
              class="slot-card"
              :class="{
                'slot-has-data': slotDialog.saves[slot],
                'slot-empty': !slotDialog.saves[slot],
              }"
              @click="slotDialog.callback(slot)"
            >
              <template v-if="slotDialog.saves[slot]">
                <div class="slot-row">
                  <div class="slot-id">存档 {{ slot }}</div>
                  <button class="slot-delete-btn" @click.stop="deleteSlot(slot)">🗑 删除</button>
                </div>
                <div class="slot-info">
                  <span class="slot-player">{{ slotDialog.saves[slot].playerName }}</span>
                  <span class="slot-hp"
                    >❤ {{ slotDialog.saves[slot].hp }}/{{ slotDialog.saves[slot].maxHp }}</span
                  >
                </div>
                <div class="slot-info2">
                  <span class="slot-room">📍 {{ slotDialog.saves[slot].currentRoom }}</span>
                  <span class="slot-money">${{ slotDialog.saves[slot].money }}</span>
                </div>
                <div class="slot-time">{{ slotDialog.saves[slot].updatedAt }}</div>
              </template>
              <template v-else>
                <div class="slot-id">存档 {{ slot }}</div>
                <div class="slot-empty-text">— 空 —</div>
              </template>
            </div>
          </div>
          <button class="slot-cancel-btn" @click="closeSlotDialog">取消</button>
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
  import { ref } from 'vue'
  import GameCanvas from './components/GameCanvas.vue'
  import MainMenu from './components/MainMenu.vue'
  import { useSlotDialog } from './composables/useSlotDialog.js'
  import { api } from './api/gameApi.js'

  // 过渡动画时长常量（毫秒）
  const TRANSITION_MASK_PAUSE = 400
  const TRANSITION_PHASER_MOUNT = 400
  const TRANSITION_MENU_ENTER = 200
  const TRANSITION_LOAD_PREPARE = 600
  const TRANSITION_LOAD_RENDER = 200

  // 存档槽位编号
  const SAVE_SLOTS = [1, 2, 3]

  export default {
    components: { GameCanvas, MainMenu },
    setup() {
      const screen = ref('menu')
      const transitioning = ref(false)

      const { slotDialog, deleteSlot, showSaveSlots, showLoadSlots, closeSlotDialog } =
        useSlotDialog()

      // 通知 GameCanvas 组件更新游戏状态
      function notifyGameCanvas(j) {
        if (!j) return
        try {
          window.dispatchEvent(
            new CustomEvent('game:update', {
              detail: { status: j.status ?? 'success', message: j.message ?? '', data: j },
            }),
          )
        } catch (e) {
          console.error('通知游戏画布失败:', e)
        }
      }

      // 加载后端数据
      async function loadGameData() {
        try {
          const j = await api.getGameState()
          notifyGameCanvas(j)
        } catch (e) {
          console.error('加载游戏数据失败:', e)
        }
      }

      // 统一的游戏过渡动画
      async function transitionToGame(preloadFn) {
        transitioning.value = true
        if (preloadFn) await preloadFn()
        await new Promise((r) => setTimeout(r, TRANSITION_MASK_PAUSE))
        screen.value = 'game'
        await new Promise((r) => setTimeout(r, TRANSITION_PHASER_MOUNT))
        await loadGameData()
        transitioning.value = false
      }

      // 启动游戏（新游戏）
      async function startNewGame() {
        await transitionToGame(async () => {
          try {
            await api.reset()
          } catch (e) {
            console.error('重置游戏失败:', e)
          }
        })
      }

      // 继续冒险 — 弹出存档选择弹窗
      async function loadGame() {
        await showLoadSlots(async (slotNum, data) => {
          transitioning.value = true
          await new Promise((r) => setTimeout(r, TRANSITION_LOAD_PREPARE))
          screen.value = 'game'
          await new Promise((r) => setTimeout(r, TRANSITION_LOAD_RENDER))
          await new Promise((r) => requestAnimationFrame(r))
          notifyGameCanvas({
            status: 'success',
            message: '已加载 存档 ' + slotNum,
            items: data.items,
          })
          transitioning.value = false
        })
      }

      // 返回主菜单（带过渡动画）
      async function backToMenu() {
        transitioning.value = true
        // 等待黑幕完全覆盖
        await new Promise((r) => setTimeout(r, TRANSITION_MASK_PAUSE))

        try {
          window.dispatchEvent(new CustomEvent('game:pause'))
        } catch (e) {
          console.error('发送暂停事件失败:', e)
        }

        // 在黑幕下切换到菜单（MainMenu 开始 enter 动画）
        screen.value = 'menu'

        // 等待 MainMenu 挂载并开始渐入，然后让黑幕淡出形成交叉渐变
        await new Promise((r) => setTimeout(r, TRANSITION_MENU_ENTER))
        transitioning.value = false
      }

      // 重置游戏
      const resetGame = async () => {
        const res = await api.reset()
        const j = await res.json()
        // 先通知小地图清空旧数据（initMinimap 中 mapLayout = null），再渲染房间
        // 这样 renderRoom → minimap:update → drawMinimap 会因 !mapLayout 提前返回
        // initMinimap 异步完成后会用新地图数据绘制
        try {
          window.dispatchEvent(new CustomEvent('game:reset'))
        } catch (e) {
          console.error('发送重置事件失败:', e)
        }
        notifyGameCanvas(j)
      }

      return {
        screen,
        transitioning,
        slotDialog,
        SAVE_SLOTS,
        resetGame,
        startNewGame,
        loadGame,
        backToMenu,
        showSaveSlots,
        deleteSlot,
        closeSlotDialog,
      }
    },
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
  /* ============ CSS 过渡变量 ============ */
  :root {
    --transition-fast: 0.2s ease;
    --transition-normal: 0.4s ease;
    --transition-slow: 0.6s ease;
  }

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

  /* ============ 存档槽位弹窗 ============ */
  .slot-overlay {
    position: fixed;
    inset: 0;
    z-index: 10000;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(4px);
  }

  .slot-panel {
    width: 560px;
    background: linear-gradient(135deg, #1a1208 0%, #2a1c0e 100%);
    border: 1px solid rgba(200, 160, 80, 0.3);
    border-radius: 8px;
    padding: 28px 32px 24px;
    box-shadow: 0 8px 48px rgba(0, 0, 0, 0.6);
  }

  .slot-title {
    color: #e8d8b0;
    font-size: 20px;
    text-align: center;
    margin: 0 0 20px;
    letter-spacing: 2px;
  }

  .slot-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 18px;
  }

  .slot-card {
    padding: 14px 18px;
    border: 1px solid rgba(200, 160, 80, 0.2);
    border-radius: 6px;
    cursor: pointer;
    transition: all var(--transition-fast);
  }

  .slot-card:hover {
    border-color: rgba(200, 160, 80, 0.55);
    background: rgba(200, 160, 80, 0.08);
    transform: translateX(4px);
  }

  .slot-has-data {
    background: rgba(200, 160, 80, 0.04);
  }

  .slot-empty {
    opacity: 0.55;
  }

  .slot-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }

  .slot-id {
    font-size: 16px;
    font-weight: bold;
    color: #c89b4a;
  }

  .slot-delete-btn {
    background: rgba(200, 60, 40, 0.15);
    border: 1px solid rgba(200, 60, 40, 0.3);
    border-radius: 3px;
    color: #cc8877;
    font-size: 11px;
    padding: 3px 8px;
    cursor: pointer;
    transition: all var(--transition-fast);
  }

  .slot-delete-btn:hover {
    background: rgba(200, 60, 40, 0.35);
    color: #ff9999;
  }

  .slot-info,
  .slot-info2 {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
  }

  .slot-player {
    color: #e8d8b0;
    font-size: 14px;
  }

  .slot-hp {
    color: #ff6666;
    font-size: 13px;
  }

  .slot-room {
    color: #aaaacc;
    font-size: 12px;
  }

  .slot-money {
    color: #ffdd88;
    font-size: 13px;
  }

  .slot-time {
    color: #888;
    font-size: 11px;
    margin-top: 4px;
  }

  .slot-empty-text {
    color: #666;
    font-size: 14px;
    font-style: italic;
  }

  .slot-cancel-btn {
    display: block;
    width: 100%;
    padding: 10px;
    background: rgba(200, 80, 60, 0.15);
    border: 1px solid rgba(200, 80, 60, 0.3);
    border-radius: 4px;
    color: #cc8877;
    font-size: 15px;
    cursor: pointer;
    transition: all var(--transition-fast);
  }

  .slot-cancel-btn:hover {
    background: rgba(200, 80, 60, 0.25);
    color: #ee6666;
  }

  /* ============ 淡入淡出动画 ============ */
  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.25s ease;
  }

  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }

  /* ============ 菜单离开动画 ============ */
  .menu-leave-leave-active {
    transition:
      opacity var(--transition-slow),
      transform var(--transition-slow);
  }

  .menu-leave-leave-from {
    opacity: 1;
    transform: scale(1);
  }

  .menu-leave-leave-to {
    opacity: 0;
    transform: scale(1.05);
  }

  /* ============ 菜单进入动画（新增 — 从游戏返回时菜单淡入） ============ */
  .menu-leave-enter-active {
    transition: opacity 0.45s ease 0.1s;
  }

  .menu-leave-enter-from {
    opacity: 0;
    transform: scale(0.97);
  }

  .menu-leave-enter-to {
    opacity: 1;
    transform: scale(1);
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

  /* ============ 游戏离开动画（黑幕覆盖下快速淡出） ============ */
  .game-enter-leave-active {
    transition: opacity 0.25s ease;
  }

  .game-enter-leave-from {
    opacity: 1;
  }

  .game-enter-leave-to {
    opacity: 0;
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
    transition: opacity 0.35s ease;
  }

  .transition-flash-leave-active {
    transition: opacity 0.55s ease 0.1s;
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
