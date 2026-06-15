<template>
  <div style="position: relative; width: 800px; height: 600px;">
    <!-- Phaser 游戏主容器 -->
    <div ref="gameContainer" style="width:800px;height:600px;border:1px solid #ccc;background:#000"></div>

    <!-- 小地图 Canvas（固定在右下角） -->
    <canvas
        ref="minimapCanvas"
        class="minimap"
        width="160"
        height="160"
        style="
        position: absolute;
        bottom: 10px;
        right: 10px;
        border: 1px solid rgba(255,255,255,0.6);
        background: rgba(0,0,0,0.55);
        border-radius: 4px;
        pointer-events: none;
        z-index: 10;
      "
    ></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Phaser from 'phaser'

const emit = defineEmits(['update'])
const gameContainer = ref(null)
const minimapCanvas = ref(null)
let game = null

// ---------- 小地图状态 ----------
let mapLayout = null          // { rooms, roomMap, coords }
let currentRoomName = ''      // 当前玩家所在房间名
// --------------------------------

// Helper: 按键映射 (保留不变)
const keyToDir = (key) => {
  switch (key) {
    case 'ArrowUp':
    case 'w':
    case 'W':
      return 'north'
    case 'ArrowDown':
    case 's':
    case 'S':
      return 'south'
    case 'ArrowLeft':
    case 'a':
    case 'A':
      return 'west'
    case 'ArrowRight':
    case 'd':
    case 'D':
      return 'east'
    default:
      return null
  }
}

// ---------- 小地图核心函数 ----------

function buildMapLayout(mapData) {
  const { rooms, startRoomName } = mapData
  const roomMap = new Map(rooms.map(r => [r.name, r]))
  const coords = new Map()   // roomName -> {x, y}
  const visited = new Set()

  const queue = [startRoomName]
  coords.set(startRoomName, { x: 0, y: 0 })
  visited.add(startRoomName)

  const dirVec = {
    north: { dx: 0, dy: -1 },
    south: { dx: 0, dy: 1 },
    west:  { dx: -1, dy: 0 },
    east:  { dx: 1, dy: 0 }
  }

  while (queue.length > 0) {
    const curName = queue.shift()
    const curRoom = roomMap.get(curName)
    if (!curRoom) continue
    const { x, y } = coords.get(curName)
    const exits = curRoom.exits || {}
    for (const [dir, neighborName] of Object.entries(exits)) {
      if (!neighborName || visited.has(neighborName)) continue
      const vec = dirVec[dir]
      if (!vec) continue
      const nx = x + vec.dx
      const ny = y + vec.dy
      coords.set(neighborName, { x: nx, y: ny })
      visited.add(neighborName)
      queue.push(neighborName)
    }
  }
  return { rooms, roomMap, coords }
}

function drawMinimap(highlightRoomName) {
  const canvas = minimapCanvas.value
  if (!canvas || !mapLayout) return

  const ctx = canvas.getContext('2d')
  const { rooms, coords } = mapLayout
  if (coords.size === 0) return

  // 计算边界
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  for (const [, pos] of coords) {
    if (pos.x < minX) minX = pos.x
    if (pos.x > maxX) maxX = pos.x
    if (pos.y < minY) minY = pos.y
    if (pos.y > maxY) maxY = pos.y
  }

  const margin = 14
  const availW = canvas.width - margin * 2
  const availH = canvas.height - margin * 2
  const rangeX = maxX - minX + 1
  const rangeY = maxY - minY + 1
  const cellSize = Math.min(availW / rangeX, availH / rangeY, 36)
  const rectW = cellSize * 0.75
  const rectH = cellSize * 0.75
  const offsetX = margin + (availW - rangeX * cellSize) / 2
  const offsetY = margin + (availH - rangeY * cellSize) / 2

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 获取房间矩形坐标
  const getRoomRect = (name) => {
    const pos = coords.get(name)
    if (!pos) return null
    const x = offsetX + (pos.x - minX) * cellSize + (cellSize - rectW) / 2
    const y = offsetY + (pos.y - minY) * cellSize + (cellSize - rectH) / 2
    return { x, y, w: rectW, h: rectH }
  }

  // 1. 绘制连线（去重）
  const drawnEdges = new Set()
  for (const room of rooms) {
    const rectA = getRoomRect(room.name)
    if (!rectA) continue
    const exits = room.exits || {}
    for (const [dir, neighborName] of Object.entries(exits)) {
      if (!neighborName) continue
      const edgeId = room.name < neighborName ? `${room.name}->${neighborName}` : `${neighborName}->${room.name}`
      if (drawnEdges.has(edgeId)) continue
      drawnEdges.add(edgeId)

      const rectB = getRoomRect(neighborName)
      if (!rectB) continue

      let x1, y1, x2, y2
      switch (dir) {
        case 'north':
          x1 = rectA.x + rectA.w / 2; y1 = rectA.y
          x2 = rectB.x + rectB.w / 2; y2 = rectB.y + rectB.h
          break
        case 'south':
          x1 = rectA.x + rectA.w / 2; y1 = rectA.y + rectA.h
          x2 = rectB.x + rectB.w / 2; y2 = rectB.y
          break
        case 'west':
          x1 = rectA.x; y1 = rectA.y + rectA.h / 2
          x2 = rectB.x + rectB.w; y2 = rectB.y + rectB.h / 2
          break
        case 'east':
          x1 = rectA.x + rectA.w; y1 = rectA.y + rectA.h / 2
          x2 = rectB.x; y2 = rectB.y + rectB.h / 2
          break
        default: continue
      }
      ctx.beginPath()
      ctx.moveTo(x1, y1)
      ctx.lineTo(x2, y2)
      ctx.strokeStyle = 'rgba(200,200,200,0.7)'
      ctx.lineWidth = 1
      ctx.stroke()
    }
  }

  // 2. 绘制房间矩形（按房间类型不同颜色）
  const roomTypeColors = {
    START_HALL:      { fill: 'rgba(255, 215, 0, 0.85)', stroke: '#FFD700' },
    SHOP:            { fill: 'rgba(0, 191, 255, 0.75)', stroke: '#00BFFF' },
    ENCOUNTER:       { fill: 'rgba(186, 85, 211, 0.75)', stroke: '#BA55D3' },
    CAMPFIRE:        { fill: 'rgba(255, 140, 0, 0.75)', stroke: '#FF8C00' },
    BOSS:            { fill: 'rgba(220, 20, 60, 0.80)', stroke: '#DC143C' },
    ELITE_MONSTER:   { fill: 'rgba(255, 99, 71, 0.75)', stroke: '#FF6347' },
    NORMAL_MONSTER:  { fill: 'rgba(100, 149, 237, 0.7)', stroke: 'rgba(255,255,255,0.8)' }
  }
  for (const room of rooms) {
    const rect = getRoomRect(room.name)
    if (!rect) continue
    const isCurrent = room.name === highlightRoomName
    const roomType = room.roomType || 'NORMAL_MONSTER'
    const typeColor = roomTypeColors[roomType] || roomTypeColors.NORMAL_MONSTER
    if (isCurrent) {
      ctx.fillStyle = 'rgba(255, 215, 0, 0.9)'
      ctx.strokeStyle = '#FFD700'
      ctx.lineWidth = 2
    } else {
      ctx.fillStyle = typeColor.fill
      ctx.strokeStyle = typeColor.stroke
      ctx.lineWidth = 1
    }
    ctx.fillRect(rect.x, rect.y, rect.w, rect.h)
    ctx.strokeRect(rect.x, rect.y, rect.w, rect.h)
    // 房间编号（提取完整数字后缀，支持多位数）
    ctx.fillStyle = '#fff'
    const label = (room.name.match(/\d+$/) || [room.name.charAt(room.name.length - 1) || '?'])[0]
    const fontSize = Math.max(7, rect.w * (label.length > 1 ? 0.28 : 0.4))
    ctx.font = `${fontSize}px Arial`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(label, rect.x + rect.w / 2, rect.y + rect.h / 2)
  }
}

async function initMinimap() {
  try {
    const res = await fetch('/api/map')
    const data = await res.json()
    console.log('[Minimap] API response rooms:', data.rooms ? data.rooms.map(r => ({ name: r.name, roomType: r.roomType })) : 'none')
    mapLayout = buildMapLayout(data)
    drawMinimap(currentRoomName)
  } catch (e) {
    console.warn('无法获取地图数据，小地图不可用', e)
  }
}

function onMinimapUpdate(e) {
  drawMinimap(e.detail.roomName)
}

// ---------- Phaser 游戏场景 ----------

onMounted(() => {
  const config = {
    type: Phaser.AUTO,
    width: 800,
    height: 600,
    parent: gameContainer.value,
    backgroundColor: '#333333',
    scene: {
      preload: function () {
        try {
          this.load.image('grass_tile', new URL('../assets/grass_tile.svg', import.meta.url).href)
        } catch (e) {}
      },
      create: async function () {
        const scene = this

        // game over state
        scene.gameOver = false
        scene.gameOverOverlay = null

        // container groups
        // 容器和图形
        scene.roomGraphics = scene.add.container(0, 0)
        scene.itemsGroup = scene.add.group()
        // group for monsters (ensure it exists before renderRoom may add monsters)
        scene.monstersGroup = scene.add.group()
        // arrays / lists used by renderRoom (ensure they exist before renderRoom may be called)
        scene.exitButtons = []
        scene.doorRects = []
        scene.monstersData = []
        scene.itemsData = []
        scene.roomBoundsGraphic = scene.add.graphics()
        scene.bgColor = 0x6bbf3a
        scene.parallax = { farFactor: 0.35, nearFactor: 0.72 }

        // 随机数工具
        const mulberry32 = (a) => {
          return function() {
            a |= 0
            a = a + 0x6D2B79F5 | 0
            let t = Math.imul(a ^ a >>> 15, 1 | a)
            t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t
            return ((t ^ t >>> 14) >>> 0) / 4294967296
          }
        }

        // 草地纹理生成
        const createSeamlessGrass = (key, size = 128, seed = 1, density = 180) => {
          const rng = mulberry32(seed)
          const canvas = document.createElement('canvas')
          canvas.width = size
          canvas.height = size
          const ctx = canvas.getContext('2d')
          const g = ctx.createLinearGradient(0, 0, 0, size)
          g.addColorStop(0, '#7fd34a')
          g.addColorStop(1, '#5fb033')
          ctx.fillStyle = g
          ctx.fillRect(0, 0, size, size)
          ctx.lineCap = 'round'
          for (let i = 0; i < density; i++) {
            const bx = rng() * size
            const by = size
            const bladeH = 12 + rng() * (size * 0.6)
            const ctrlX = bx + (rng() * 12 - 6)
            const ctrlY = by - bladeH / 2
            const tipX = bx + (rng() * 18 - 9)
            const tipY = Math.max(0, by - bladeH)
            const hueShift = (rng() * 20 - 10)
            ctx.strokeStyle = `hsl(${100 + hueShift}, 40%, ${30 + rng() * 20}%)`
            ctx.lineWidth = 1 + rng() * 1.8
            ctx.beginPath()
            ctx.moveTo(bx, by)
            ctx.quadraticCurveTo(ctrlX, ctrlY, tipX, tipY)
            ctx.stroke()
            const parts = [ {ox: -size, oy:0}, {ox: size, oy:0}, {ox:0, oy:-size}, {ox:0, oy:size}, {ox:-size, oy:-size}, {ox:size, oy:-size}, {ox:-size, oy:size}, {ox:size, oy:size} ]
            for (const p of parts) {
              ctx.beginPath()
              ctx.moveTo(bx + p.ox, by + p.oy)
              ctx.quadraticCurveTo(ctrlX + p.ox, ctrlY + p.oy, tipX + p.ox, tipY + p.oy)
              ctx.stroke()
            }
          }
          for (let i = 0; i < Math.round(size * size / 160); i++) {
            ctx.fillStyle = `rgba(255,255,255,${0.02 + rng() * 0.06})`
            const x = Math.floor(rng() * size)
            const y = Math.floor(rng() * size)
            ctx.fillRect(x, y, 1, 1)
          }
          ctx.globalCompositeOperation = 'multiply'
          ctx.fillStyle = 'rgba(20,40,20,0.06)'
          ctx.fillRect(0, 0, size, size)
          ctx.globalCompositeOperation = 'source-over'
          try {
            if (scene.textures.exists(key)) scene.textures.remove(key)
            scene.textures.addCanvas(key, canvas)
          } catch (e) {
            try { scene.textures.createCanvas(key, size, size).context.drawImage(canvas,0,0); scene.textures.get(key).refresh() } catch (ee) {}
          }
          return key
        }

        const tileSize = 128
        const v0 = createSeamlessGrass('grass_v0', tileSize, 12345, 260)
        const v1 = createSeamlessGrass('grass_v1', tileSize, 54321, 200)

        const bgFar = scene.add.tileSprite(0, 0, 800, 600, v0).setOrigin(0, 0)
        const bgNear = scene.add.tileSprite(0, 0, 800, 600, v1).setOrigin(0, 0)
        bgNear.setAlpha(0.88).setScale(1.02)
        scene.roomGraphics.addAt(bgFar, 0)
        scene.roomGraphics.addAt(bgNear, 1)
        scene.bgFar = bgFar
        scene.bgNear = bgNear
        scene._prevPlayerX = 400
        scene._prevPlayerY = 320

        // ---------- HP / MP 状态条（右上角） ----------
        scene.playerStats = { hp: 100, maxHp: 100, mp: 100, maxMp: 100 }

        const barX = 570, barW = 210, barH = 18
        const hpY = 15, mpY = 39
        const barDepth = 100

        // HP label
        scene.hpLabel = scene.add.text(barX - 30, hpY + 1, 'HP', { font: 'bold 14px Arial', fill: '#ff6666' }).setDepth(barDepth)
        // HP background
        scene.hpBarBg = scene.add.rectangle(barX + barW / 2, hpY + barH / 2, barW, barH, 0x333333).setDepth(barDepth).setStrokeStyle(1, 0x666666)
        scene.hpBarBg.setOrigin(0.5, 0.5)
        // HP fill
        scene.hpBarFill = scene.add.rectangle(barX, hpY, barW, barH, 0xcc2222).setDepth(barDepth + 1).setOrigin(0, 0)
        // HP text
        scene.hpText = scene.add.text(barX + barW / 2, hpY + barH / 2, '100/100', {
          font: 'bold 12px Arial', fill: '#ffffff'
        }).setDepth(barDepth + 2).setOrigin(0.5, 0.5)

        // MP label
        scene.mpLabel = scene.add.text(barX - 30, mpY + 1, 'MP', { font: 'bold 14px Arial', fill: '#6699ff' }).setDepth(barDepth)
        // MP background
        scene.mpBarBg = scene.add.rectangle(barX + barW / 2, mpY + barH / 2, barW, barH, 0x333333).setDepth(barDepth).setStrokeStyle(1, 0x666666)
        scene.mpBarBg.setOrigin(0.5, 0.5)
        // MP fill
        scene.mpBarFill = scene.add.rectangle(barX, mpY, barW, barH, 0x2244cc).setDepth(barDepth + 1).setOrigin(0, 0)
        // MP text
        scene.mpText = scene.add.text(barX + barW / 2, mpY + barH / 2, '100/100', {
          font: 'bold 12px Arial', fill: '#ffffff'
        }).setDepth(barDepth + 2).setOrigin(0.5, 0.5)

        /**
         * 更新 HP/MP 状态条，带平滑动画
         */
        scene.updatePlayerBars = function (hp, maxHp, mp, maxMp) {
          const prev = scene.playerStats
          const barW = 210

          // HP 动画
          const hpRatio = Math.max(0, Math.min(1, hp / Math.max(1, maxHp)))
          const targetHpW = barW * hpRatio
          scene.tweens.add({
            targets: scene.hpBarFill,
            displayWidth: targetHpW,
            duration: 300,
            ease: 'Cubic.easeOut'
          })
          // HP 颜色随血量变化
          let hpColor = 0xcc2222
          if (hpRatio > 0.5) hpColor = 0x22aa22
          else if (hpRatio > 0.25) hpColor = 0xccaa22
          scene.tweens.add({
            targets: {},
            duration: 300,
            onUpdate: () => {
              scene.hpBarFill.setFillStyle(hpColor)
            }
          })
          scene.hpText.setText(hp + '/' + maxHp)

          // MP 动画
          const mpRatio = Math.max(0, Math.min(1, mp / Math.max(1, maxMp)))
          const targetMpW = barW * mpRatio
          scene.tweens.add({
            targets: scene.mpBarFill,
            displayWidth: targetMpW,
            duration: 300,
            ease: 'Cubic.easeOut'
          })
          scene.mpText.setText(mp + '/' + maxMp)

          scene.playerStats = { hp, maxHp, mp, maxMp }
        }

        scene.titleText = scene.add.text(20, 20, '', { font: '20px Arial', fill: '#ffffff' })
        scene.descText = scene.add.text(20, 50, '', { font: '14px Arial', fill: '#cccccc', wordWrap: { width: 760 } })

        scene.playerRadius = 10
        scene.player = scene.add.circle(400, 320, scene.playerRadius, 0x00aaff).setStrokeStyle(2, 0x000000)
        scene.playerLabel = scene.add.text(400 - 20, 320 + 18, 'You', { font: '12px Arial', fill: '#fff' })
        scene._roomBounds = { left: 16, top: 16, right: 800 - 16, bottom: 600 - 16 }

        scene.add.text(20, 560, '使用方向键 / 点击出口 / 点击物品 与后端交互', { font: '14px Arial', fill: '#cccccc' })

        scene.sendCommand = async function (cmd, fromDir = null) {
          scene._lastMoveDir = fromDir
          try {
            const res = await fetch('/api/command', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ command: cmd })
            })
            const j = await res.json()
            emit('update', j)
            // if response contains room data, re-render
            if (j && j.data) {
              scene.renderRoom(j.data)
            }
            // check for game over from backend response
            if (j && j.status === 'error' && j.message && j.message.includes('Game is over')) {
              scene.showGameOver()
            }
          } catch (e) {
            emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
          }
        }

        // show game over overlay
        scene.showGameOver = function () {
          scene.gameOver = true
          // remove existing overlay if any
          if (scene.gameOverOverlay) {
            try { scene.gameOverOverlay.destroy() } catch (e) {}
          }
          const overlay = scene.add.container(0, 0)
          const bg = scene.add.rectangle(400, 300, 800, 600, 0x000000, 0.7)
          bg.setInteractive() // block clicks behind
          const title = scene.add.text(400, 240, '游戏结束', { font: 'bold 48px Arial', fill: '#ff4444' }).setOrigin(0.5)
          const hint = scene.add.text(400, 310, '你被击败了！点击下方按钮重新开始', { font: '20px Arial', fill: '#ffffff' }).setOrigin(0.5)
          const resetBtn = scene.add.text(400, 380, '[ 重新开始 ]', { font: 'bold 28px Arial', fill: '#ffcc00', backgroundColor: '#333', padding: { x: 20, y: 10 } }).setOrigin(0.5).setInteractive({ useHandCursor: true })
          resetBtn.on('pointerdown', async () => {
            try {
              const res = await fetch('/api/reset', { method: 'POST' })
              const j = await res.json()
              scene.gameOver = false
              if (scene.gameOverOverlay) { try { scene.gameOverOverlay.destroy() } catch (e) {}; scene.gameOverOverlay = null }
              try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
              if (j && j.data) scene.renderRoom(j.data)
            } catch (e) { /* ignore */ }
          })
          overlay.add([bg, title, hint, resetBtn])
          overlay.setDepth(9999)
          scene.gameOverOverlay = overlay
        }

        // render room view given backend room info
        // renderRoom 核心函数
        scene.renderRoom = function (roomInfo) {
          // 更新 HP/MP 状态条
          const hp = roomInfo.playerHp !== undefined ? roomInfo.playerHp : scene.playerStats.hp
          const maxHp = roomInfo.playerMaxHp !== undefined ? roomInfo.playerMaxHp : scene.playerStats.maxHp
          const mp = roomInfo.playerMp !== undefined ? roomInfo.playerMp : scene.playerStats.mp
          const maxMp = roomInfo.playerMaxMp !== undefined ? roomInfo.playerMaxMp : scene.playerStats.maxMp
          try { scene.updatePlayerBars(hp, maxHp, mp, maxMp) } catch (e) {}

          scene.itemsGroup.clear(true, true)
          scene.itemsData = []
          // save current AI-driven monster positions before clearing
          const prevMonsterPositions = {}
          for (const mon of scene.monstersData) {
            if (mon && mon.name) {
              prevMonsterPositions[mon.name] = { x: mon.x, y: mon.y }
            }
          }
          // clear previous monster sprites and state (fix: monsters were not being cleared)
          scene.monstersGroup.clear(true, true)
          scene.monstersData = []
          // remove exit buttons
          scene.exitButtons.forEach(b => b.destroy && b.destroy())
          scene.exitButtons = []

          scene.titleText.setText(roomInfo.name || '未知房间')
          scene.descText.setText(roomInfo.description || '')

          const exitsStr = roomInfo.exits || ''
          const exits = exitsStr.split(/\s+/).filter(s => s)

          const roomW = roomInfo.width || 650
          const roomH = roomInfo.height || 450
          const rectLeft = Math.round(400 - roomW / 2)
          const rectTop = Math.round(320 - roomH / 2)
          const rectCenterX = rectLeft + roomW / 2
          const rectCenterY = rectTop + roomH / 2

          scene.roomBoundsGraphic.clear()
          scene.roomBoundsGraphic.fillStyle(0x000000, 0.06)
          scene.roomBoundsGraphic.fillRect(rectLeft, rectTop, roomW, roomH)
          scene.roomBoundsGraphic.lineStyle(2, 0xffffff, 0.9)
          scene.roomBoundsGraphic.strokeRect(rectLeft, rectTop, roomW, roomH)

          if (!scene.doorRects) scene.doorRects = []
          scene.doorRects.forEach(d => { try { d.rect.destroy(); d.label.destroy() } catch (e) {} })
          scene.doorRects = []

          exits.forEach(dir => {
            let w = 120, h = 40
            if (dir === 'west' || dir === 'east') { w = 40; h = 120 }
            let posx = rectCenterX, posy = rectCenterY
            const outsideOffset = -10
            if (dir === 'north') { posx = rectCenterX; posy = rectTop - h / 2 - outsideOffset }
            if (dir === 'south') { posx = rectCenterX; posy = rectTop + roomH + h / 2 + outsideOffset }
            if (dir === 'west')  { posx = rectLeft - w / 2 - outsideOffset; posy = rectCenterY }
            if (dir === 'east')  { posx = rectLeft + roomW + w / 2 + outsideOffset; posy = rectCenterY }

            const rect = scene.add.rectangle(posx, posy, w, h, 0x664422).setStrokeStyle(2, 0x222222)
            const label = scene.add.text(posx - 20, posy - 10, dir.toUpperCase(), { font: '14px Arial', fill: '#ffffff' })
            rect.setInteractive({ useHandCursor: true })
            rect.on('pointerdown', () => {
              scene.tweens.add({ targets: scene.player, x: posx, y: posy, duration: 80 })
              scene.sendCommand('go ' + dir, dir)
            })
            scene.exitButtons.push(rect)
            scene.exitButtons.push(label)
            scene.doorRects.push({ dir, rect, label })
          })

          scene._roomBounds = { left: rectLeft, top: rectTop, right: rectLeft + roomW, bottom: rectTop + roomH }

          const opposite = { north: 'south', south: 'north', east: 'west', west: 'east' }
          if (scene._lastMoveDir) {
            const from = scene._lastMoveDir
            const to = opposite[from]
            if (to && exits.includes(to)) {
              let tw = 120, th = 40
              if (to === 'west' || to === 'east') { tw = 40; th = 120 }
              let targetX = rectCenterX, targetY = rectCenterY
              if (to === 'north') { targetX = rectCenterX; targetY = rectTop + th / 2 + 8 }
              if (to === 'south') { targetX = rectCenterX; targetY = rectTop + roomH - th / 2 - 8 }
              if (to === 'west')  { targetX = rectLeft + tw / 2 + 8; targetY = rectCenterY }
              if (to === 'east')  { targetX = rectLeft + roomW - tw / 2 - 8; targetY = rectCenterY }
              const pr = scene.playerRadius || 10
              targetX = Phaser.Math.Clamp(targetX, rectLeft + pr, rectLeft + roomW - pr)
              targetY = Phaser.Math.Clamp(targetY, rectTop + pr, rectTop + roomH - pr)
              try { scene.player.setPosition(targetX, targetY) } catch (e) { scene.player.x = targetX; scene.player.y = targetY }
              try { scene.playerLabel.setPosition(scene.player.x - 20, scene.player.y + 18) } catch (e) {}
            }
            scene._lastMoveDir = null
          }

          const items = roomInfo.items || []
          const startX = 520
          let ix = 0
          items.forEach(item => {
            const x = startX + (ix % 2) * 60
            const y = 360 + Math.floor(ix / 2) * 60
            const circle = scene.add.circle(x, y, 20, 0x8b5a2b).setStrokeStyle(2, 0x000000)
            const label = scene.add.text(x - 24, y + 24, item.name, { font: '14px Arial', fill: '#fff' })
            circle.setInteractive({ useHandCursor: true })
            circle.on('pointerover', () => circle.setScale(1.05))
            circle.on('pointerout', () => circle.setScale(1))
            circle.on('pointerdown', async () => {
              try {
                const res = await fetch('/api/command', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ command: 'take ' + item.name })
                })
                const j = await res.json()
                emit('update', j)
                if (j && j.status === 'success') {
                  scene.tweens.add({ targets: [circle, label], y: '-=100', alpha: 0, scale: 0.5, duration: 200, onComplete: () => { circle.destroy(); label.destroy() } })
                }
                if (j && j.data) scene.renderRoom(j.data)
              } catch (e) {
                emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
              }
            })
            scene.itemsGroup.add(circle)
            scene.itemsGroup.add(label)
            scene.itemsData.push({ name: item.name, x, y, circle, label, prompted: false, _removed: false })
            ix++
          })

          const monsters = roomInfo.monsters || []
          // vertical stacking layout to prevent label overlap
          const mCenterX = 300
          const mStartY = 160
          const mSpacingY = 120
          let mi = 0
          monsters.forEach(mon => {
            // restore AI-driven position if available, otherwise use default layout
            let x = mCenterX
            let y = mStartY + mi * mSpacingY
            const saved = prevMonsterPositions[mon.name]
            if (saved) {
              x = saved.x
              y = saved.y
            }
            const circ = scene.add.circle(x, y, 20, 0xaa0000).setStrokeStyle(2, 0x000000)
            const label = scene.add.text(x - 32, y + 24, mon.name + ' (HP:' + (mon.hp || 0) + ')', { font: '14px Arial', fill: '#fff' })
            circ.setInteractive({ useHandCursor: true })
            circ.on('pointerover', () => circ.setScale(1.05))
            circ.on('pointerout', () => circ.setScale(1))
            circ.on('pointerdown', async () => {
              try {
                const res = await fetch('/api/command', {
                  method: 'POST', headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ command: 'attack ' + mon.name })
                })
                const j = await res.json()
                emit('update', j)
                if (j && j.data) scene.renderRoom(j.data)
              } catch (e) {
                emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
              }
            })
            if (!scene.monstersGroup) scene.monstersGroup = scene.add.group()
            if (!scene.monstersData) scene.monstersData = []
            scene.monstersGroup.add(circ)
            scene.monstersGroup.add(label)
            scene.monstersData.push({ name: mon.name, x, y, circ, label, hp: mon.hp })
            mi++
          })



          // ---------- 小地图更新通知 ----------
          try {
            currentRoomName = roomInfo.name || ''
            window.dispatchEvent(new CustomEvent('minimap:update', { detail: { roomName: currentRoomName } }))
          } catch (e) {}
        }

        // 键盘控制
        scene.keys = scene.input.keyboard.addKeys('W,A,S,D,E,SHIFT,J')
        scene.baseMoveSpeed = 160
        scene.facingAngle = 0
        scene.attackConfig = {
          radius: 110,
          angleDeg: 135,
          segments: 96,
          sweepDuration: 140,
          ghostFade: 120,
          finalFade: 60,
          ghostSpacing: 0.035,
          mainAlpha: 0.95,
          ghostAlpha: 0.92,
          ghostMinFade: 40,
          pierceDistance: 120,
          pierceDistanceExpand: 1.15,
          pierceDuration: 100,
          pierceFade: 180,
          pierceWidth: 14
        }
        // ---------- 强化攻击特效辅助函数 ----------
        // 弧形刀光（内圈加速消失，增强质感）
        scene.drawArcSlash = (gfx, progress, alpha = 1) => {
          const baseAngle = scene.attackConfig.angleDeg
          const startAngle = scene.facingAngle - Phaser.Math.DegToRad(baseAngle / 2)
          const endAngle = startAngle + Phaser.Math.DegToRad(baseAngle) * progress
          const outerR = scene.attackConfig.radius * 0.9
          const innerR = scene.attackConfig.radius * 0.3
          const segs = 48
          const innerFade = Math.pow(1 - progress, 6.5)

          gfx.clear()

          // 最外层扩散光晕
          const glowOuter = []
          for (let i = 0; i <= segs; i++) {
            const t = i / segs
            const ang = startAngle + (endAngle - startAngle) * t
            glowOuter.push({ x: scene.player.x + Math.cos(ang) * outerR * 1.1, y: scene.player.y + Math.sin(ang) * outerR * 1.1 })
          }
          gfx.lineStyle(8, 0xff8800, alpha * 0.05)
          gfx.beginPath(); gfx.moveTo(glowOuter[0].x, glowOuter[0].y)
          glowOuter.forEach(p => gfx.lineTo(p.x, p.y)); gfx.strokePath()

          // 中层主体月牙
          const outer = [], inner = []
          for (let i = 0; i <= segs; i++) {
            const t = i / segs
            const ang = startAngle + (endAngle - startAngle) * t
            outer.push({ x: scene.player.x + Math.cos(ang) * outerR, y: scene.player.y + Math.sin(ang) * outerR })
          }
          for (let i = segs; i >= 0; i--) {
            const t = i / segs
            const ang = startAngle + (endAngle - startAngle) * t
            inner.push({ x: scene.player.x + Math.cos(ang) * innerR, y: scene.player.y + Math.sin(ang) * innerR })
          }
          gfx.fillStyle(0xff5500, alpha * 0.9)
          gfx.fillPoints(outer.concat(inner), true)

          // 内层炽白高亮 —— 加速消失
          const inner2 = []
          for (let i = segs; i >= 0; i--) {
            const t = i / segs
            const ang = startAngle + (endAngle - startAngle) * t
            inner2.push({ x: scene.player.x + Math.cos(ang) * innerR * 0.6, y: scene.player.y + Math.sin(ang) * innerR * 0.6 })
          }
          gfx.fillStyle(0xffdd88, alpha * 0.6 * innerFade)
          gfx.fillPoints(inner2, true)

          // 外刃金色描边
          gfx.lineStyle(2, 0xffcc00, alpha * 0.9)
          gfx.beginPath(); gfx.moveTo(outer[0].x, outer[0].y)
          outer.forEach(p => gfx.lineTo(p.x, p.y)); gfx.strokePath()

          // 中心冲击亮核
          gfx.fillStyle(0xffffff, alpha * 0.5 * innerFade)
          gfx.fillCircle(scene.player.x, scene.player.y, innerR * 0.3)
        }

        // 火焰扰动层
        scene.drawFireDistortion = (gfx, progress) => {
          const startAngle = scene.facingAngle - Phaser.Math.DegToRad(scene.attackConfig.angleDeg / 2)
          const endAngle = startAngle + Phaser.Math.DegToRad(scene.attackConfig.angleDeg) * progress
          const baseR = scene.attackConfig.radius
          gfx.clear()
          for (let layer = 0; layer < 4; layer++) {
            const offset = Math.sin(progress * Math.PI * 3 + layer * 2) * 0.12 + (Math.random() - 0.5) * 0.06
            const r = baseR * (0.5 + layer * 0.15)
            const pts = []
            for (let i = 0; i <= 24; i++) {
              const t = i / 24
              const ang = startAngle + (endAngle - startAngle) * t + offset
              pts.push({ x: scene.player.x + Math.cos(ang) * r, y: scene.player.y + Math.sin(ang) * r })
            }
            pts.push({ x: scene.player.x, y: scene.player.y })
            gfx.fillStyle(0xff2200, 0.2 * (1 - layer * 0.18))
            gfx.fillPoints(pts, true)
          }
        }

        // 环形冲击波
        scene.spawnShockwave = () => {
          const ring = scene.add.circle(scene.player.x, scene.player.y, 10, 0xffffff, 0)
          ring.setStrokeStyle(3, 0xff6600).setDepth(12)
          scene.tweens.add({
            targets: ring, radius: scene.attackConfig.radius * 1.2, alpha: 0, duration: 200, ease: 'Cubic.easeOut',
            onUpdate: () => { ring.setStrokeStyle(2, 0xff6600, ring.alpha) },
            onComplete: () => ring.destroy()
          })
        }

        // 火星/粒子爆发
        scene.spawnAttackParticles = (progress, count) => {
          const startAngle = scene.facingAngle - Phaser.Math.DegToRad(scene.attackConfig.angleDeg / 2)
          const endAngle = startAngle + Phaser.Math.DegToRad(scene.attackConfig.angleDeg) * progress
          for (let i = 0; i < count; i++) {
            const ang = endAngle + (Math.random() - 0.5) * Phaser.Math.DegToRad(scene.attackConfig.angleDeg) * 0.8
            const dist = scene.attackConfig.radius * (0.3 + Math.random() * 0.7)
            const px = scene.player.x + Math.cos(ang) * dist
            const py = scene.player.y + Math.sin(ang) * dist
            const isHot = Math.random() < 0.4
            const color = isHot ? 0xffee88 : 0xff4400
            const size = isHot ? 2.5 + Math.random() * 2 : 1.5 + Math.random() * 2
            const dot = scene.add.circle(px, py, size, color).setDepth(11)
            scene.tweens.add({
              targets: dot,
              x: px + Math.cos(ang) * (80 + Math.random() * 40),
              y: py + Math.sin(ang) * (80 + Math.random() * 40),
              alpha: 0, scale: 0.1,
              duration: 180 + Math.random() * 220, ease: 'Cubic.easeOut',
              onComplete: () => dot.destroy()
            })
          }
        }
        scene._ghostCounter = 0
        scene.lastDoorEntered = null
        scene.doorRects = []
        scene.itemsData = []
        // monster AI state: cooldown tracking (ms timestamp per monster name)
        scene.monsterAttackCooldowns = {}  // { monName: lastAttackTime }
        scene.monsterAIPendingAttack = false  // prevent concurrent attack requests

        // update 循环
        this.sys.events.on('update', function (time, delta) {
          const dt = delta / 1000
          const rb = scene._roomBounds || { left: 16, top: 16, right: 800 - 16, bottom: 600 - 16 }

          // block all movement and actions when game is over
          if (scene.gameOver) return

          // movement by WASD
          let vx = 0, vy = 0
          if (scene.keys.W.isDown) vy -= 1
          if (scene.keys.S.isDown) vy += 1
          if (scene.keys.A.isDown) vx -= 1
          if (scene.keys.D.isDown) vx += 1
          if (vx !== 0 || vy !== 0) {
            const len = Math.sqrt(vx*vx + vy*vy)
            vx = vx / len
            vy = vy / len
            try { scene.facingAngle = Math.atan2(vy, vx) } catch (e) {}
            let speed = scene.baseMoveSpeed
            try {
              if (scene.keys.SHIFT && scene.keys.SHIFT.isDown) speed = speed * 2
            } catch (e) {}
            const prevX = scene.player.x, prevY = scene.player.y
            scene.player.x += vx * speed * dt
            scene.player.y += vy * speed * dt
            try {
              const dx = scene.player.x - prevX, dy = scene.player.y - prevY
              if (scene.bgFar) { scene.bgFar.tilePositionX += dx * (scene.parallax?.farFactor ?? 0.35); scene.bgFar.tilePositionY += dy * (scene.parallax?.farFactor ?? 0.35) }
              if (scene.bgNear) { scene.bgNear.tilePositionX += dx * (scene.parallax?.nearFactor ?? 0.72); scene.bgNear.tilePositionY += dy * (scene.parallax?.nearFactor ?? 0.72) }
            } catch (e) {}
          }

          // ---------- 怪物 AI：索敌 + 追击 + 攻击 ----------
          const MONSTER_DETECT_RANGE = 280   // 发现玩家的距离
          const MONSTER_ATTACK_RANGE = 45    // 攻击距离
          const MONSTER_ATTACK_COOLDOWN = 1500 // 攻击间隔 (ms)
          const MONSTER_SPEED = 80           // 怪物移动速度 (pixel/s)
          const pr = scene.playerRadius || 10

          let attackingThisFrame = false
          const now = Date.now()

          for (const mon of scene.monstersData) {
            if (!mon || !mon.circ) continue
            const dx = scene.player.x - mon.x
            const dy = scene.player.y - mon.y
            const dist = Math.sqrt(dx * dx + dy * dy)

            if (dist <= MONSTER_ATTACK_RANGE) {
              // 在攻击范围内 → 攻击玩家
              if (!scene.monsterAttackCooldowns[mon.name] || now - scene.monsterAttackCooldowns[mon.name] >= MONSTER_ATTACK_COOLDOWN) {
                scene.monsterAttackCooldowns[mon.name] = now
                if (!scene.monsterAIPendingAttack) {
                  scene.monsterAIPendingAttack = true
                  attackingThisFrame = true
                  ;(async () => {
                    try {
                      const res = await fetch('/api/command', {
                        method: 'POST', headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ command: 'attack ' + mon.name })
                      })
                      const j = await res.json()
                      emit('update', j)
                      if (j && j.data) scene.renderRoom(j.data)
                      if (j && j.message && j.message.includes('游戏结束')) {
                        scene.showGameOver()
                      }
                    } catch (e) {
                      emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                    }
                    scene.monsterAIPendingAttack = false
                  })()
                }
              }
            } else if (dist <= MONSTER_DETECT_RANGE) {
              // 在索敌范围内 → 向玩家移动
              const norm = Math.max(1, dist)
              const mvx = (dx / norm) * MONSTER_SPEED * dt
              const mvy = (dy / norm) * MONSTER_SPEED * dt
              mon.x += mvx
              mon.y += mvy
              // 限制在房间边界内
              mon.x = Phaser.Math.Clamp(mon.x, rb.left + pr, rb.right - pr)
              mon.y = Phaser.Math.Clamp(mon.y, rb.top + pr, rb.bottom - pr)
              // 更新怪物精灵位置
              try { mon.circ.setPosition(mon.x, mon.y) } catch (e) {}
              try { mon.label.setPosition(mon.x - 32, mon.y + 24) } catch (e) {}
            }
          }

          // handle attack key (J) pressed — enhanced sweep with particles, plus pierce on Shift+move
          // Also detect monsters in range and send damage commands to backend
          try {
            if (scene.keys.J && Phaser.Input.Keyboard.JustDown(scene.keys.J)) {
              const cfg = scene.attackConfig || {}

              // helper: send attack command to backend for a specific monster
              const attackMonster = async (monName) => {
                try {
                  const res = await fetch('/api/command', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: 'attack ' + monName })
                  })
                  const j = await res.json()
                  emit('update', j)
                  if (j && j.data) {
                    scene.renderRoom(j.data)
                  }
                  if (j && j.message && j.message.includes('游戏结束')) {
                    scene.showGameOver()
                  }
                } catch (e) {
                  emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                }
              }

              // helper: check if a monster is inside the sweep sector (fan shape)
              const isMonsterInSweep = (monster) => {
                const monDx = monster.x - scene.player.x
                const monDy = monster.y - scene.player.y
                const dist = Math.sqrt(monDx * monDx + monDy * monDy)
                if (dist > (cfg.radius || 110)) return false
                const angleToMon = Math.atan2(monDy, monDx)
                let angleDiff = angleToMon - scene.facingAngle
                while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI
                while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI
                const halfSpan = Phaser.Math.DegToRad((cfg.angleDeg || 135) / 2)
                return Math.abs(angleDiff) <= halfSpan
              }

              // helper: check if a monster is along the pierce line
              const isMonsterInPierce = (monster, startX, startY, dirX, dirY, dist) => {
                const monDx = monster.x - startX
                const monDy = monster.y - startY
                const along = monDx * dirX + monDy * dirY
                if (along < 0 || along > dist) return false
                const perpDist = Math.abs(monDx * (-dirY) + monDy * dirX)
                const halfW = (cfg.pierceWidth || 12) / 2 + 15
                return perpDist <= halfW
              }

              // determine attack type
              const isShiftMove = (scene.keys.SHIFT && scene.keys.SHIFT.isDown) && (scene.keys.W.isDown || scene.keys.A.isDown || scene.keys.S.isDown || scene.keys.D.isDown)

              // detect hit monsters before any visual effect
              const hitMonsters = []
              if (isShiftMove) {
                const startX = scene.player.x
                const startY = scene.player.y
                const dx = Math.cos(scene.facingAngle)
                const dy = Math.sin(scene.facingAngle)
                const dist = cfg.pierceDistance || 120
                for (const mon of scene.monstersData) {
                  if (isMonsterInPierce(mon, startX, startY, dx, dy, dist)) {
                    hitMonsters.push(mon.name)
                  }
                }
              } else {
                for (const mon of scene.monstersData) {
                  if (isMonsterInSweep(mon)) {
                    hitMonsters.push(mon.name)
                  }
                }
              }

              if (isShiftMove) {
                // === PIERCE ATTACK ===
                const startX = scene.player.x
                const startY = scene.player.y
                const dx = Math.cos(scene.facingAngle)
                const dy = Math.sin(scene.facingAngle)
                const dist = cfg.pierceDistance || 120
                const pr = scene.playerRadius || 10
                const targetX = Phaser.Math.Clamp(startX + dx * dist, rb.left + pr, rb.right - pr)
                const targetY = Phaser.Math.Clamp(startY + dy * dist, rb.top + pr, rb.bottom - pr)

                scene.tweens.add({ targets: scene.player, x: targetX, y: targetY, duration: cfg.pierceDuration || 100, ease: 'Cubic.easeOut' })
                try {
                  const g2 = scene.add.graphics()
                  const extra = cfg.pierceDistanceExpand || 1.0
                  const effTargetX = Phaser.Math.Clamp(startX + dx * dist * extra, rb.left + pr, rb.right - pr)
                  const effTargetY = Phaser.Math.Clamp(startY + dy * dist * extra, rb.top + pr, rb.bottom - pr)
                  const mx = (startX + effTargetX) / 2, my = (startY + effTargetY) / 2
                  const w = cfg.pierceWidth || 12
                  const px = -dy, py = dx
                  const hx = px * (w/2), hy = py * (w/2)
                  g2.fillStyle(0xC0C0C0, cfg.mainAlpha || 0.95)
                  g2.fillPoints([
                    { x: effTargetX, y: effTargetY },
                    { x: mx + hx, y: my + hy },
                    { x: startX, y: startY },
                    { x: mx - hx, y: my - hy }
                  ], true)
                  scene.tweens.add({ targets: g2, alpha: 0, duration: cfg.pierceFade || 180, onComplete: () => { try { g2.destroy() } catch (e) {} } })
                } catch (e) {}
              } else {
                // === SWEEP ATTACK (enhanced visuals) ===
                const mainGfx = scene.add.graphics()
                const fireGfx = scene.add.graphics()
                const progress = { t: 0 }
                const duration = scene.attackConfig.sweepDuration || 160

                scene.drawArcSlash(mainGfx, 0)
                scene.drawFireDistortion(fireGfx, 0)
                scene.spawnAttackParticles(0, 20)
                scene.spawnShockwave()
                if (scene.cameras && scene.cameras.main) {
                  scene.cameras.main.shake(120, 0.005)
                }

                scene.tweens.add({
                  targets: progress,
                  t: 1,
                  duration: duration,
                  ease: 'Cubic.easeOut',
                  onUpdate: () => {
                    const t = progress.t
                    scene.drawArcSlash(mainGfx, t, 0.95)
                    scene.drawFireDistortion(fireGfx, t)
                    if (t > 0.1 && Math.random() < 0.5) {
                      const ghost = scene.add.graphics()
                      scene.drawArcSlash(ghost, t, 0.3)
                      scene.tweens.add({ targets: ghost, alpha: 0, duration: 80, onComplete: () => ghost.destroy() })
                    }
                    if (Math.random() < 0.5 && t > 0.15 && t < 0.9) {
                      scene.spawnAttackParticles(t, 4)
                    }
                  },
                  onComplete: () => {
                    scene.spawnAttackParticles(1, 15)
                    scene.tweens.add({ targets: mainGfx, alpha: 0, duration: 100, ease: 'Cubic.easeIn', onComplete: () => mainGfx.destroy() })
                    scene.tweens.add({ targets: fireGfx, alpha: 0, duration: 150, ease: 'Cubic.easeIn', onComplete: () => fireGfx.destroy() })
                  }
                })
              }

              // send attack commands for all monsters hit
              for (const monName of hitMonsters) {
                attackMonster(monName)
              }
            }
          } catch (e) { /* ignore input issues */ }

          // 玩家边界限制
          const pr2 = scene.playerRadius || 14
          scene.player.x = Phaser.Math.Clamp(scene.player.x, rb.left + pr2, rb.right - pr2)
          scene.player.y = Phaser.Math.Clamp(scene.player.y, rb.top + pr2, rb.bottom - pr2)
          scene.playerLabel.setPosition(scene.player.x - 20, scene.player.y + 28)

          // 门检测
          let insideAnyDoor = false
          if (scene.doorRects) {
            for (const dr of scene.doorRects) {
              const b = dr.rect.getBounds()
              if (scene.player.x >= b.left && scene.player.x <= b.right && scene.player.y >= b.top && scene.player.y <= b.bottom) {
                insideAnyDoor = true
                if (scene.lastDoorEntered !== dr.dir) {
                  scene.lastDoorEntered = dr.dir
                  scene.sendCommand('go ' + dr.dir, dr.dir)
                }
                break
              }
            }
          }
          if (!insideAnyDoor) scene.lastDoorEntered = null

          // 物品拾取提示
          if (scene.itemsData) {
            const pickupRadius = 40
            for (let i = scene.itemsData.length - 1; i >= 0; i--) {
              const it = scene.itemsData[i]
              if (!it || it._removed) continue
              const dist = Phaser.Math.Distance.Between(scene.player.x, scene.player.y, it.x, it.y)
              if (dist <= pickupRadius) {
                if (!it.prompted) {
                  it.prompted = true
                  try {
                    const ok = window.confirm('是否将 ' + it.name + ' 放入背包？')
                    if (ok) {
                      (async () => {
                        try {
                          const res = await fetch('/api/command', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ command: 'take ' + it.name })
                          })
                          const j = await res.json()
                          emit('update', j)
                          if (j && j.status === 'success') {
                            it.circle.destroy()
                            it.label.destroy()
                            it._removed = true
                            scene.itemsData.splice(i, 1)
                          }
                          if (j && j.data) scene.renderRoom(j.data)
                        } catch (e) {
                          emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                        }
                      })()
                    }
                  } catch (e) {}
                }
              } else {
                if (it.prompted) it.prompted = false
              }
            }
          }
        })

        // 获取初始游戏状态
        try {
          const res = await fetch('/api/game')
          const j = await res.json()
          emit('update', j)
          if (j && j.data) scene.renderRoom(j.data)
        } catch (e) {
          emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
        }
      },
      destroy: function () {
        // 场景销毁时的清理（如有需要可添加）
      }
    }
  }

  game = new Phaser.Game(config)

  // 初始化小地图并监听更新事件
  initMinimap()
  window.addEventListener('minimap:update', onMinimapUpdate)
})

onBeforeUnmount(() => {
  if (game) {
    try { game.destroy(true) } catch (e) {}
    game = null
  }
  window.removeEventListener('minimap:update', onMinimapUpdate)
  // 清理其他全局事件
  try {
    if (window.__zuul_key_handler) {
      window.removeEventListener('keydown', window.__zuul_key_handler)
      delete window.__zuul_key_handler
    }
    if (window.__zuul_game_update_handler) {
      window.removeEventListener('game:update', window.__zuul_game_update_handler)
      delete window.__zuul_game_update_handler
    }
  } catch (e) {}
})
</script>

<style scoped>
.minimap {
  /* 可根据需要微调样式 */
}
</style>