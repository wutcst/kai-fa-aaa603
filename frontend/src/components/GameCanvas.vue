<template>
  <div style="position: relative; width: 800px; height: 600px;">
    <!-- Phaser 游戏主容器 -->
    <div ref="gameContainer" style="width:800px;height:600px;background:#000;"></div>

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

    <!-- ==================== 背包 UI 覆盖层 ==================== -->
    <div v-if="backpackVisible" class="backpack-overlay" @click.self="closeBackpack">
      <div class="backpack-panel">
        <!-- 标题栏 -->
        <div class="backpack-header">
          <span class="backpack-title">🎒 背包</span>
          <span class="backpack-close" @click="closeBackpack">✕</span>
        </div>

        <div class="backpack-body">
          <!-- 左侧：5x3 物品格子 -->
          <div class="backpack-left">
            <div
                v-for="i in 15"
                :key="'slot-' + i"
                class="backpack-slot"
                :class="{
                'has-item': getSlotItem(i - 1),
                'slot-selected': selectedSlot === (i - 1),
                'slot-hovered': hoveredSlot === (i - 1)
              }"
                @click="selectSlot(i - 1)"
                @mouseenter="hoveredSlot = (i - 1)"
                @mouseleave="hoveredSlot = null"
            >
              <!-- 物品简略图标 -->
              <!-- 生命浆果：三个红色重叠圆点 -->
              <div v-if="getSlotItem(i - 1) && getSlotItem(i - 1).rarity === 'lifeBerry'" class="slot-icon slot-icon-lifeberry">
                <span class="berry-dot" style="left:10px;top:14px;"></span>
                <span class="berry-dot" style="left:20px;top:10px;"></span>
                <span class="berry-dot" style="left:28px;top:18px;"></span>
              </div>
              <!-- 魔力浆果：三个蓝色重叠椭圆 -->
              <div v-else-if="getSlotItem(i - 1) && getSlotItem(i - 1).rarity === 'manaBerry'" class="slot-icon slot-icon-manaberry">
                <span class="mana-ellipse" style="left:10px;top:16px;"></span>
                <span class="mana-ellipse" style="left:18px;top:10px;"></span>
                <span class="mana-ellipse" style="left:26px;top:18px;"></span>
              </div>
              <!-- 其他物品：显示名称首字母 -->
              <div v-else-if="getSlotItem(i - 1)" class="slot-icon">
                {{ getSlotItem(i - 1).name.charAt(0) }}
              </div>
              <!-- 物品数量角标 -->
              <span v-if="getSlotItem(i - 1)" class="slot-qty">x{{ getSlotItem(i - 1).quantity }}</span>
            </div>
          </div>

          <!-- 右侧：物品详情 -->
          <div class="backpack-right">
            <!-- 上方大图像框 -->
            <div class="detail-image-frame">
              <span v-if="!selectedItem" class="placeholder-text">选择物品查看详情</span>
              <div v-else class="detail-image-placeholder">
                {{ selectedItem.name.charAt(0) }}
              </div>
            </div>

            <!-- 下方物品信息 -->
            <div class="detail-info">
              <template v-if="selectedItem">
                <!-- 第一行：名称 + 编号 + 数量 -->
                <div class="detail-row1">
                  <span class="detail-name" :style="{ color: rarityColor(selectedItem.rarity) }">
                    {{ selectedItem.name }}
                  </span>
                  <span class="detail-id" :style="{ color: rarityColor(selectedItem.rarity) }">
                    NO.{{ String(selectedItem.itemId).padStart(2, '0') }}
                  </span>
                  <span class="detail-qty" :style="{ color: rarityColor(selectedItem.rarity) }">
                    拥有：{{ selectedItem.quantity }}
                  </span>
                </div>
                <!-- 功能描述 -->
                <div class="detail-func">
                  {{ selectedItem.functionDesc }}
                </div>
                <!-- 空行间距 -->
                <div class="detail-spacer"></div>
                <!-- 背景描述（斜体） -->
                <div class="detail-lore">
                  "{{ selectedItem.loreDesc }}"
                </div>
              </template>
              <template v-else>
                <div class="detail-empty">← 点击左侧物品查看详情</div>
              </template>
            </div>

            <!-- 操作按钮 -->
            <div class="detail-buttons">
              <button
                  class="btn-use"
                  :disabled="!selectedItem"
                  @click="useItem"
              >使用</button>
              <button
                  class="btn-discard"
                  :disabled="!selectedItem"
                  @click="discardItem"
              >丢弃</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Phaser from 'phaser'

const emit = defineEmits(['update'])
const gameContainer = ref(null)
const minimapCanvas = ref(null)
let game = null

// ==================== 背包 UI 响应式状态 ====================
const backpackVisible = ref(false)
const selectedSlot = ref(null)      // 选中的格子索引 (0-14)
const hoveredSlot = ref(null)       // 鼠标悬停的格子索引
const backpackItems = ref([])       // 背包物品列表 [{itemId, name, rarity, functionDesc, loreDesc, quantity, ...}]

// 稀有度对应颜色
function rarityColor(rarity) {
  switch (rarity) {
    case 'legendary': return '#FF6600'
    case 'epic': return '#CC44FF'
    case 'rare': return '#4488FF'
    case 'lifeBerry': return '#44cc44' // 生命浆果 → 绿色
    case 'manaBerry': return '#4488ff' // 魔力浆果 → 蓝色
    default: return '#FFD700' // common -> 金色
  }
}

// 获取某个格子对应的物品
function getSlotItem(slotIndex) {
  return slotIndex < backpackItems.value.length ? backpackItems.value[slotIndex] : null
}

// 当前选中的物品
const selectedItem = ref(null)

// 选中格子
function selectSlot(slotIndex) {
  const item = getSlotItem(slotIndex)
  if (item) {
    selectedSlot.value = slotIndex
    selectedItem.value = item
  } else {
    selectedSlot.value = null
    selectedItem.value = null
  }
}

// 从后端刷新背包数据
async function refreshBackpack() {
  try {
    const res = await fetch('/api/backpack')
    const j = await res.json()
    console.log('[Backpack] API full response:', JSON.stringify(j, null, 2))
    if (j && j.data && j.data.backpack) {
      backpackItems.value = j.data.backpack
      console.log('[Backpack] items loaded:', backpackItems.value.length,
        backpackItems.value.map(it => ({ name: it.name, rarity: it.rarity, qty: it.quantity })))
    } else {
      console.warn('[Backpack] No backpack data in response. data keys:', j.data ? Object.keys(j.data) : 'null')
    }
  } catch (e) {
    console.warn('[Backpack] 无法获取背包数据', e)
  }
}

// 使用物品
async function useItem() {
  if (!selectedItem.value) return
  try {
    const res = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: 'bag use ' + selectedItem.value.name })
    })
    const j = await res.json()
    emit('update', j)
    if (j && j.data) {
      if (j.data.backpack) backpackItems.value = j.data.backpack
      // 通知游戏场景更新渲染
      window.dispatchEvent(new CustomEvent('game:update', { detail: j }))
    }
    // 重置选择
    if (selectedItem.value) {
      const stillExists = backpackItems.value.find(it => it.name === selectedItem.value.name)
      if (!stillExists) {
        selectedSlot.value = null
        selectedItem.value = null
      } else {
        const idx = backpackItems.value.indexOf(stillExists)
        selectedSlot.value = idx
        selectedItem.value = stillExists
      }
    }
  } catch (e) {
    console.warn('使用物品失败', e)
  }
}

// 丢弃物品
async function discardItem() {
  if (!selectedItem.value) return
  if (!confirm('确定要丢弃全部 "' + selectedItem.value.name + '" 吗？')) return
  try {
    const res = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: 'bag discard ' + selectedItem.value.name })
    })
    const j = await res.json()
    emit('update', j)
    if (j && j.data) {
      if (j.data.backpack) backpackItems.value = j.data.backpack
      window.dispatchEvent(new CustomEvent('game:update', { detail: j }))
    }
    selectedSlot.value = null
    selectedItem.value = null
  } catch (e) {
    console.warn('丢弃物品失败', e)
  }
}

// 关闭背包（同时通知 Phaser 恢复游戏）
function closeBackpack() {
  backpackVisible.value = false
  window.dispatchEvent(new CustomEvent('backpack:toggle', { detail: { visible: false } }))
}

// B 键切换背包（全局监听）
function onKeyDown(e) {
  if (e.key === 'b' || e.key === 'B') {
    // 避免在输入框中误触发
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return
    e.preventDefault()
    backpackVisible.value = !backpackVisible.value
    if (backpackVisible.value) {
      refreshBackpack()
      selectedSlot.value = null
      selectedItem.value = null
    }
    // 通知 Phaser 场景暂停/恢复
    window.dispatchEvent(new CustomEvent('backpack:toggle', { detail: { visible: backpackVisible.value } }))
  }
  // 背包打开时阻止 Esc 之外的其他按键进入游戏
  if (backpackVisible.value) {
    if (e.key === 'Escape') {
      closeBackpack()
    }
    // 阻止所有游戏按键传递
    if (['w','W','a','A','s','S','d','D','j','J',' ', 'h','H','ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(e.key)) {
      e.stopPropagation()
      e.stopImmediatePropagation()
    }
  }
}

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
          // load small decorative tile (if present)
          try {
            this.load.image('grass_tile', new URL('../assets/grass_tile.svg', import.meta.url).href)
          } catch (e) {}

          // try loading up to 4 background images named bg0.png..bg3.png placed in src/assets/
          // If you want to use the images you showed, put them in frontend/src/assets as bg0.png, bg1.png, bg2.png, bg3.png
          for (let i = 0; i < 4; i++) {
            try {
              const path = new URL(`../assets/bg${i}.png`, import.meta.url).href
              this.load.image(`bg${i}`, path)
            } catch (e) {
              // file may not exist during dev; ignore
            }
          }
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

          // Create background — use Image (单张不重复) for user-provided bg images; fallback to tileSprite for procedural grass
          const availableBgKeys = []
          for (let i = 0; i < 4; i++) { if (scene.textures.exists(`bg${i}`)) availableBgKeys.push(`bg${i}`) }

          let bgFar, bgNear
          if (availableBgKeys.length > 0) {
            // 使用单张 Image，不再平铺
            bgFar = scene.add.image(0, 0, availableBgKeys[0]).setOrigin(0, 0)
            bgFar._isSingleImage = true
            // near layer is invisible since we use user-provided background images
            bgNear = scene.add.tileSprite(0, 0, 800, 600, v1).setOrigin(0, 0)
            bgNear.setAlpha(0).setScale(1.02)
          } else {
            // fallback: fully procedural grass (seamless, tiling is fine)
            bgFar = scene.add.tileSprite(0, 0, 800, 600, v0).setOrigin(0, 0)
            bgNear = scene.add.tileSprite(0, 0, 800, 600, v1).setOrigin(0, 0)
            bgNear.setAlpha(0.88).setScale(1.02)
          }

          scene.roomGraphics.addAt(bgFar, 0)
          scene.roomGraphics.addAt(bgNear, 1)
          scene.bgFar = bgFar
          scene.bgNear = bgNear

          // ---------- 房间边界框遮罩（将背景裁剪到传送门围成的矩形内） ----------
          scene.roomMaskShape = scene.add.graphics()
          scene.roomMaskShape.setPosition(0, 0)
          scene.roomMaskShape.setVisible(false)  // 仅用作遮罩形状，不可见
          scene.roomMask = null  // 在第一次 renderRoom 时创建遮罩并应用

          // 将背景图片缩放适配到房间矩形内
          scene.fitBgToRoom = function (rectLeft, rectTop, roomW, roomH) {
            if (!scene.bgFar || !scene.bgFar._isSingleImage) return
            try {
              // 图片原点设为 (0,0)，位置设在矩形左上角，尺寸缩放为房间矩形大小
              scene.bgFar.setPosition(rectLeft, rectTop)
              scene.bgFar.setDisplaySize(roomW, roomH)
              scene.bgFar.setOrigin(0, 0)
            } catch (e) {}
          }

          // Helper to choose and apply a background image for a given room
          scene.setBgForRoom = function (roomInfo) {
            try {
              const roomType = (roomInfo && roomInfo.roomType) ? roomInfo.roomType : null
              const mapping = {
                START_HALL: 'bg0',
                SHOP: 'bg1',
                ENCOUNTER: 'bg2',
                BOSS: 'bg3'
              }
              const avail = []
              for (let i = 0; i < 4; i++) if (scene.textures.exists(`bg${i}`)) avail.push(`bg${i}`)
              if (avail.length === 0) {
                // no image backgrounds available -> keep procedural textures
                return
              }
              // prefer mapping, otherwise random available
              let chosen = null
              if (roomType && mapping[roomType] && scene.textures.exists(mapping[roomType])) chosen = mapping[roomType]
              if (!chosen) chosen = avail[Math.floor(Math.random() * avail.length)]
              // apply chosen key to far layer
              try { scene.bgFar.setTexture(chosen) } catch (e) {}
              // ensure near layer stays invisible
              try { scene.bgNear.setAlpha(0); scene.bgFar.setAlpha(1.0); scene.bgNear.setScale(1.02) } catch (e) {}
            } catch (e) {}
          }
        scene._prevPlayerX = 400
        scene._prevPlayerY = 320

        // ---------- HP / MP 状态条（右上角） ----------
        scene.playerStats = { hp: 100, maxHp: 100, mp: 100, maxMp: 100, money: 50 }

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

        // ---------- Buff/Debuff 显示状态初始化 ----------
        scene._activeEffects = []          // 后端传来的活跃状态列表
        scene._burnFlameGfx = null         // 火焰 Graphics 对象
        scene._burnLayersText = null       // 层数文字
        scene._burnTimerText = null        // 倒计时文字
        scene._burnLastTickMs = 0          // 后端 lastTickTime（毫秒）
        scene._burnNextTickMs = 0          // 本地计算的剩余毫秒

        // ---------- 货币显示（HP/MP 左侧，金黄色） ----------
        scene.moneyIcon = scene.add.text(455, 23, '$', { font: 'bold 18px Arial', fill: '#FFD700' }).setDepth(barDepth)
        scene.moneyText = scene.add.text(475, 24, '50', { font: 'bold 15px Arial', fill: '#FFD700' }).setDepth(barDepth)

        /**
         * 更新 HP/MP 状态条与货币显示，带平滑动画
         */
        scene.updatePlayerBars = function (hp, maxHp, mp, maxMp, money) {
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

          // 更新货币
          if (money !== undefined) {
            scene.moneyText.setText(String(money))
            scene.playerStats = { hp, maxHp, mp, maxMp, money }
          } else {
            scene.playerStats = { hp, maxHp, mp, maxMp }
          }
        }

        // ---------- 烧伤火焰显示区域（魔力条下方 到 房间方框上缘 之间） ----------
        // buff 栏区域：mpY + barH + 4 ~ roomTop（由 renderRoom 更新 roomTop）

        /**
         * 绘制一朵动态火焰与层数标注。
         * 火焰使用多层不规则多边形模拟，颜色从橙黄到红，有闪烁动画。
         * @param {Phaser.Scene} scene
         * @param {number} cx      火焰中心X
         * @param {number} cy      火焰中心Y（底部基点）
         * @param {number} size    火焰大小系数
         * @param {number} layers  烧伤层数
         * @param {number} timerSec 距下次结算秒数
         */
        scene.drawBurnFlame = function (cx, cy, size, layers, timerSec) {
          // 清理旧的火焰
          if (scene._burnFlameGfx) { try { scene._burnFlameGfx.destroy() } catch (e) {}; scene._burnFlameGfx = null }
          if (scene._burnLayersText) { try { scene._burnLayersText.destroy() } catch (e) {}; scene._burnLayersText = null }
          if (scene._burnTimerText) { try { scene._burnTimerText.destroy() } catch (e) {}; scene._burnTimerText = null }

          if (layers <= 0) return  // 无烧伤则不绘制

          const gfx = scene.add.graphics().setDepth(120)
          scene._burnFlameGfx = gfx

          const baseY = cy
          const flameH = 28 + size * 10
          const flameW = 14 + size * 5

          // 火焰主体：三层从内到外的渐变填充
          // 内层（白色/亮黄）
          gfx.fillStyle(0xFFFF88, 0.85)
          gfx.fillTriangle(
            cx, baseY - flameH * 0.85,
            cx - flameW * 0.15, baseY - flameH * 0.20,
            cx + flameW * 0.15, baseY - flameH * 0.20
          )
          gfx.fillTriangle(
            cx, baseY - flameH * 0.55,
            cx - flameW * 0.22, baseY - flameH * 0.05,
            cx + flameW * 0.22, baseY - flameH * 0.05
          )

          // 中层（橙色）
          gfx.fillStyle(0xFF8800, 0.75)
          gfx.fillTriangle(
            cx, baseY - flameH * 0.95,
            cx - flameW * 0.32, baseY,
            cx + flameW * 0.32, baseY
          )
          gfx.fillTriangle(
            cx, baseY - flameH * 0.65,
            cx - flameW * 0.38, baseY + 2,
            cx + flameW * 0.38, baseY + 2
          )

          // 外层（红色）
          gfx.fillStyle(0xFF3300, 0.55)
          gfx.fillTriangle(
            cx, baseY - flameH * 1.05,
            cx - flameW * 0.55, baseY + 4,
            cx + flameW * 0.55, baseY + 4
          )
          gfx.fillTriangle(
            cx, baseY - flameH * 0.45,
            cx - flameW * 0.6, baseY + 6,
            cx + flameW * 0.6, baseY + 6
          )

          // 外焰光晕
          gfx.fillStyle(0xFF6600, 0.2)
          gfx.fillEllipse(cx, baseY - flameH * 0.5, flameW * 1.3, flameH * 1.1)

          // 火星粒子点缀
          const rng = () => (Math.sin(Date.now() * 0.003 + cx) * 0.5 + 0.5)  // 伪随机用于静态点缀
          for (let i = 0; i < 5; i++) {
            const px = cx - flameW * 0.4 + (i / 4) * flameW * 0.8 + (Math.sin(i * 2.3 + cx * 0.7) * flameW * 0.15)
            const py = baseY - flameH * 0.1 - (i % 3) * flameH * 0.25 - Math.random() * flameH * 0.15
            gfx.fillStyle(0xFFDD44, 0.5 + Math.random() * 0.3)
            gfx.fillCircle(px, py, 1.2 + Math.random() * 1.5)
          }

          // 层数标注（火焰右下角）
          const layersText = scene.add.text(cx + flameW * 0.8, baseY + 2, String(layers), {
            font: 'bold 14px Arial',
            fill: '#FF6644',
            stroke: '#000000',
            strokeThickness: 3
          }).setDepth(121).setOrigin(0, 0.5)
          scene._burnLayersText = layersText

          // 倒计时标注（火焰下方）
          const timerText = scene.add.text(cx, baseY + 8, Math.ceil(timerSec) + 's', {
            font: '10px Arial',
            fill: '#FFAA66',
            stroke: '#000000',
            strokeThickness: 2
          }).setDepth(121).setOrigin(0.5, 0)
          scene._burnTimerText = timerText
        }

        /**
         * 根据后端 activeEffects 数据更新 buff 显示。
         * 位置：HP/MP 条下方，房间方框上缘之间的空隙。
         * @param {Array} activeEffects  后端传来的活跃状态列表
         */
        scene.updateBuffDisplay = function (activeEffects) {
          scene._activeEffects = activeEffects || []

          // 获取房间边界上缘（用于计算 buff 栏下边界）
          const rb = scene._roomBounds
          const roomTop = rb ? rb.top : 100   // 默认100（通常约75+）

          // buff 栏位置参数
          const barDepth = 100
          const mpY = 39          // MP 条 Y（与 create 中一致）
          const barH = 18          // MP 条高度
          const barX = 570
          const barW = 210

          // buff 栏范围：mpY + barH + 4 到 roomTop - 4
          const buffTop = mpY + barH + 10   // MP条下缘 + 间距
          const buffBottom = roomTop - 4
          const buffLeft = barX - 30         // HP/MP label 左边缘
          const buffRight = barX + barW       // HP/MP 条右边缘
          const buffMidX = (buffLeft + buffRight) / 2
          const buffMidY = (buffTop + buffBottom) / 2

          // 查找烧伤状态
          let burnEffect = null
          for (const eff of scene._activeEffects) {
            if (eff && eff.type === 'BURN') {
              burnEffect = eff
              break
            }
          }

          if (burnEffect && burnEffect.layers > 0) {
            const layers = burnEffect.layers || 0
            const nextTickIn = burnEffect.nextTickIn || 3000

            // 记录用于实时倒计时
            scene._burnLastTickMs = Date.now()
            scene._burnNextTickMs = nextTickIn

            const flameSize = Math.min(3, 1 + layers * 0.5)  // 层数越多火焰越大，上限3
            const timerSec = nextTickIn / 1000

            // 火焰置于 buff 栏中央偏左
            const flameCX = buffMidX - 40
            const flameCY = buffMidY + 6

            scene.drawBurnFlame(flameCX, flameCY, flameSize, layers, timerSec)
          } else {
            // 无烧伤：清理火焰显示
            if (scene._burnFlameGfx) { try { scene._burnFlameGfx.destroy() } catch (e) {}; scene._burnFlameGfx = null }
            if (scene._burnLayersText) { try { scene._burnLayersText.destroy() } catch (e) {}; scene._burnLayersText = null }
            if (scene._burnTimerText) { try { scene._burnTimerText.destroy() } catch (e) {}; scene._burnTimerText = null }
            scene._burnNextTickMs = 0
          }
        }

        scene.titleText = scene.add.text(20, 20, '', { font: '20px Arial', fill: '#ffffff' })
        scene.descText = scene.add.text(20, 50, '', { font: '14px Arial', fill: '#cccccc', wordWrap: { width: 760 } })

        scene.playerRadius = 10
        scene.player = scene.add.circle(400, 320, scene.playerRadius, 0x00aaff).setStrokeStyle(2, 0x000000)
        scene.playerLabel = scene.add.text(400 - 20, 320 + 18, 'You', { font: '12px Arial', fill: '#fff' })
        scene._roomBounds = { left: 16, top: 16, right: 800 - 16, bottom: 600 - 16 }

        scene.add.text(20, 560, 'WASD 移动 | J 攻击 | Shift+方向+J 突刺 | 空格 互动 | H 月光波', { font: '14px Arial', fill: '#cccccc' })

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
          const money = roomInfo.playerMoney !== undefined ? roomInfo.playerMoney : scene.playerStats.money
          try { scene.updatePlayerBars(hp, maxHp, mp, maxMp, money) } catch (e) {}

          // 清理上一帧的爆炸特效残留（避免切换房间时 crash）
          if (scene._prevExplodeGfxRefs) {
            for (const ref of scene._prevExplodeGfxRefs) {
              try { ref.destroy() } catch (e) {}
            }
            scene._prevExplodeGfxRefs = null
          }
          if (scene._explodeGfxRefs) {
            for (const ref of scene._explodeGfxRefs) {
              try { ref.destroy() } catch (e) {}
            }
            scene._explodeGfxRefs = []
          }

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
          // clear previous altar sprites and state
          scene.altarsGroup.clear(true, true)
          scene.altarsData = []
          scene.altarIndicators.forEach(ind => { try { ind.destroy() } catch (e) {} })
          scene.altarIndicators = []
          if (scene.altarGlowGraphics) { scene.altarGlowGraphics.clear() }
          if (scene.wisdomOverlay) { try { scene.wisdomOverlay.destroy() } catch (e) {}; scene.wisdomOverlay = null }
          // 清理商店覆盖层和NPC状态
          if (scene.shopMenuOverlay) { try { scene.shopMenuOverlay.destroy() } catch (e) {}; scene.shopMenuOverlay = null }
          if (scene.shopBuyOverlay) { try { scene.shopBuyOverlay.destroy() } catch (e) {}; scene.shopBuyOverlay = null }
          if (scene.shopNpcCircle) { try { scene.shopNpcCircle.destroy() } catch (e) {}; scene.shopNpcCircle = null }
          if (scene.shopNpcLabel) { try { scene.shopNpcLabel.destroy() } catch (e) {}; scene.shopNpcLabel = null }
          scene.shopNpcData = null
          scene.shopIndicators.forEach(ind => { try { ind.destroy() } catch (e) {} })
          scene.shopIndicators = []
          // remove exit buttons
          scene.exitButtons.forEach(b => b.destroy && b.destroy())
          scene.exitButtons = []

          scene.titleText.setText(roomInfo.name || '未知房间')
          scene.descText.setText(roomInfo.description || '')
          // 只在房间真正切换时才更换背景，避免攻击等操作导致背景闪变
          if (roomInfo.name && roomInfo.name !== scene._currentRoomName) {
            try { scene.setBgForRoom && scene.setBgForRoom(roomInfo) } catch (e) {}
            scene._currentRoomName = roomInfo.name
          }

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

          // ---------- 将背景缩放到房间矩形内并裁剪 ----------
          try { scene.fitBgToRoom && scene.fitBgToRoom(rectLeft, rectTop, roomW, roomH) } catch (e) {}
          scene.roomMaskShape.clear()
          scene.roomMaskShape.fillStyle(0xffffff)
          scene.roomMaskShape.fillRect(rectLeft, rectTop, roomW, roomH)
          if (!scene.roomMask) {
            // 首次创建遮罩并应用到两个背景层
            scene.roomMask = new Phaser.Display.Masks.GeometryMask(scene, scene.roomMaskShape)
            scene.bgFar.setMask(scene.roomMask)
            scene.bgNear.setMask(scene.roomMask)
          }
          // 遮罩 geometryMask 引用的是 scene.roomMaskShape 对象，更新其几何后遮罩自动生效

          // 房间矩形外填充深色背景
          if (!scene._darkBg) {
            scene._darkBg = scene.add.rectangle(400, 300, 800, 600, 0x111111).setDepth(-1).setOrigin(0.5)
          }

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
            scene.exitButtons.push(rect)
            scene.exitButtons.push(label)
            scene.doorRects.push({ dir, rect, label })
          })

          scene._roomBounds = { left: rectLeft, top: rectTop, right: rectLeft + roomW, bottom: rectTop + roomH }

          // 更新 Buff/Debuff 显示（基于后端 activeEffects）
          try { scene.updateBuffDisplay(roomInfo.activeEffects) } catch (e) {}

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
            scene.itemsGroup.add(circle)
            scene.itemsGroup.add(label)
            scene.itemsData.push({ name: item.name, x, y, circle, label, prompted: false, _removed: false })
            ix++
          })

          const monsters = roomInfo.monsters || []
          const explodingMonsters = roomInfo.explodingMonsters || []
          // vertical stacking layout to prevent label overlap
          const mCenterX = 300
          const mStartY = 160
          const mSpacingY = 120
          let mi = 0
          monsters.forEach(mon => {
            // 检查是否为爆炸中的怪物（来自后端 explodingMonsters 列表 或 怪物自身的exploding字段）
            const explodeInfo = explodingMonsters.find(em => em.name === mon.name)
            const isExploding = explodeInfo != null || mon.exploding === true
            // 已通知爆炸的怪物（countdown到0）仍在monsters列表，继续显示；已通知的视为不再自爆倒计时
            const isNotified = mon.explodeNotified === true
            const displayAsExploding = isExploding && !isNotified

            // restore AI-driven position if available, otherwise use default layout
            let x = mCenterX
            let y = mStartY + mi * mSpacingY
            const saved = prevMonsterPositions[mon.name]
            if (saved) {
              x = saved.x
              y = saved.y
            }
            // 爆炸中的火焰史莱姆用橙色，普通怪物用红色
            const isFlameSlime = mon.specialType === 'FLAME_SLIME'
            const isExplodingFlameSlime = isFlameSlime && displayAsExploding
            const circleColor = isExplodingFlameSlime ? 0xff6600 : 0xaa0000
            const circ = scene.add.circle(x, y, 20, circleColor).setStrokeStyle(2, 0x000000)
            // 爆炸中半透明
            if (isExplodingFlameSlime) {
              circ.setAlpha(0.6)
            }
            // 已通知但未清除（等待explode命令）的怪物：显示为灰色+准备自爆
            const isNotifiedFlameSlime = isFlameSlime && isExploding && isNotified
            if (isNotifiedFlameSlime) {
              circ.setFillStyle(0x666666)
              circ.setAlpha(0.4)
            }
            const labelText = isExplodingFlameSlime
              ? mon.name + ' (自爆中...)'
              : (isNotifiedFlameSlime ? mon.name + ' (准备自爆!)' : (mon.name + ' (HP:' + (mon.hp || 0) + ')'))
            const label = scene.add.text(x - 32, y + 24, labelText, { font: '14px Arial', fill: '#fff' })
            if (!scene.monstersGroup) scene.monstersGroup = scene.add.group()
            if (!scene.monstersData) scene.monstersData = []
            scene.monstersGroup.add(circ)
            scene.monstersGroup.add(label)
            scene.monstersData.push({
              name: mon.name, x, y, circ, label,
              hp: mon.hp, type: mon.type,
              defense: mon.defense || 0, magicResist: mon.magicResist || 0, speed: mon.speed || 100,
              specialType: mon.specialType || null,
              exploding: isExplodingFlameSlime,
              explodeNotified: mon.explodeNotified === true,
              explodeRange: (explodeInfo && explodeInfo.explodeRange) || mon.explodeRange || 80,
              explodeRemaining: (explodeInfo && explodeInfo.explodeRemaining) || 0
            })
            mi++
          })

          // ---------- 祭坛渲染（仅篝火房间） ----------
          const roomType = roomInfo.roomType || ''
          const altarsFromBackend = roomInfo.altars || []
          scene.altarUsedInRoom = roomInfo.altarUsed || false
          if (roomType === 'CAMPFIRE' && altarsFromBackend.length > 0) {
            // 三个祭坛从左至右排列在房间中央，中间祭坛位于 rectCenterX
            const altarSize = 36
            const altarSpacing = 100
            const altarY = rectCenterY
            // 顺序：HEAL, TRAIN, WISDOM (left to right)
            const altarOrder = ['HEAL', 'TRAIN', 'WISDOM']
            for (let ai = 0; ai < altarOrder.length; ai++) {
              const typeName = altarOrder[ai]
              const altarInfo = altarsFromBackend.find(a => a.type === typeName)
              if (!altarInfo) continue
              const isActivated = altarInfo.activated || false
              const isUsed = scene.altarUsedInRoom
              // x position: middle altar at center, others offset
              const ax = rectCenterX + (ai - 1) * (altarSize + altarSpacing)
              const ay = altarY
              // color: use backend color, gray if roomUsed but not this activated one
              let color = altarInfo.color || 0x888888
              if (isUsed && !isActivated) {
                color = 0x666666 // gray for unused altars
              }
              const rect = scene.add.rectangle(ax, ay, altarSize, altarSize, color).setStrokeStyle(2, isActivated ? 0xffffff : 0x444444)
              const typeLabel = scene.add.text(ax, ay + altarSize / 2 + 8, altarInfo.displayName || typeName, {
                font: '11px Arial', fill: '#ffffff'
              }).setOrigin(0.5, 0)
              if (!scene.altarsGroup) scene.altarsGroup = scene.add.group()
              scene.altarsGroup.add(rect)
              scene.altarsGroup.add(typeLabel)
              if (!scene.altarsData) scene.altarsData = []
              scene.altarsData.push({
                type: typeName,
                displayName: altarInfo.displayName,
                x: ax, y: ay,
                size: altarSize,
                rect, label: typeLabel,
                activated: isActivated,
                color: color,
                originalColor: altarInfo.color || 0x888888
              })
            }
          }

          // ---------- NPC 商人渲染（仅商店房间） ----------
          const isShop = roomInfo.isShop || roomType === 'SHOP'
          scene.shopNpcCircle = null
          scene.shopNpcData = null
          if (isShop) {
            const npcRadius = 15  // 比玩家(10)略大
            const shopItems = roomInfo.shopItems || []
            scene.shopNpcData = {
              x: rectCenterX,
              y: rectCenterY,
              radius: npcRadius,
              shopInitialized: roomInfo.shopInitialized || false,
              shopItems: shopItems
            }
            const npc = scene.add.circle(rectCenterX, rectCenterY, npcRadius, 0xFFD700).setStrokeStyle(2, 0x000000)
            const npcLabel = scene.add.text(rectCenterX - 24, rectCenterY + npcRadius + 6, '商人', {
              font: '12px Arial', fill: '#FFD700'
            })
            scene.shopNpcCircle = npc
            scene.shopNpcLabel = npcLabel
          }

          // ---------- 小地图更新通知 ----------
          try {
            currentRoomName = roomInfo.name || ''
            window.dispatchEvent(new CustomEvent('minimap:update', { detail: { roomName: currentRoomName } }))
          } catch (e) {}
        }

        // ---------- 祭坛光芒特效和博学选项浮层 ----------
        scene.spawnAltarGlow = function (cx, cy, size, color, duration) {
          const glowGfx = scene.add.graphics()
          glowGfx.setDepth(5)
          const startTime = Date.now()
          const drawGlow = () => {
            const elapsed = Date.now() - startTime
            const progress = Math.min(1, elapsed / duration)
            const alpha = 1 - progress
            glowGfx.clear()
            for (let r = 0; r < 4; r++) {
              const radius = (size / 2 + 10) + r * 10 + Math.sin(elapsed * 0.005 + r) * 5
              const ringAlpha = alpha * (0.3 - r * 0.05)
              glowGfx.lineStyle(3 - r * 0.5, color, Math.max(0, ringAlpha))
              glowGfx.strokeCircle(cx, cy, radius)
            }
            glowGfx.fillStyle(color, alpha * 0.15)
            glowGfx.fillCircle(cx, cy, size / 2 + 20)
            if (progress < 1) {
              requestAnimationFrame(drawGlow)
            } else {
              glowGfx.destroy()
            }
          }
          drawGlow()
        }

        scene.showWisdomOverlay = function () {
          if (scene.wisdomOverlay) { try { scene.wisdomOverlay.destroy() } catch (e) {} }
          const overlay = scene.add.container(0, 0).setDepth(200)
          const bg = scene.add.rectangle(400, 300, 800, 600, 0x000000, 0.5)
          bg.setInteractive()
          const title = scene.add.text(400, 200, '博学祭坛 — 请选择一项增益', {
            font: 'bold 22px Arial', fill: '#4488ff'
          }).setOrigin(0.5)
          const options = [
            { label: '选项一（暂未实现）', y: 260 },
            { label: '选项二（暂未实现）', y: 310 },
            { label: '选项三（暂未实现）', y: 360 }
          ]
          const optionTexts = []
          options.forEach(opt => {
            const txt = scene.add.text(400, opt.y, opt.label, {
              font: '18px Arial', fill: '#aaaaaa', backgroundColor: '#222222',
              padding: { x: 20, y: 8 }
            }).setOrigin(0.5).setInteractive({ useHandCursor: true })
            txt.on('pointerover', () => txt.setStyle({ fill: '#ffffff', backgroundColor: '#444444' }))
            txt.on('pointerout', () => txt.setStyle({ fill: '#aaaaaa', backgroundColor: '#222222' }))
            txt.on('pointerdown', () => {
              try { scene.wisdomOverlay.destroy() } catch (e) {}
              scene.wisdomOverlay = null
            })
            optionTexts.push(txt)
          })
          const hint = scene.add.text(400, 440, '点击选项后浮层关闭（增益系统待实现）', {
            font: '13px Arial', fill: '#888888'
          }).setOrigin(0.5)
          overlay.add([bg, title, ...optionTexts, hint])
          scene.wisdomOverlay = overlay
        }

        // ---------- 商店菜单浮层（购物/售卖选项） ----------
        scene.showShopMenu = function () {
          if (scene.shopMenuOverlay) { try { scene.shopMenuOverlay.destroy() } catch (e) {} }
          if (scene.shopBuyOverlay) { try { scene.shopBuyOverlay.destroy() } catch (e) {}; scene.shopBuyOverlay = null }
          const overlay = scene.add.container(0, 0).setDepth(200)
          const bg = scene.add.rectangle(400, 300, 800, 600, 0x000000, 0.5)
          bg.setInteractive()
          const title = scene.add.text(400, 200, '商人', {
            font: 'bold 24px Arial', fill: '#FFD700'
          }).setOrigin(0.5)
          const hint = scene.add.text(400, 245, '你想要做什么？', {
            font: '16px Arial', fill: '#cccccc'
          }).setOrigin(0.5)

          // 购物选项（左侧）
          const buyBtn = scene.add.text(310, 320, '🛒 购物', {
            font: 'bold 22px Arial', fill: '#44cc44', backgroundColor: '#222222',
            padding: { x: 30, y: 10 }
          }).setOrigin(0.5).setInteractive({ useHandCursor: true })
          buyBtn.on('pointerover', () => buyBtn.setStyle({ fill: '#ffffff', backgroundColor: '#445544' }))
          buyBtn.on('pointerout', () => buyBtn.setStyle({ fill: '#44cc44', backgroundColor: '#222222' }))
          buyBtn.on('pointerdown', () => {
            try { scene.shopMenuOverlay.destroy() } catch (e) {}
            scene.shopMenuOverlay = null
            scene.showShopBuy()
          })

          // 售卖选项（右侧，与购物同一水平线）
          const sellBtn = scene.add.text(490, 320, '💰 售卖', {
            font: 'bold 22px Arial', fill: '#FFD700', backgroundColor: '#222222',
            padding: { x: 30, y: 10 }
          }).setOrigin(0.5).setInteractive({ useHandCursor: true })
          sellBtn.on('pointerover', () => sellBtn.setStyle({ fill: '#ffffff', backgroundColor: '#445544' }))
          sellBtn.on('pointerout', () => sellBtn.setStyle({ fill: '#FFD700', backgroundColor: '#222222' }))
          sellBtn.on('pointerdown', () => {
            try { scene.shopMenuOverlay.destroy() } catch (e) {}
            scene.shopMenuOverlay = null
            // 售卖功能仅显示，不做任何操作
          })

          // 关闭按钮
          const closeBtn = scene.add.text(400, 400, '关闭', {
            font: '18px Arial', fill: '#ff6666', backgroundColor: '#222222',
            padding: { x: 20, y: 6 }
          }).setOrigin(0.5).setInteractive({ useHandCursor: true })
          closeBtn.on('pointerover', () => closeBtn.setStyle({ fill: '#ffffff', backgroundColor: '#664444' }))
          closeBtn.on('pointerout', () => closeBtn.setStyle({ fill: '#ff6666', backgroundColor: '#222222' }))
          closeBtn.on('pointerdown', () => {
            try { scene.shopMenuOverlay.destroy() } catch (e) {}
            scene.shopMenuOverlay = null
          })

          overlay.add([bg, title, hint, buyBtn, sellBtn, closeBtn])
          scene.shopMenuOverlay = overlay
        }

        // ---------- 商店购买界面浮层 ----------
        scene.showShopBuy = function () {
          if (scene.shopBuyOverlay) { try { scene.shopBuyOverlay.destroy() } catch (e) {} }
          const overlay = scene.add.container(0, 0).setDepth(200)
          const bg = scene.add.rectangle(400, 300, 800, 600, 0x000000, 0.6)
          bg.setInteractive()
          const title = scene.add.text(400, 130, '购物 — 点击商品购买', {
            font: 'bold 22px Arial', fill: '#FFD700'
          }).setOrigin(0.5)

          // 获取商店数据
          const shopItems = scene.shopNpcData ? (scene.shopNpcData.shopItems || []) : []

          // 每个商品的布局：横向2行3列
          const gridCols = 3
          const gridRows = 2
          const itemW = 130   // 每个商品占用宽度
          const itemH = 120   // 每个商品占用高度
          const gridStartX = 400 - (gridCols - 1) * itemW / 2
          const gridStartY = 220
          const iconSize = 48  // 灰色方框大小

          const itemElements = [] // 用于后续刷新

          for (let i = 0; i < shopItems.length; i++) {
            const item = shopItems[i]
            const col = i % gridCols
            const row = Math.floor(i / gridCols)
            const cx = gridStartX + col * itemW
            const cy = gridStartY + row * itemH

            if (item.sold) {
              // 已售出：不显示任何内容（空出位置）
              continue
            }

            // 灰色方框图标
            const icon = scene.add.rectangle(cx, cy, iconSize, iconSize, 0x888888).setStrokeStyle(2, 0xaaaaaa)
            // 商品名称
            const nameText = scene.add.text(cx, cy + iconSize / 2 + 8, item.name, {
              font: '13px Arial', fill: '#ffffff'
            }).setOrigin(0.5, 0)
            // 价格
            const priceText = scene.add.text(cx, cy + iconSize / 2 + 28, '$' + item.price, {
              font: 'bold 14px Arial', fill: '#FFD700'
            }).setOrigin(0.5, 0)

            // 整个商品区域可点击
            const hitArea = scene.add.rectangle(cx, cy, itemW - 10, itemH - 10, 0x000000, 0).setInteractive({ useHandCursor: true })
            hitArea.on('pointerover', () => {
              icon.setStrokeStyle(3, 0xffffff)
            })
            hitArea.on('pointerout', () => {
              icon.setStrokeStyle(2, 0xaaaaaa)
            })
            hitArea.on('pointerdown', async () => {
              // 购买商品
              try {
                const res = await fetch('/api/command', {
                  method: 'POST', headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ command: 'shop buy ' + item.name })
                })
                const j = await res.json()
                emit('update', j)

                if (j && j.status === 'success') {
                  // 购买成功：销毁旧浮层，更新数据，重新渲染
                  try { scene.shopBuyOverlay.destroy() } catch (e) {}
                  scene.shopBuyOverlay = null

                  // 更新本地商店数据
                  if (j.data && j.data.shopItems) {
                    scene.shopNpcData.shopItems = j.data.shopItems
                    scene.shopNpcData.shopInitialized = j.data.shopInitialized
                  }
                  // 更新玩家货币显示
                  if (j.data) {
                    const hp = j.data.playerHp !== undefined ? j.data.playerHp : scene.playerStats.hp
                    const maxHp = j.data.playerMaxHp !== undefined ? j.data.playerMaxHp : scene.playerStats.maxHp
                    const mp = j.data.playerMp !== undefined ? j.data.playerMp : scene.playerStats.mp
                    const maxMp = j.data.playerMaxMp !== undefined ? j.data.playerMaxMp : scene.playerStats.maxMp
                    const money = j.data.playerMoney !== undefined ? j.data.playerMoney : scene.playerStats.money
                    try { scene.updatePlayerBars(hp, maxHp, mp, maxMp, money) } catch (e) {}
                  }

                  // 重新显示购物界面
                  scene.showShopBuy()
                } else if (j && j.status === 'error') {
                  // 显示错误提示
                  const errText = scene.add.text(400, 500, j.message || '购买失败', {
                    font: '16px Arial', fill: '#ff4444', backgroundColor: '#330000',
                    padding: { x: 10, y: 4 }
                  }).setOrigin(0.5).setDepth(201)
                  scene.tweens.add({
                    targets: errText, alpha: 0, duration: 2000, delay: 1500,
                    onComplete: () => { try { errText.destroy() } catch (e) {} }
                  })
                }
              } catch (e) {
                emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
              }
            })

            // 存储引用以便清理
            itemElements.push(icon, nameText, priceText, hitArea)
          }

          // "结束购物"按钮
          const endBtn = scene.add.text(400, 510, '结束购物', {
            font: 'bold 20px Arial', fill: '#ff6666', backgroundColor: '#222222',
            padding: { x: 24, y: 8 }
          }).setOrigin(0.5).setInteractive({ useHandCursor: true })
          endBtn.on('pointerover', () => endBtn.setStyle({ fill: '#ffffff', backgroundColor: '#664444' }))
          endBtn.on('pointerout', () => endBtn.setStyle({ fill: '#ff6666', backgroundColor: '#222222' }))
          endBtn.on('pointerdown', () => {
            try { scene.shopBuyOverlay.destroy() } catch (e) {}
            scene.shopBuyOverlay = null
          })

          overlay.add([
            bg, title, ...itemElements, endBtn
          ])
          scene.shopBuyOverlay = overlay
        }

        // 键盘控制
        scene.keys = scene.input.keyboard.addKeys('W,A,S,D,E,SHIFT,J,SPACE,H')
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
        // 月光波蓄力状态
        scene._waveCharging = { active: false, startTime: 0, chargeBarGfx: null, charged: false, directionGfx: null }
        scene._waveProjectiles = []
        scene._wavePendingSend = false  // prevent duplicate sends

        // ---------- 月光波发射与特效 ----------
        scene.spawnWaveProjectile = function (startX, startY, angle, rb) {
          const speed = 420  // pixels per second
          const maxBounces = 2
          const proj = {
            x: startX, y: startY,
            angle: angle,
            speed: speed,
            bounces: 0,
            maxBounces: maxBounces,
            alive: true,
            radius: 27,
            rotation: 0,      // visual spin
            trailPositions: [] // for trail effect
          }
          scene._waveProjectiles.push(proj)
          return proj
        }

        scene.drawWaveProjectile = function () {
          // draw all active projectiles
          const gfx = scene.add.graphics()
          gfx.setDepth(200)
          for (const proj of scene._waveProjectiles) {
            if (!proj.alive) continue
            // draw trail
            if (proj.trailPositions.length > 1) {
              for (let i = 1; i < proj.trailPositions.length; i++) {
                const t = proj.trailPositions[i]
                const alpha = (i / proj.trailPositions.length) * 0.3
                gfx.fillStyle(0xFFD700, alpha)
                gfx.fillCircle(t.x, t.y, proj.radius * 0.6)
              }
            }
            // draw main crescent shape
            gfx.fillStyle(0xFFD700, 1)
            // outer glow
            gfx.fillStyle(0xFFEE88, 0.25)
            gfx.fillCircle(proj.x, proj.y, proj.radius * 1.5)
            // core
            gfx.fillStyle(0xFFD700, 0.9)
            gfx.fillCircle(proj.x, proj.y, proj.radius)
            // bright center
            gfx.fillStyle(0xFFFFCC, 0.7)
            gfx.fillCircle(proj.x, proj.y, proj.radius * 0.55)
            // crescent arc lines
            gfx.lineStyle(3, 0xFFAA00, 0.8)
            const cr = proj.radius * 0.9
            const startA = proj.rotation - 1.2
            const endA = proj.rotation + 1.2
            gfx.beginPath()
            for (let a = startA; a <= endA; a += 0.15) {
              const px = proj.x + Math.cos(a) * cr
              const py = proj.y + Math.sin(a) * cr
              if (a === startA) gfx.moveTo(px, py)
              else gfx.lineTo(px, py)
            }
            gfx.strokePath()
            // inner highlight arc
            gfx.lineStyle(2, 0xFFFFFF, 0.5)
            const ir = proj.radius * 0.5
            gfx.beginPath()
            for (let a = startA; a <= endA; a += 0.2) {
              const px = proj.x + Math.cos(a) * ir
              const py = proj.y + Math.sin(a) * ir
              if (a === startA) gfx.moveTo(px, py)
              else gfx.lineTo(px, py)
            }
            gfx.strokePath()
          }
          // auto-destroy after short duration
          scene.time.delayedCall(50, () => {
            try { gfx.destroy() } catch (e) {}
          })
        }

        scene.lastDoorEntered = null
        scene.doorRects = []
        scene.itemsData = []
        // altar state
        scene.altarsData = []
        scene.altarsGroup = scene.add.group()
        scene.altarGlowGraphics = scene.add.graphics()
        scene.altarIndicators = []       // white triangle indicators
        scene.wisdomOverlay = null       // 博学祭坛选项浮层
        scene.altarUsedInRoom = false    // 当前房间祭坛是否已被使用
        // shop state
        scene.shopNpcCircle = null       // NPC 商人圆点
        scene.shopNpcLabel = null        // NPC 商人标签
        scene.shopNpcData = null         // { x, y, radius, shopInitialized, shopItems }
        scene.shopIndicators = []        // 商店白色倒三角指示器
        scene.shopMenuOverlay = null     // 购物/售卖选项浮层
        scene.shopBuyOverlay = null      // 购买界面浮层
        // monster AI state: cooldown tracking (ms timestamp per monster name)
        scene.monsterAttackCooldowns = {}  // { monName: lastAttackTime }

        // 背包暂停状态
        scene._backpackPaused = false
        window.addEventListener('backpack:toggle', function(e) {
          scene._backpackPaused = e.detail.visible
        })

        // 监听 game:update 事件，背包打开时也能即时更新 HP/MP 条
        window.__zuul_game_update_handler = function(e) {
          const j = e.detail
          if (j && j.data) {
            const hp = j.data.playerHp !== undefined ? j.data.playerHp : scene.playerStats.hp
            const maxHp = j.data.playerMaxHp !== undefined ? j.data.playerMaxHp : scene.playerStats.maxHp
            const mp = j.data.playerMp !== undefined ? j.data.playerMp : scene.playerStats.mp
            const maxMp = j.data.playerMaxMp !== undefined ? j.data.playerMaxMp : scene.playerStats.maxMp
            const money = j.data.playerMoney !== undefined ? j.data.playerMoney : scene.playerStats.money
            try { scene.updatePlayerBars(hp, maxHp, mp, maxMp, money) } catch (e) {}
            // 更新 Buff/Debuff 显示
            if (j.data.activeEffects) {
              try { scene.updateBuffDisplay(j.data.activeEffects) } catch (e) {}
            }
          }
          // 如果背包未打开且响应包含房间数据，正常渲染房间
          if (!scene._backpackPaused && j && j.data && j.data.name) {
            try { scene.renderRoom(j.data) } catch (e) {}
          }
        }
        window.addEventListener('game:update', window.__zuul_game_update_handler)

        // update 循环
        this.sys.events.on('update', function (time, delta) {
          const dt = delta / 1000
          const rb = scene._roomBounds || { left: 16, top: 16, right: 800 - 16, bottom: 600 - 16 }

          // block all movement and actions when game is over
          if (scene.gameOver) return

          // block all movement and actions when backpack is open
          if (scene._backpackPaused) return

          // ---------- 烧伤倒计时实时更新 ----------
          if (scene._burnNextTickMs > 0) {
            const nowBurn = Date.now()
            const elapsed = nowBurn - scene._burnLastTickMs
            const remaining = Math.max(0, scene._burnNextTickMs - elapsed)
            scene._burnNextTickMs = remaining
            scene._burnLastTickMs = nowBurn
            // 更新倒计时文字
            if (scene._burnTimerText && scene._burnTimerText.active) {
              const timerSec = remaining / 1000
              scene._burnTimerText.setText(Math.ceil(timerSec) + 's')
              // 小于3秒时变红色闪烁
              if (timerSec < 3) {
                const flash = Math.sin(nowBurn * 0.01) * 0.3 + 0.7
                scene._burnTimerText.setAlpha(flash)
                scene._burnTimerText.setColor('#FF4444')
              } else {
                scene._burnTimerText.setAlpha(1)
                scene._burnTimerText.setColor('#FFAA66')
              }
            }
            if (remaining <= 0) {
              // 倒计时归零：清理显示（下次后端数据到达时会重新评估）
              scene._burnNextTickMs = 0
            }
          }

          // ---------- 定期轮询后端驱动爆炸计时与全局状态更新 ----------
          // 每500ms轮询一次GET /api/game，驱动tickExplosions（自爆倒计时）、烧伤结算等
          if (!scene._nextPollTime) scene._nextPollTime = 0
          if (time > scene._nextPollTime && !scene._backpackPaused && !scene.gameOver) {
            scene._nextPollTime = time + 500
            ;(async () => {
              try {
                const res = await fetch('/api/game')
                const j = await res.json()
                if (j && j.data) {
                  // ---- 处理已触发的爆炸倒计时（倒计时到，后端标记为 explodeTriggered） ----
                  if (j.data.explodeTriggeredMonsters) {
                    for (const tmon of j.data.explodeTriggeredMonsters) {
                      // 从当前 monstersData 找到对应怪物的前端位置
                      const findMon = scene.monstersData.find(m => m && m.name === tmon.name)
                      const mx = findMon ? findMon.x : (tmon.x || 400)
                      const my = findMon ? findMon.y : (tmon.y || 300)
                      const range = tmon.explodeRange || 80
                      const px = scene.player.x
                      const py = scene.player.y
                      const dist = Math.sqrt((px - mx) * (px - mx) + (py - my) * (py - my))
                      // 判断玩家是否在爆炸范围内，决定是否附加 _nodmg
                      const inRange = dist <= range + (scene.playerRadius || 10)
                      const cmd = inRange ? ('explode ' + tmon.name) : ('explode ' + tmon.name + ' _nodmg')
                      ;(async () => {
                        try {
                          const eres = await fetch('/api/command', {
                            method: 'POST', headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ command: cmd })
                          })
                          const ej = await eres.json()
                          emit('update', ej)
                          if (ej && ej.data) scene.renderRoom(ej.data)
                          if (ej && ej.status === 'error' && ej.message && ej.message.includes('游戏结束')) {
                            scene.showGameOver()
                          }
                          // 仅范围内显示自爆特效
                          if (inRange) {
                            const flash = scene.add.rectangle(400, 300, 800, 600, 0xFF2200, 0.25).setDepth(500)
                            scene.tweens.add({
                              targets: flash, alpha: 0, duration: 500,
                              onComplete: () => { try { flash.destroy() } catch (e) {} }
                            })
                            if (scene.cameras && scene.cameras.main) {
                              scene.cameras.main.shake(200, 0.012)
                            }
                          }
                        } catch (e) {}
                      })()
                    }
                  }
                  // 更新HP/MP条
                  const hp = j.data.playerHp !== undefined ? j.data.playerHp : scene.playerStats.hp
                  const maxHp = j.data.playerMaxHp !== undefined ? j.data.playerMaxHp : scene.playerStats.maxHp
                  const mp = j.data.playerMp !== undefined ? j.data.playerMp : scene.playerStats.mp
                  const maxMp = j.data.playerMaxMp !== undefined ? j.data.playerMaxMp : scene.playerStats.maxMp
                  const money = j.data.playerMoney !== undefined ? j.data.playerMoney : scene.playerStats.money
                  try { scene.updatePlayerBars(hp, maxHp, mp, maxMp, money) } catch (e) {}
                  // 更新Buff/Debuff显示
                  if (j.data.activeEffects) {
                    try { scene.updateBuffDisplay(j.data.activeEffects) } catch (e) {}
                  }
                  // 如果有爆炸中的怪物数据，更新 renderRoom（保留AI位置）
                  if (j.data.explodingMonsters) {
                    try { scene.renderRoom(j.data) } catch (e) {}
                  }
                  // 检查游戏结束
                  if (j.data.gameOver) {
                    try { scene.showGameOver() } catch (e) {}
                  }
                  // 通知小地图
                  try {
                    currentRoomName = j.data.name || ''
                    if (j.data.name) {
                      window.dispatchEvent(new CustomEvent('minimap:update', { detail: { roomName: currentRoomName } }))
                    }
                  } catch (e) {}
                }
              } catch (e) {
                // 轮询失败静默处理
              }
            })()
          }

          // ---------- H 键月光波蓄力系统 ----------
          const CHARGE_DURATION = 2000  // 2 seconds to full charge
          const WAVE_MP_COST = 30
          const nowMs = Date.now()

          // check if player has enough MP
          const hasEnoughMp = scene.playerStats.mp >= WAVE_MP_COST

          // ---- 开始蓄力 ----
          try {
            if (scene.keys.H && scene.keys.H.isDown && hasEnoughMp && !scene._waveCharging.active && !scene._wavePendingSend && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.wisdomOverlay) {
              scene._waveCharging.active = true
              scene._waveCharging.startTime = nowMs
              scene._waveCharging.charged = false
            }
          } catch (e) {}

          // ---- 发射函数 ----
          const fireWave = () => {
            if (scene._wavePendingSend) return
            scene._wavePendingSend = true
            // clear UI
            try {
              if (scene._waveCharging.chargeBarGfx) { scene._waveCharging.chargeBarGfx.destroy(); scene._waveCharging.chargeBarGfx = null }
              if (scene._waveCharging.directionGfx) { scene._waveCharging.directionGfx.destroy(); scene._waveCharging.directionGfx = null }
            } catch (e) {}
            scene._waveCharging.active = false
            scene._waveCharging.charged = false

            const rbWave = scene._roomBounds || { left: 16, top: 16, right: 784, bottom: 584 }
            const proj = scene.spawnWaveProjectile(scene.player.x, scene.player.y, scene.facingAngle, rbWave)
            proj.hitMonsters = new Set()

            ;(async () => {
              try {
                const res = await fetch('/api/command', {
                  method: 'POST', headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ command: 'wave _init' })
                })
                const j = await res.json()
                emit('update', j)
                if (j && j.data) scene.renderRoom(j.data)
              } catch (e) {
                emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
              }
              scene._wavePendingSend = false
            })()

            if (scene.cameras && scene.cameras.main) {
              scene.cameras.main.shake(180, 0.008)
            }
            const flash = scene.add.rectangle(400, 300, 800, 600, 0xFFFFCC, 0.2).setDepth(500)
            scene.tweens.add({
              targets: flash, alpha: 0, duration: 300,
              onComplete: () => { try { flash.destroy() } catch (e) {} }
            })
          }

          // ---- 蓄力 / 瞄准状态处理 ----
          if (scene._waveCharging.active) {
            try {
              const hDown = scene.keys.H && scene.keys.H.isDown
              if (!hDown) {
                // H 松开
                if (scene._waveCharging.charged) {
                  fireWave()   // 满蓄力松开 → 发射
                } else {
                  // 未满蓄力松开 → 取消
                  scene._waveCharging.active = false
                  if (scene._waveCharging.chargeBarGfx) {
                    try { scene._waveCharging.chargeBarGfx.destroy() } catch (e) {}
                    scene._waveCharging.chargeBarGfx = null
                  }
                }
              } else if (!scene._waveCharging.charged) {
                // 蓄力中（未满）
                const elapsed = nowMs - scene._waveCharging.startTime
                const progress = Math.min(1, elapsed / CHARGE_DURATION)

                if (!scene._waveCharging.chargeBarGfx) {
                  scene._waveCharging.chargeBarGfx = scene.add.graphics().setDepth(150)
                }
                const gfx = scene._waveCharging.chargeBarGfx
                gfx.clear()
                const barWidth = 50, barHeight = 8
                const barX = scene.player.x - barWidth / 2
                const barY = scene.player.y + 30
                gfx.fillStyle(0x222222, 0.8)
                gfx.fillRect(barX, barY, barWidth, barHeight)
                gfx.lineStyle(1, 0x44aa44, 0.9)
                gfx.strokeRect(barX, barY, barWidth, barHeight)
                const r = Math.floor(30 + progress * 80)
                const g = Math.floor(180 + progress * 75)
                const b = Math.floor(30)
                const fillColor = (r << 16) | (g << 8) | b
                gfx.fillStyle(fillColor, 0.9)
                gfx.fillRect(barX, barY, barWidth * progress, barHeight)
                if (progress >= 1) {
                  scene._waveCharging.charged = true
                }
              } else {
                // 已满蓄力，保持满蓄力条 + 绘制方向指示器
                if (!scene._waveCharging.chargeBarGfx) {
                  scene._waveCharging.chargeBarGfx = scene.add.graphics().setDepth(150)
                }
                const gfx = scene._waveCharging.chargeBarGfx
                gfx.clear()
                const barWidth = 50, barHeight = 8
                const barX = scene.player.x - barWidth / 2
                const barY = scene.player.y + 30
                gfx.fillStyle(0x222222, 0.8)
                gfx.fillRect(barX, barY, barWidth, barHeight)
                gfx.lineStyle(2, 0x00ff44, 1.0)
                gfx.strokeRect(barX, barY, barWidth, barHeight)
                gfx.fillStyle(0x00ff44, 0.4 + 0.4 * Math.sin(nowMs * 0.01))
                gfx.fillRect(barX, barY, barWidth, barHeight)

                // 方向指示器：白色小三角形（基部贴在圆点外缘，尖端向外）
                if (!scene._waveCharging.directionGfx) {
                  scene._waveCharging.directionGfx = scene.add.graphics().setDepth(151)
                }
                const dgfx = scene._waveCharging.directionGfx
                dgfx.clear()
                const prInd = (scene.playerRadius || 10)
                const triTipDist = prInd + 8       // 尖端在圆点外 8px
                const triBaseDist = prInd + 5       // 基部与圆点外缘留出间距
                const triHalfWidth = 4              // 基部半宽
                const perpLeft = scene.facingAngle + Math.PI / 2
                const perpRight = scene.facingAngle - Math.PI / 2
                const triTipX = scene.player.x + Math.cos(scene.facingAngle) * triTipDist
                const triTipY = scene.player.y + Math.sin(scene.facingAngle) * triTipDist
                const baseLX = scene.player.x + Math.cos(scene.facingAngle) * triBaseDist + Math.cos(perpLeft) * triHalfWidth
                const baseLY = scene.player.y + Math.sin(scene.facingAngle) * triBaseDist + Math.sin(perpLeft) * triHalfWidth
                const baseRX = scene.player.x + Math.cos(scene.facingAngle) * triBaseDist + Math.cos(perpRight) * triHalfWidth
                const baseRY = scene.player.y + Math.sin(scene.facingAngle) * triBaseDist + Math.sin(perpRight) * triHalfWidth
                dgfx.fillStyle(0xffffff, 0.9 + 0.1 * Math.sin(nowMs * 0.005))
                dgfx.fillTriangle(triTipX, triTipY, baseLX, baseLY, baseRX, baseRY)
              }
            } catch (e) {}
          }

          // movement by WASD
          const isCharging = scene._waveCharging.active && !scene._waveCharging.charged  // 蓄力中（未满）禁止移动
          const isAiming = scene._waveCharging.active && scene._waveCharging.charged      // 满蓄力瞄准中：允许转向
          if (!isCharging) {
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
            if (!isAiming) {
              let speed = scene.baseMoveSpeed
              try {
                if (scene.keys.SHIFT && scene.keys.SHIFT.isDown) speed = speed * 2
              } catch (e) {}
              scene.player.x += vx * speed * dt
              scene.player.y += vy * speed * dt
            }
          }
          }

          // ---------- 怪物 AI：索敌 + 追击 + 攻击 ----------
          const MONSTER_DETECT_RANGE = 280   // 发现玩家的距离
          const MONSTER_ATTACK_RANGE = 45    // 攻击距离
          const MONSTER_ATTACK_COOLDOWN = 1500 // 攻击间隔 (ms)
          const pr = scene.playerRadius || 10

          const now = Date.now()

          // 收集当前爆炸中的火焰史莱姆渲染信息
          const explodingFlameSlimes = []

          for (const mon of scene.monstersData) {
            if (!mon || !mon.circ) continue

            // 爆炸中或已通知的怪物：不移动、不攻击
            if (mon.exploding || mon.explodeNotified) {
              // 仅未通知的收集爆炸视觉信息
              if (mon.exploding) {
                explodingFlameSlimes.push(mon)
              }
              continue
            }

            const dx = scene.player.x - mon.x
            const dy = scene.player.y - mon.y
            const dist = Math.sqrt(dx * dx + dy * dy)

            if (dist <= MONSTER_ATTACK_RANGE) {
              // 在攻击范围内 → 攻击玩家（每个怪物独立冷却，无全局锁）
              if (!scene.monsterAttackCooldowns[mon.name] || now - scene.monsterAttackCooldowns[mon.name] >= MONSTER_ATTACK_COOLDOWN) {
                scene.monsterAttackCooldowns[mon.name] = now
                ;(async () => {
                  try {
                    const res = await fetch('/api/command', {
                      method: 'POST', headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ command: 'monsterattack ' + mon.name })
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
                })()
              }
            } else if (dist <= MONSTER_DETECT_RANGE || mon.type === 2) {
              // 在索敌范围内 → 向玩家移动
              // Boss（type===2）不受距离限制，全图索敌
              const speed = mon.speed || 100
              const norm = Math.max(1, dist)
              const mvx = (dx / norm) * speed * dt
              const mvy = (dy / norm) * speed * dt
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

          // ---- 火焰史莱姆自爆范围渲染 ----
          // 绘制爆炸中的火焰史莱姆的半透明红圈（自爆范围指示器）
          if (explodingFlameSlimes.length > 0) {
            const explodeGfx = scene.add.graphics()
            explodeGfx.setDepth(90)
            for (const mon of explodingFlameSlimes) {
              const range = mon.explodeRange || 80
              const remaining = mon.explodeRemaining || 0
              const remainingSec = remaining / 1000
              // 半透明红色填充圆（表示即将自爆的范围）
              const pulseAlpha = 0.12 + 0.06 * Math.sin(now * 0.008)
              explodeGfx.fillStyle(0xFF2200, pulseAlpha)
              explodeGfx.fillCircle(mon.x, mon.y, range)
              // 红色虚线边框
              explodeGfx.lineStyle(2, 0xFF3300, 0.4 + 0.2 * Math.sin(now * 0.01))
              // 绘制虚线圆（用弧段模拟）
              const dashCount = 24
              for (let d = 0; d < dashCount; d += 2) {
                const startAngle = (d / dashCount) * Math.PI * 2
                const endAngle = ((d + 1) / dashCount) * Math.PI * 2
                explodeGfx.beginPath()
                explodeGfx.arc(mon.x, mon.y, range, startAngle, endAngle, false)
                explodeGfx.strokePath()
              }
              // 倒计时文字
              const countdownText = scene.add.text(mon.x, mon.y - range - 12,
                remainingSec <= 1 ? '!!! ' + remainingSec.toFixed(1) + 's !!!' : remainingSec.toFixed(1) + 's',
                {
                  font: 'bold 14px Arial',
                  fill: remainingSec <= 1 ? '#FF2222' : '#FF6644',
                  stroke: '#000000',
                  strokeThickness: 3
                }
              ).setOrigin(0.5).setDepth(91)
              // 存储引用以便下一帧清理
              if (!scene._explodeGfxRefs) scene._explodeGfxRefs = []
              scene._explodeGfxRefs.push(explodeGfx, countdownText)
            }
            // 清理上一帧的爆炸特效图（在绘制新图后延迟销毁）
            if (scene._prevExplodeGfxRefs) {
              scene.time.delayedCall(60, () => {
                for (const ref of scene._prevExplodeGfxRefs) {
                  try { ref.destroy() } catch (e) {}
                }
              })
            }
            scene._prevExplodeGfxRefs = scene._explodeGfxRefs
            scene._explodeGfxRefs = []
          } else {
            // 无爆炸怪物时清理残留
            if (scene._prevExplodeGfxRefs) {
              for (const ref of scene._prevExplodeGfxRefs) {
                try { ref.destroy() } catch (e) {}
              }
              scene._prevExplodeGfxRefs = null
            }
          }

          // handle attack key (J) pressed — enhanced sweep with particles, plus pierce on Shift+move
          // Also detect monsters in range and send damage commands to backend
          try {
            if (scene.keys.J && Phaser.Input.Keyboard.JustDown(scene.keys.J) && !scene._waveCharging.active) {
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
                  // 爆炸中的怪物不可被攻击
                  if (mon.exploding) continue
                  if (isMonsterInPierce(mon, startX, startY, dx, dy, dist)) {
                    hitMonsters.push(mon.name)
                  }
                }
              } else {
                for (const mon of scene.monstersData) {
                  // 爆炸中的怪物不可被攻击
                  if (mon.exploding) continue
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

          // ---------- 月光波弹射运动 ----------
          // move projectiles, handle wall bounce, and knockback monsters
          const waveRb = scene._roomBounds || { left: 16, top: 16, right: 784, bottom: 584 }
          for (const proj of scene._waveProjectiles) {
            if (!proj.alive) continue
            // move
            const stepX = Math.cos(proj.angle) * proj.speed * dt
            const stepY = Math.sin(proj.angle) * proj.speed * dt
            proj.x += stepX
            proj.y += stepY
            // spin visually
            proj.rotation += 4 * dt

            // store trail
            proj.trailPositions.push({ x: proj.x, y: proj.y })
            if (proj.trailPositions.length > 12) proj.trailPositions.shift()

            // wall bounce
            let bounced = false
            if (proj.x - proj.radius < waveRb.left) {
              proj.x = waveRb.left + proj.radius
              proj.angle = Math.PI - proj.angle
              bounced = true
            } else if (proj.x + proj.radius > waveRb.right) {
              proj.x = waveRb.right - proj.radius
              proj.angle = Math.PI - proj.angle
              bounced = true
            }
            if (proj.y - proj.radius < waveRb.top) {
              proj.y = waveRb.top + proj.radius
              proj.angle = -proj.angle
              bounced = true
            } else if (proj.y + proj.radius > waveRb.bottom) {
              proj.y = waveRb.bottom - proj.radius
              proj.angle = -proj.angle
              bounced = true
            }

            if (bounced) {
              proj.bounces++
              // 反弹后重置伤害判定，允许对同一敌人重复造成伤害
              proj.hitMonsters = new Set()
              // bounce flash
              const bounceFlash = scene.add.circle(proj.x, proj.y, proj.radius * 2, 0xFFD700, 0.5).setDepth(201)
              scene.tweens.add({
                targets: bounceFlash, alpha: 0, scale: 2, duration: 200,
                onComplete: () => { try { bounceFlash.destroy() } catch (e) {} }
              })
            }

            // check if max bounces reached
            if (proj.bounces > proj.maxBounces) {
              proj.alive = false
              // death flash
              const deathFlash = scene.add.circle(proj.x, proj.y, proj.radius * 3, 0xFFFFAA, 0.6).setDepth(202)
              scene.tweens.add({
                targets: deathFlash, alpha: 0, scale: 1.8, duration: 300,
                onComplete: () => { try { deathFlash.destroy() } catch (e) {} }
              })
              continue
            }

            // collision: damage + knockback monsters in range of projectile
            const COLLISION_RANGE = proj.radius + 30
            const KNOCKBACK_FORCE = 40
            if (!proj.hitMonsters) proj.hitMonsters = new Set()
            for (const mon of scene.monstersData) {
              if (!mon || !mon.circ) continue
              // 爆炸中的怪物不受月光波影响
              if (mon.exploding) continue
              const mdx = mon.x - proj.x
              const mdy = mon.y - proj.y
              const mdist = Math.sqrt(mdx * mdx + mdy * mdy)
              if (mdist < COLLISION_RANGE && mdist > 0.01) {
                // send damage command if this monster hasn't been hit yet by this projectile
                if (!proj.hitMonsters.has(mon.name)) {
                  proj.hitMonsters.add(mon.name)
                  ;(async () => {
                    try {
                      const res = await fetch('/api/command', {
                        method: 'POST', headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ command: 'wave ' + mon.name })
                      })
                      const j = await res.json()
                      emit('update', j)
                      if (j && j.data) scene.renderRoom(j.data)
                      if (j && j.message && j.message.includes('游戏结束')) {
                        scene.showGameOver()
                      }
                    } catch (e) {
                      // ignore network errors during rapid collisions
                    }
                  })()
                  // hit flash on monster
                  const hitFlash = scene.add.circle(mon.x, mon.y, 24, 0xFFD700, 0.6).setDepth(50)
                  scene.tweens.add({
                    targets: hitFlash, alpha: 0, scale: 1.6, duration: 200,
                    onComplete: () => { try { hitFlash.destroy() } catch (e) {} }
                  })
                }

                // knockback
                const nx = mdx / mdist
                const ny = mdy / mdist
                const pushX = nx * KNOCKBACK_FORCE
                const pushY = ny * KNOCKBACK_FORCE
                scene.tweens.add({
                  targets: { x: mon.x, y: mon.y },
                  x: Math.max(waveRb.left + pr, Math.min(waveRb.right - pr, mon.x + pushX)),
                  y: Math.max(waveRb.top + pr, Math.min(waveRb.bottom - pr, mon.y + pushY)),
                  duration: 150,
                  ease: 'Cubic.easeOut',
                  onUpdate: function (tween) {
                    const target = tween.targets[0]
                    mon.x = target.x
                    mon.y = target.y
                    try { mon.circ.setPosition(mon.x, mon.y) } catch (e) {}
                    try { mon.label.setPosition(mon.x - 32, mon.y + 24) } catch (e) {}
                  }
                })
              }
            }
          }

          // remove dead projectiles after 1 second
          scene._waveProjectiles = scene._waveProjectiles.filter(p => {
            if (!p.alive && !p._deathMarked) {
              p._deathMarked = true
              return true  // keep one more frame for flash
            }
            return p.alive
          })

          // draw wave projectiles every frame
          scene.drawWaveProjectile()

          // 祭坛碰撞回避：玩家圆点不与祭坛方块重叠
          if (scene.altarsData && scene.altarsData.length > 0) {
            const pr3 = (scene.playerRadius || 10) + 2
            for (const altar of scene.altarsData) {
              const halfSize = altar.size / 2
              const closestX = Phaser.Math.Clamp(scene.player.x, altar.x - halfSize, altar.x + halfSize)
              const closestY = Phaser.Math.Clamp(scene.player.y, altar.y - halfSize, altar.y + halfSize)
              const dx = scene.player.x - closestX
              const dy = scene.player.y - closestY
              const dist = Math.sqrt(dx * dx + dy * dy)
              if (dist < pr3) {
                if (dist > 0.001) {
                  const overlap = pr3 - dist
                  scene.player.x += (dx / dist) * overlap
                  scene.player.y += (dy / dist) * overlap
                } else {
                  scene.player.y -= pr3
                }
              }
            }
          }

          // NPC 商人碰撞回避：玩家圆点不与NPC重叠
          if (scene.shopNpcData) {
            const npcPr = (scene.playerRadius || 10) + scene.shopNpcData.radius + 4
            const dx = scene.player.x - scene.shopNpcData.x
            const dy = scene.player.y - scene.shopNpcData.y
            const dist = Math.sqrt(dx * dx + dy * dy)
            if (dist < npcPr && dist > 0.001) {
              const overlap = npcPr - dist
              scene.player.x += (dx / dist) * overlap
              scene.player.y += (dy / dist) * overlap
            }
          }

          // 门检测 (skip if charging)
          let insideAnyDoor = false
          if (scene.doorRects && !scene._waveCharging.active) {
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

          // ---------- 祭坛交互检测 ----------
          const ALTAR_INTERACT_RANGE = 50
          let closestAltar = null
          let closestDist = Infinity

          if (scene.altarsData && scene.altarsData.length > 0 && !scene.altarUsedInRoom) {
            for (const altar of scene.altarsData) {
              if (altar.activated) continue
              const dx = scene.player.x - altar.x
              const dy = scene.player.y - altar.y
              const dist = Math.sqrt(dx * dx + dy * dy)
              if (dist < ALTAR_INTERACT_RANGE && dist < closestDist) {
                closestDist = dist
                closestAltar = altar
              }
            }
          }

          // 更新白色倒三角指示器（祭坛）
          scene.altarIndicators.forEach(ind => { try { ind.destroy() } catch (e) {} })
          scene.altarIndicators = []
          if (closestAltar && !scene.altarUsedInRoom) {
            const triSize = 14
            const triY = closestAltar.y - closestAltar.size / 2 - triSize - 4
            const triX = closestAltar.x
            const triangle = scene.add.triangle(triX, triY, 0, triSize, triSize, 0, triSize * 2, triSize, 0xffffff, 0.9)
            triangle.setOrigin(0.5, 0.5)
            triangle.setScale(1, -1)
            scene.tweens.add({
              targets: triangle,
              alpha: 0.4,
              duration: 500,
              yoyo: true,
              repeat: -1,
              ease: 'Sine.easeInOut'
            })
            scene.altarIndicators.push(triangle)
          }

          // ---------- 商店NPC交互检测 ----------
          const SHOP_INTERACT_RANGE = 50
          let nearShopNpc = false
          if (scene.shopNpcData && !scene.shopMenuOverlay && !scene.shopBuyOverlay) {
            const dx = scene.player.x - scene.shopNpcData.x
            const dy = scene.player.y - scene.shopNpcData.y
            const dist = Math.sqrt(dx * dx + dy * dy)
            if (dist < SHOP_INTERACT_RANGE) {
              nearShopNpc = true
            }
          }

          // 更新商店NPC白色倒三角指示器
          scene.shopIndicators.forEach(ind => { try { ind.destroy() } catch (e) {} })
          scene.shopIndicators = []
          if (nearShopNpc) {
            const triSize = 14
            const triY = scene.shopNpcData.y - scene.shopNpcData.radius - triSize - 4
            const triX = scene.shopNpcData.x
            const triangle = scene.add.triangle(triX, triY, 0, triSize, triSize, 0, triSize * 2, triSize, 0xffffff, 0.9)
            triangle.setOrigin(0.5, 0.5)
            triangle.setScale(1, -1)
            scene.tweens.add({
              targets: triangle,
              alpha: 0.4,
              duration: 500,
              yoyo: true,
              repeat: -1,
              ease: 'Sine.easeInOut'
            })
            scene.shopIndicators.push(triangle)
          }

          // SPACE 键交互（商店优先于祭坛，skip if charging）
          try {
            if (scene.keys.SPACE && Phaser.Input.Keyboard.JustDown(scene.keys.SPACE) && !scene._waveCharging.active) {
              if (nearShopNpc) {
                // 与商人交互：弹出购物/售卖选项菜单
                scene.showShopMenu()
              } else if (closestAltar && !scene.altarUsedInRoom && !closestAltar.activated) {
                const altarType = closestAltar.type.toLowerCase()
                ;(async () => {
                  try {
                    const res = await fetch('/api/command', {
                      method: 'POST', headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ command: 'interact ' + altarType })
                    })
                    const j = await res.json()
                    emit('update', j)
                    if (j && j.data) {
                      scene.renderRoom(j.data)
                      if (altarType === 'heal') {
                        scene.spawnAltarGlow(closestAltar.x, closestAltar.y, closestAltar.size, 0x44cc44, 5000)
                      } else if (altarType === 'train') {
                        scene.spawnAltarGlow(closestAltar.x, closestAltar.y, closestAltar.size, 0xff8800, 5000)
                      } else if (altarType === 'wisdom') {
                        scene.spawnAltarGlow(closestAltar.x, closestAltar.y, closestAltar.size, 0x4488ff, 5000)
                        scene.showWisdomOverlay()
                      }
                    }
                  } catch (e) {
                    emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                  }
                })()
              }
            }
          } catch (e) { /* ignore */ }

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

  // 注册 B 键背包全局监听
  window.addEventListener('keydown', onKeyDown, true)
})

onBeforeUnmount(() => {
  if (game) {
    try { game.destroy(true) } catch (e) {}
    game = null
  }
  window.removeEventListener('minimap:update', onMinimapUpdate)
  window.removeEventListener('keydown', onKeyDown, true)
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

/* ==================== 背包 UI 样式 ==================== */
.backpack-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 800px;
  height: 600px;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.backpack-panel {
  width: 720px;
  height: 480px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  border: 2px solid #4a6fa5;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 40px rgba(0, 100, 200, 0.3), inset 0 0 20px rgba(0, 100, 200, 0.1);
}

.backpack-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #4a6fa5;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 12px 12px 0 0;
}

.backpack-title {
  font-size: 22px;
  font-weight: bold;
  color: #FFD700;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
}

.backpack-close {
  font-size: 22px;
  color: #ff6666;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}
.backpack-close:hover {
  background: rgba(255, 100, 100, 0.3);
}

.backpack-body {
  display: flex;
  flex: 1;
  padding: 16px;
  gap: 16px;
  overflow: hidden;
}

.backpack-left {
  display: grid;
  grid-template-columns: repeat(5, 72px);
  grid-template-rows: repeat(3, 72px);
  gap: 8px;
  padding: 4px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid #3a5a8c;
  border-radius: 8px;
}

.backpack-slot {
  width: 72px;
  height: 72px;
  background: rgba(30, 40, 60, 0.8);
  border: 2px solid #3a5a8c;
  border-radius: 6px;
  position: relative;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.backpack-slot.has-item {
  background: rgba(40, 55, 80, 0.9);
  border-color: #5a7aac;
}

.backpack-slot.slot-hovered.has-item {
  transform: scale(1.12);
  border-color: #88aadd;
  box-shadow: 0 0 12px rgba(100, 150, 255, 0.4);
  z-index: 2;
}

.backpack-slot.slot-selected {
  border-color: #ffffff;
  border-width: 3px;
  box-shadow: 0 0 14px rgba(255, 255, 255, 0.4);
}

.slot-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 215, 0, 0.15);
  border: 1px solid rgba(255, 215, 0, 0.3);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: bold;
  color: #FFD700;
  position: relative;
  overflow: visible;
}

/* 生命浆果图标：三个红色重叠圆点 */
.slot-icon-lifeberry {
  background: rgba(68, 204, 68, 0.12);
  border-color: rgba(68, 204, 68, 0.25);
}

.berry-dot {
  display: block;
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #ff4444, #cc0000);
  box-shadow: 0 0 4px rgba(255, 50, 50, 0.6);
}

/* 魔力浆果图标：三个蓝色重叠椭圆 */
.slot-icon-manaberry {
  background: rgba(68, 136, 255, 0.12);
  border-color: rgba(68, 136, 255, 0.25);
}

.mana-ellipse {
  display: block;
  position: absolute;
  width: 18px;
  height: 12px;
  border-radius: 50%;
  background: radial-gradient(ellipse at 35% 35%, #66aaff, #2255cc);
  box-shadow: 0 0 5px rgba(80, 140, 255, 0.6);
  transform: rotate(-30deg);
}

.slot-qty {
  position: absolute;
  bottom: 3px;
  right: 5px;
  font-size: 11px;
  color: #FFD700;
  text-shadow: 0 0 4px rgba(0, 0, 0, 0.8);
  font-weight: bold;
}

.backpack-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-image-frame {
  height: 180px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid #3a5a8c;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  color: #667799;
  font-size: 16px;
  font-style: italic;
}

.detail-image-placeholder {
  width: 120px;
  height: 120px;
  background: rgba(255, 215, 0, 0.08);
  border: 2px solid rgba(255, 215, 0, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: bold;
  color: #FFD700;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.4);
}

.detail-info {
  flex: 1;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid #3a5a8c;
  border-radius: 8px;
  overflow-y: auto;
}

.detail-row1 {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.detail-name {
  font-size: 18px;
  font-weight: bold;
}

.detail-id {
  font-size: 13px;
}

.detail-qty {
  font-size: 13px;
}

.detail-func {
  font-size: 14px;
  color: #ccddff;
  line-height: 1.5;
  margin-bottom: 4px;
}

.detail-spacer {
  height: 16px;
}

.detail-lore {
  font-size: 13px;
  color: #99aacc;
  font-style: italic;
  line-height: 1.5;
}

.detail-empty {
  color: #667799;
  font-size: 14px;
  font-style: italic;
  text-align: center;
  padding-top: 20px;
}

.detail-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-use {
  padding: 8px 28px;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(180deg, #33cc44 0%, #228833 100%);
  color: #ffffff;
  border: 1px solid #2aa033;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
.btn-use:hover:not(:disabled) {
  background: linear-gradient(180deg, #44dd55 0%, #33aa44 100%);
  box-shadow: 0 0 12px rgba(50, 200, 60, 0.5);
}
.btn-use:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-discard {
  padding: 8px 28px;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(180deg, #dd3333 0%, #991111 100%);
  color: #ffffff;
  border: 1px solid #bb2222;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
.btn-discard:hover:not(:disabled) {
  background: linear-gradient(180deg, #ee4444 0%, #aa2222 100%);
  box-shadow: 0 0 12px rgba(220, 40, 40, 0.5);
}
.btn-discard:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>