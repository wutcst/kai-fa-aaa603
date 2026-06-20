<template>
  <canvas
    ref="minimapCanvas"
    class="minimap"
    width="160"
    height="160"
  ></canvas>
</template>

<script setup>
/**
 * MinimapPanel.vue — 小地图独立组件
 *
 * 通信方式：监听 window 上的 CustomEvent
 *   - minimap:update → 重绘高亮房间
 *   - game:reset      → 清空旧数据并重新 fetch /api/map
 *
 * 与 BackpackPanel / ControlPanel 保持一致的组件化模式。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ROOM_TYPE_COLORS, MINIMAP_CONFIG } from '../game/constants.js'

const minimapCanvas = ref(null)

// ---------- 状态 ----------
let mapLayout = null          // { rooms, roomMap, coords }
let currentRoomName = ''      // 当前玩家所在房间名

// ---------- 核心函数 ----------

function buildMapLayout(mapData) {
  const { rooms, startRoomName } = mapData
  const roomMap = new Map(rooms.map(r => [r.name, r]))
  const coords = new Map()   // roomName -> {x, y}
  const visited = new Set()

  const queue = [startRoomName]
  coords.set(startRoomName, { x: 0, y: 0 })
  visited.add(startRoomName)

  const dirVec = MINIMAP_CONFIG.dirVec

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

  const margin = MINIMAP_CONFIG.margin
  const availW = canvas.width - margin * 2
  const availH = canvas.height - margin * 2
  const rangeX = maxX - minX + 1
  const rangeY = maxY - minY + 1
  const cellSize = Math.min(availW / rangeX, availH / rangeY, MINIMAP_CONFIG.maxCellSize)
  const rectW = cellSize * MINIMAP_CONFIG.rectSizeRatio
  const rectH = cellSize * MINIMAP_CONFIG.rectSizeRatio
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

  // 2. 绘制房间矩形（按房间类型不同颜色，使用统一常量）
  for (const room of rooms) {
    const rect = getRoomRect(room.name)
    if (!rect) continue
    const isCurrent = room.name === highlightRoomName
    const roomType = room.roomType || 'NORMAL_MONSTER'
    const typeColor = ROOM_TYPE_COLORS[roomType] || ROOM_TYPE_COLORS.NORMAL_MONSTER
    if (isCurrent) {
      ctx.fillStyle = ROOM_TYPE_COLORS.CURRENT_ROOM.fill
      ctx.strokeStyle = ROOM_TYPE_COLORS.CURRENT_ROOM.stroke
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
  // 立即清空旧地图布局，防止并发的 drawMinimap 调用使用过期数据绘制
  mapLayout = null
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
  currentRoomName = e.detail.roomName || ''
  drawMinimap(e.detail.roomName)
}

// ---------- 生命周期 ----------

onMounted(() => {
  initMinimap()
  window.addEventListener('minimap:update', onMinimapUpdate)
  window.addEventListener('game:reset', initMinimap)
})

onBeforeUnmount(() => {
  window.removeEventListener('minimap:update', onMinimapUpdate)
  window.removeEventListener('game:reset', initMinimap)
})
</script>

<style scoped>
.minimap {
  position: absolute;
  bottom: 10px;
  right: 10px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(0, 0, 0, 0.55);
  border-radius: 4px;
  pointer-events: none;
  z-index: 10;
}
</style>
