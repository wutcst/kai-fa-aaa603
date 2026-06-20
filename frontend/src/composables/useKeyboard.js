/**
 * 键盘事件统一管理
 */

export function createKeyboardManager(options) {
  const {
    isInputFocused = () => false,
    toggleBackpack,
    toggleControlPanel,
    getBackpackVisible = () => false,
    getControlPanelVisible = () => false
  } = options

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
        toggleControlPanel()
      }
    }

    if (getBackpackVisible()) {
      const gameKeys = [
        'w', 'W', 'a', 'A', 's', 'S', 'd', 'D',
        'j', 'J', ' ', 'h', 'H',
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
