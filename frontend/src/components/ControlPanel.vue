<template>
  <!-- ==================== 控制面板覆盖层（ESC 打开/关闭） ==================== -->
  <div v-if="cp.visible" class="control-overlay" @click.self="cp.close">
    <div class="control-panel">
      <button class="control-close-btn" title="关闭 (ESC)" @click="cp.close">
        <svg
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
      <h2 class="control-title">⚙ 游戏控制</h2>
      <div class="control-body">
        <button class="control-btn control-btn-restart" @click="handleRestart">🔄 重新开始</button>
        <button class="control-btn control-btn-save" @click="handleSaveGame">💾 保存游戏</button>
        <button class="control-btn control-btn-menu" @click="handleBackToMenu">🚪 返回菜单</button>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { useControlPanel } from '../composables/useControlPanel.js'

  const emit = defineEmits(['restart', 'save', 'back-to-menu'])
  const cp = useControlPanel()

  function handleRestart() {
    cp.close()
    emit('restart')
  }

  function handleSaveGame() {
    cp.close()
    emit('save')
  }

  function handleBackToMenu() {
    cp.close()
    emit('back-to-menu')
  }
</script>

<style scoped>
  /* ==================== 控制面板样式 ==================== */

  .control-overlay {
    position: absolute;
    inset: 0;
    z-index: 2000;
    background: rgba(0, 0, 0, 0.65);
    display: flex;
    justify-content: center;
    align-items: center;
    pointer-events: auto;
  }

  .control-panel {
    position: relative;
    width: 300px;
    background: linear-gradient(180deg, #1a1a2e 0%, #0d0d1a 100%);
    border: 2px solid rgba(180, 150, 80, 0.5);
    border-radius: 16px;
    overflow: hidden;
    padding: 32px 28px 28px;
  }

  /* 右上角关闭按钮 */
  .control-close-btn {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 32px;
    height: 32px;
    border-radius: 6px;
    background: transparent;
    border: 1px solid rgba(180, 150, 80, 0.3);
    color: #998866;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    padding: 0;
    z-index: 2;
  }
  .control-close-btn:hover {
    background: rgba(200, 60, 60, 0.12);
    border-color: rgba(200, 60, 60, 0.4);
    color: #cc6666;
  }

  /* 标题 */
  .control-title {
    font-size: 20px;
    font-weight: bold;
    color: #e8d8b0;
    text-align: center;
    letter-spacing: 2px;
    margin: 0 0 20px;
    padding-top: 8px;
  }

  /* 按钮容器 */
  .control-body {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  /* 按钮通用 */
  .control-btn {
    padding: 12px 20px;
    font-size: 15px;
    font-weight: bold;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    text-align: center;
    letter-spacing: 1px;
    border: 1px solid;
  }

  /* 重新开始 */
  .control-btn-restart {
    background: #1e2a38;
    color: #c8d8e8;
    border-color: rgba(140, 170, 210, 0.25);
  }
  .control-btn-restart:hover {
    background: #2a3a4e;
    border-color: rgba(160, 190, 220, 0.4);
  }

  /* 保存游戏 */
  .control-btn-save {
    background: #1a2418;
    color: #aaccaa;
    border-color: rgba(100, 180, 100, 0.35);
    cursor: pointer;
  }
  .control-btn-save:hover {
    background: #2a3a28;
    color: #ccddcc;
    border-color: rgba(120, 200, 120, 0.5);
    box-shadow: 0 0 12px rgba(80, 160, 80, 0.3);
  }

  /* 返回菜单 */
  .control-btn-menu {
    background: #2a1a0e;
    color: #e0c898;
    border-color: rgba(200, 160, 80, 0.3);
  }
  .control-btn-menu:hover {
    background: #3a2510;
    border-color: rgba(220, 180, 100, 0.45);
  }
</style>
