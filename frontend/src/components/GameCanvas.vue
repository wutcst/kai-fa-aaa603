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

  // 2. 绘制房间矩形
  for (const room of rooms) {
    const rect = getRoomRect(room.name)
    if (!rect) continue
    const isCurrent = room.name === highlightRoomName
    ctx.fillStyle = isCurrent ? 'rgba(255, 215, 0, 0.9)' : 'rgba(100, 149, 237, 0.7)'
    ctx.fillRect(rect.x, rect.y, rect.w, rect.h)
    ctx.strokeStyle = isCurrent ? '#FFD700' : 'rgba(255,255,255,0.8)'
    ctx.lineWidth = isCurrent ? 2 : 1
    ctx.strokeRect(rect.x, rect.y, rect.w, rect.h)
    // 房间编号
    ctx.fillStyle = '#fff'
    ctx.font = `${Math.max(8, rect.w * 0.4)}px Arial`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(room.name.charAt(room.name.length - 1) || '?', rect.x + rect.w / 2, rect.y + rect.h / 2)
  }
}

async function initMinimap() {
  try {
    const res = await fetch('/api/map')
    const data = await res.json()
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

        // 容器和图形
        scene.roomGraphics = scene.add.container(0, 0)
        scene.itemsGroup = scene.add.group()
        scene.exitButtons = []
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

        scene.titleText = scene.add.text(20, 20, '', { font: '20px Arial', fill: '#ffffff' })
        scene.descText = scene.add.text(20, 50, '', { font: '14px Arial', fill: '#cccccc', wordWrap: { width: 760 } })

        scene.playerRadius = 10
        scene.player = scene.add.circle(400, 320, scene.playerRadius, 0x00aaff).setStrokeStyle(2, 0x000000)
        scene.playerLabel = scene.add.text(400 - 20, 320 + 18, 'You', { font: '12px Arial', fill: '#fff' })
        scene._roomBounds = { left: scene.playerRadius, top: scene.playerRadius, right: 800 - scene.playerRadius, bottom: 600 - scene.playerRadius }

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
            if (j && j.data) scene.renderRoom(j.data)
          } catch (e) {
            emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
          }
        }

        // renderRoom 核心函数
        scene.renderRoom = function (roomInfo) {
          scene.itemsGroup.clear(true, true)
          scene.itemsData = []
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
              scene.tweens.add({ targets: scene.player, x: posx, y: posy, duration: 180 })
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
                  scene.tweens.add({ targets: [circle, label], y: '-=100', alpha: 0, scale: 0.5, duration: 500, onComplete: () => { circle.destroy(); label.destroy() } })
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
          const mStartX = 160
          let mi = 0
          monsters.forEach(mon => {
            const x = mStartX + (mi % 2) * 60
            const y = 220 + Math.floor(mi / 2) * 70
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

          if (roomInfo.isTeleportRoom) {
            scene.add.text(600, 20, '传送房间', { font: '14px Arial', fill: '#ffcc00' })
          }

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
        scene._ghostCounter = 0
        scene.lastDoorEntered = null
        scene.doorRects = []
        scene.itemsData = []

        // update 循环
        this.sys.events.on('update', function (time, delta) {
          const dt = delta / 1000
          const rb = scene._roomBounds || { left: 16, top: 16, right: 800 - 16, bottom: 600 - 16 }
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

          // 攻击逻辑 (保留 J 键攻击和 Shift+移动穿刺)
          if (scene.keys.J && Phaser.Input.Keyboard.JustDown(scene.keys.J)) {
            const cfg = scene.attackConfig || {}
            const isShiftMove = (scene.keys.SHIFT && scene.keys.SHIFT.isDown) && (scene.keys.W.isDown || scene.keys.A.isDown || scene.keys.S.isDown || scene.keys.D.isDown)
            if (isShiftMove) {
              // 穿刺攻击
              const startX = scene.player.x, startY = scene.player.y
              const dx = Math.cos(scene.facingAngle), dy = Math.sin(scene.facingAngle)
              const pr = scene.playerRadius || 10
              const targetX = Phaser.Math.Clamp(startX + dx * cfg.pierceDistance, rb.left + pr, rb.right - pr)
              const targetY = Phaser.Math.Clamp(startY + dy * cfg.pierceDistance, rb.top + pr, rb.bottom - pr)
              scene.tweens.add({ targets: scene.player, x: targetX, y: targetY, duration: cfg.pierceDuration || 100, ease: 'Cubic.easeOut' })
              try {
                const g2 = scene.add.graphics()
                const extra = cfg.pierceDistanceExpand || 1.0
                const effTargetX = Phaser.Math.Clamp(startX + dx * cfg.pierceDistance * extra, rb.left + pr, rb.right - pr)
                const effTargetY = Phaser.Math.Clamp(startY + dy * cfg.pierceDistance * extra, rb.top + pr, rb.bottom - pr)
                const mx = (startX + effTargetX) / 2, my = (startY + effTargetY) / 2
                const px = -dy, py = dx
                const hx = px * (cfg.pierceWidth/2), hy = py * (cfg.pierceWidth/2)
                const front = { x: effTargetX, y: effTargetY }
                const right = { x: mx + hx, y: my + hy }
                const back = { x: startX, y: startY }
                const left = { x: mx - hx, y: my - hy }
                g2.fillStyle(0xC0C0C0, cfg.mainAlpha || 0.95)
                g2.fillPoints([front, right, back, left], true)
                scene.tweens.add({ targets: g2, alpha: 0, duration: cfg.pierceFade || 180, onComplete: () => { try { g2.destroy() } catch (e) {} } })
              } catch (e) {}
            } else {
              // 扇形攻击
              const cx = scene.player.x, cy = scene.player.y
              const radius = cfg.radius || 110
              const spanRad = Phaser.Math.DegToRad(cfg.angleDeg || 135)
              const half = spanRad / 2
              const startAngle = scene.facingAngle - half
              const segments = cfg.segments || 30
              const redraw = (gfx, progress, alpha = cfg.mainAlpha || 0.85, color = 0xff4444) => {
                gfx.clear()
                const grow = cfg.radiusGrow || 0.08
                const adjRadius = radius * (1 + grow * Phaser.Math.Clamp(progress, 0, 1))
                gfx.fillStyle(color, alpha)
                const usedSpan = spanRad * Phaser.Math.Clamp(progress, 0, 1)
                const endAngle = startAngle + usedSpan
                const points = []
                points.push({ x: cx, y: cy })
                for (let i = 0; i <= segments; i++) {
                  const t = i / segments
                  const ang = startAngle + (endAngle - startAngle) * t
                  points.push({ x: cx + Math.cos(ang) * adjRadius, y: cy + Math.sin(ang) * adjRadius })
                }
                gfx.fillPoints(points, true)
                // 中心圆环
                const ringInner = Math.max(4, adjRadius * 0.12)
                const ringThickness = Math.max(3, adjRadius * 0.06)
                const ringOuter = ringInner + ringThickness
                gfx.fillStyle(0xC0C0C0, alpha * 0.9)
                gfx.fillCircle(cx, cy, ringOuter)
                gfx.fillStyle(scene.bgColor || 0x2d2d2d, 1)
                gfx.fillCircle(cx, cy, ringInner)
              }
              const g = scene.add.graphics()
              redraw(g, 0)
              let lastGhostT = -1
              const ghostSpacing = cfg.ghostSpacing || 0.12
              const ghostFade = cfg.ghostFade || 150
              const sweepDuration = cfg.sweepDuration || 150
              const finalFade = cfg.finalFade || 100
              const prog = { t: 0 }
              scene.tweens.add({
                targets: prog,
                t: 1,
                duration: sweepDuration,
                ease: 'Linear',
                onUpdate: () => {
                  try {
                    redraw(g, prog.t)
                    if (prog.t - lastGhostT >= ghostSpacing) {
                      lastGhostT = prog.t
                      const ghost = scene.add.graphics()
                      const gAlpha = cfg.ghostAlpha || 0.92
                      redraw(ghost, prog.t, gAlpha, 0xC0C0C0)
                      try { ghost.setDepth(scene._ghostCounter++) } catch (e) {}
                      const minFade = cfg.ghostMinFade || 40
                      const fadeDur = Math.max(minFade, ghostFade * (0.15 + prog.t * 0.85))
                      const scaledFade = Math.max(minFade, fadeDur * 0.7)
                      scene.tweens.add({ targets: ghost, alpha: 0, duration: scaledFade, onComplete: () => { try { ghost.destroy() } catch (e) {} } })
                    }
                  } catch (e) {}
                },
                onComplete: () => {
                  scene.tweens.add({ targets: g, alpha: 0, duration: finalFade, onComplete: () => { try { g.destroy() } catch (e) {} } })
                }
              })
            }
          }

          // 玩家边界限制
          const pr2 = scene.playerRadius || 10
          scene.player.x = Phaser.Math.Clamp(scene.player.x, rb.left + pr2, rb.right - pr2)
          scene.player.y = Phaser.Math.Clamp(scene.player.y, rb.top + pr2, rb.bottom - pr2)
          scene.playerLabel.setPosition(scene.player.x - 20, scene.player.y + 18)

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