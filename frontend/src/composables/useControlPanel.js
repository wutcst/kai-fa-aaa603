/**
 * 控制面板全自治 Composable（单例模式）
 *
 * 管理控制面板的全部状态与事件分发。
 * 所有组件通过 `useControlPanel()` 获取同一个实例。
 *
 * 用法：
 *   import { useControlPanel } from '../composables/useControlPanel.js'
 *   const cp = useControlPanel()
 *   // cp.visible, cp.open(), cp.close(), cp.toggle()
 */

import { ref, reactive } from 'vue'

// ==================== 模块级单例状态 ====================
const visible = ref(false)

// ==================== 公开方法 ====================

/** 打开控制面板，同时通知游戏场景暂停 */
function open() {
  visible.value = true
  try {
    window.dispatchEvent(new CustomEvent('game:pause-scene'))
  } catch (e) {
    /* ignore */
  }
}

/** 关闭控制面板，同时通知游戏场景恢复 */
function close() {
  visible.value = false
  try {
    window.dispatchEvent(new CustomEvent('game:resume-scene'))
  } catch (e) {
    /* ignore */
  }
}

/** 切换控制面板显示/隐藏 */
function toggle() {
  if (visible.value) {
    close()
  } else {
    open()
  }
}

// ==================== 单例导出 ====================

let _instance = null

export function useControlPanel() {
  if (_instance) return _instance

  _instance = reactive({
    visible,
    open,
    close,
    toggle,
  })

  return _instance
}
