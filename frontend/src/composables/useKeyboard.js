/**
 * 键盘事件统一管理
 *
 * 处理 B 键（背包）、ESC 键（控制面板）以及游戏键在背包打开时的屏蔽。
 * 控制面板状态由 useControlPanel() 单例管理，无需外部注入。
 */

import { useControlPanel } from './useControlPanel.js'

export function createKeyboardManager(options) {
  const {
    isInputFocused = () => false,
    toggleBackpack,
    getBackpackVisible = () => false,
  } = options

  const cp = useControlPanel()

  function onKeyDown(e) {
    if (isInputFocused()) return
    const key = e.key

    if (key === 'b' || key === 'B') {
      e.preventDefault()
      toggleBackpack()
    }

    if (key === 'Escape') {
      if (getBackpackVisible()) {
        toggleBackpack()
      } else {
        e.preventDefault()
        cp.toggle()
      }
    }

    // 背包或控制面板打开时屏蔽游戏按键
    if (getBackpackVisible() || cp.visible) {
      const gameKeys = [
        'w', 'W', 'a', 'A', 's', 'S', 'd', 'D',
        'j', 'J', ' ', 'h', 'H', 'f', 'F', 'g', 'G',
        'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'
      ]
      if (gameKeys.includes(key)) {
        e.stopPropagation()
        e.stopImmediatePropagation()
      }
    }
  }

  return {
    attach() { window.addEventListener('keydown', onKeyDown, true) },
    detach() { window.removeEventListener('keydown', onKeyDown, true) }
  }
}
