/**
 * 统一 API 请求封装
 * 消除 GameCanvas.vue 中重复的 fetch/emit/error/overlayCheck 模式
 */

function isOverlayOpen(scene) {
  if (!scene) return false
  return !!(scene.shopMenuOverlay || scene.shopBuyOverlay || scene.shopSellOverlay || scene.wisdomOverlay)
}

export function createApi(emit, getScene) {

  async function _fetch(url, options = {}) {
    try {
      const res = await fetch(url, options)
      const j = await res.json()
      emit('update', j)
      return j
    } catch (e) {
      emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
      return null
    }
  }

  async function sendCommand(command, fromDir = null) {
    const scene = getScene()
    if (scene) scene._lastMoveDir = fromDir
    const j = await _fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command })
    })
    if (j) {
      const sceneNow = getScene()
      if (j.data && !isOverlayOpen(sceneNow)) {
        try { sceneNow?.renderRoom(j.data) } catch (e) {}
      }
      if (j.status === 'error' && j.message?.includes('Game is over')) {
        try { sceneNow?.showGameOver() } catch (e) {}
      }
    }
    return j
  }

  async function sendAttack(body) {
    const j = await _fetch('/api/attack', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    if (j) {
      const scene = getScene()
      if (j.data && !isOverlayOpen(scene)) {
        try { scene?.renderRoom(j.data) } catch (e) {}
      }
      if (j.message?.includes('游戏结束')) {
        try { scene?.showGameOver() } catch (e) {}
      }
    }
    return j
  }

  async function pollGame() {
    try {
      const res = await fetch('/api/game')
      const j = await res.json()
      if (!j?.data) return
      const scene = getScene()
      if (!scene) return
      const hp = j.data.playerHp ?? scene.playerStats?.hp
      const maxHp = j.data.playerMaxHp ?? scene.playerStats?.maxHp
      const mp = j.data.playerMp ?? scene.playerStats?.mp
      const maxMp = j.data.playerMaxMp ?? scene.playerStats?.maxMp
      const money = j.data.playerMoney ?? scene.playerStats?.money
      try { scene.updatePlayerBars?.(hp, maxHp, mp, maxMp, money) } catch (e) {}
      if (j.data.activeEffects) {
        try { scene.updateBuffDisplay?.(j.data.activeEffects) } catch (e) {}
      }
      if (j.data.name && !isOverlayOpen(scene)) {
        try { scene.renderRoom(j.data) } catch (e) {}
      }
      if (j.data.gameOver) {
        try { scene.showGameOver() } catch (e) {}
      }
      if (j.data.name) {
        try { window.dispatchEvent(new CustomEvent('minimap:update', { detail: { roomName: j.data.name } })) } catch (e) {}
      }
    } catch (e) { /* silent */ }
  }

  async function resetGame() {
    const j = await _fetch('/api/reset', { method: 'POST' })
    if (j) {
      const scene = getScene()
      scene.gameOver = false
      try { scene.gameOverOverlay?.destroy() } catch (e) {}
      scene.gameOverOverlay = null
      try { window.dispatchEvent(new CustomEvent('game:reset')) } catch (e) {}
      try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
      if (j.data && !isOverlayOpen(scene)) {
        try { scene.renderRoom(j.data) } catch (e) {}
      }
    }
    return j
  }

  async function fetchInitGame() {
    const j = await _fetch('/api/game')
    if (j?.data) {
      const scene = getScene()
      if (!isOverlayOpen(scene)) {
        try { scene?.renderRoom(j.data) } catch (e) {}
      }
    }
    return j
  }

  async function fetchBackpack() {
    try {
      const res = await fetch('/api/backpack')
      const j = await res.json()
      return j?.data?.backpack || []
    } catch (e) {
      console.warn('[Backpack] 无法获取背包数据', e)
      return []
    }
  }

  async function fetchMap() {
    try {
      const res = await fetch('/api/map')
      return await res.json()
    } catch (e) {
      console.warn('无法获取地图数据，小地图不可用', e)
      return null
    }
  }

  async function fetchRaw(url, options = {}) {
    return _fetch(url, options)
  }

  return {
    sendCommand, sendAttack, pollGame, resetGame,
    fetchInitGame, fetchBackpack, fetchMap, fetchRaw,
    isOverlayOpen: () => isOverlayOpen(getScene())
  }
}
