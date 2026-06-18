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
              <div v-if="getSlotItem(i - 1) && getSlotItem(i - 1).name.includes('生命浆果')" class="slot-icon slot-icon-lifeberry">
                <span class="berry-dot" style="left:10px;top:14px;"></span>
                <span class="berry-dot" style="left:20px;top:10px;"></span>
                <span class="berry-dot" style="left:28px;top:18px;"></span>
              </div>
              <!-- 魔力浆果：三个蓝色重叠椭圆 -->
              <div v-else-if="getSlotItem(i - 1) && getSlotItem(i - 1).name.includes('魔力浆果')" class="slot-icon slot-icon-manaberry">
                <span class="mana-ellipse" style="left:10px;top:16px;"></span>
                <span class="mana-ellipse" style="left:18px;top:10px;"></span>
                <span class="mana-ellipse" style="left:26px;top:18px;"></span>
              </div>
              <!-- 其他物品：显示名称首字母 -->
              <div v-else-if="getSlotItem(i - 1)" class="slot-icon">
                {{ getSlotItem(i - 1).name.charAt(0) }}
              </div>
              <!-- 已佩戴标记 -->
              <span v-if="getSlotItem(i - 1) && getSlotItem(i - 1).equipped" class="slot-equipped-badge">已佩戴</span>
              <!-- 物品数量角标 -->
              <span v-if="getSlotItem(i - 1) && !getSlotItem(i - 1).equipped" class="slot-qty">x{{ getSlotItem(i - 1).quantity }}</span>
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
              <!-- 饰品已佩戴 → 显示"卸下"按钮 -->
              <button
                  v-if="selectedItem && selectedItem.equipped"
                  class="btn-unequip"
                  @click="unequipItem"
              >卸下</button>
              <!-- 饰品未佩戴 → 显示"佩戴"按钮 -->
              <button
                  v-else-if="selectedItem && isAccessory(selectedItem)"
                  class="btn-use"
                  @click="useItem"
              >佩戴</button>
              <!-- 普通消耗品 → 显示"使用"按钮 -->
              <button
                  v-else
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

    <!-- ==================== 控制面板覆盖层（ESC 打开/关闭） ==================== -->
    <div v-if="controlPanelVisible" class="control-overlay" @click.self="closeControlPanel">
      <div class="control-panel">
        <button class="control-close-btn" @click="closeControlPanel" title="关闭 (ESC)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
        <h2 class="control-title">⚙ 游戏控制</h2>
        <div class="control-body">
          <button class="control-btn control-btn-restart" @click="handleRestart">🔄 重新开始</button>
          <button class="control-btn control-btn-save" @click="handleSaveGame" disabled>💾 保存游戏（开发中）</button>
          <button class="control-btn control-btn-menu" @click="handleBackToMenu">🚪 返回菜单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Phaser from 'phaser'

const emit = defineEmits(['update', 'resetGame', 'backToMenu'])
const gameContainer = ref(null)
const minimapCanvas = ref(null)
let game = null

// ==================== 背包 UI 响应式状态 ====================
const backpackVisible = ref(false)
const selectedSlot = ref(null)      // 选中的格子索引 (0-14)
const hoveredSlot = ref(null)       // 鼠标悬停的格子索引
const backpackItems = ref([])       // 背包物品列表 [{itemId, name, rarity, functionDesc, loreDesc, quantity, ...}]

// ==================== 控制面板 UI 响应式状态 ====================
const controlPanelVisible = ref(false)

function openControlPanel() {
  // 暂停 Phaser 场景，防止 ESC 关闭面板时触发其他行为
  if (game && game.scene && game.scene.scenes) {
    game.scene.scenes.forEach(s => { if (s.scene.isActive()) s.scene.pause() })
  }
  controlPanelVisible.value = true
}

function closeControlPanel() {
  controlPanelVisible.value = false
  // 恢复 Phaser 场景
  if (game && game.scene && game.scene.scenes) {
    game.scene.scenes.forEach(s => { if (s.scene.isPaused()) s.scene.resume() })
  }
}

function handleRestart() {
  closeControlPanel()
  emit('resetGame')
}

function handleSaveGame() {
  // 预留接口：未来接入数据库存档
}

function handleBackToMenu() {
  closeControlPanel()
  emit('backToMenu')
}

// 稀有度对应颜色
function rarityColor(rarity) {
  switch (rarity) {
    case 'legendary': return '#FF6600'
    case 'epic': return '#CC44FF'
    case 'rare': return '#4488FF'
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

// 判断是否为饰品
function isAccessory(item) {
  if (!item || !item.name) return false
  const name = item.name
  return name.includes('暗影披风') || name.includes('生命戒指') || name.includes('元素项链')
}

// 使用物品（饰品则为佩戴）
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

// 卸下饰品
async function unequipItem() {
  if (!selectedItem.value) return
  try {
    const res = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: 'bag unequip ' + selectedItem.value.name })
    })
    const j = await res.json()
    emit('update', j)
    if (j && j.data) {
      if (j.data.backpack) backpackItems.value = j.data.backpack
      window.dispatchEvent(new CustomEvent('game:update', { detail: j }))
    }
    if (selectedItem.value) {
      const stillExists = backpackItems.value.find(it => it.name === selectedItem.value.name)
      if (stillExists) {
        const idx = backpackItems.value.indexOf(stillExists)
        selectedSlot.value = idx
        selectedItem.value = stillExists
      }
    }
  } catch (e) {
    console.warn('卸下饰品失败', e)
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
  // ESC 键：切换控制面板
  if (e.key === 'Escape') {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return
    // 如果背包打开，先关闭背包
    if (backpackVisible.value) {
      closeBackpack()
      return
    }
    // 切换控制面板
    e.preventDefault()
    if (controlPanelVisible.value) {
      closeControlPanel()
    } else {
      openControlPanel()
    }
  }
  // 背包打开时阻止其他按键进入游戏
  if (backpackVisible.value) {
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
          // 房间背景映射缓存：确保同一房间始终使用同一背景
          scene._roomBgCache = {}
          scene.setBgForRoom = function (roomInfo) {
            try {
              if (!roomInfo || !roomInfo.name) return
              const roomName = roomInfo.name
              // 如果已缓存，直接使用
              if (scene._roomBgCache[roomName]) {
                try { scene.bgFar.setTexture(scene._roomBgCache[roomName]) } catch (e) {}
                try { scene.bgNear.setAlpha(0); scene.bgFar.setAlpha(1.0) } catch (e) {}
                return
              }
              const roomType = roomInfo.roomType ? roomInfo.roomType : null
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
              // prefer mapping, otherwise deterministic random based on room name hash
              let chosen = null
              if (roomType && mapping[roomType] && scene.textures.exists(mapping[roomType])) chosen = mapping[roomType]
              if (!chosen) {
                // 使用房间名的hashCode确定背景，保证唯一性
                const hash = roomName.split('').reduce((acc, c) => acc * 31 + c.charCodeAt(0), 0)
                chosen = avail[Math.abs(hash) % avail.length]
              }
              // 缓存并应用
              scene._roomBgCache[roomName] = chosen
              try { scene.bgFar.setTexture(chosen) } catch (e) {}
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

        // 中毒状态相关
        scene._poisonIconGfx = null        // 中毒图标（💀）
        scene._poisonLayersText = null     // 中毒层数文字
        scene._poisonTimerText = null      // 中毒倒计时文字
        scene._poisonLastTickMs = 0        // 后端 lastTickTime（毫秒）
        scene._poisonNextTickMs = 0        // 本地计算的剩余毫秒

        // 流血状态相关
        scene._bleedIconGfx = null         // 流血图标（🩸）
        scene._bleedLayersText = null      // 流血层数文字

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

          // 使用 🔥 emoji 作为烧伤图标，固定大小不随层数变化
          const flameText = scene.add.text(cx, cy, '🔥', {
            font: '20px Arial',
          }).setDepth(120).setOrigin(0.5, 0.5)
          scene._burnFlameGfx = flameText

          // 层数标注（火焰右侧）
          const layersText = scene.add.text(cx + 10, cy, String(layers), {
            font: 'bold 14px Arial',
            fill: '#FF6644',
            stroke: '#000000',
            strokeThickness: 3
          }).setDepth(121).setOrigin(0, 0.5)
          scene._burnLayersText = layersText

          // 倒计时标注（火焰下方）
          const timerText = scene.add.text(cx, cy + 16, Math.ceil(timerSec) + 's', {
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
          const buffTop = mpY + barH - 7      // MP条下缘
          const buffBottom = roomTop - 4
          const buffLeft = barX - 105        // HP/MP label 左边缘
          const buffRight = barX + barW       // HP/MP 条右边缘
          const buffMidY = (buffTop + buffBottom) / 2

          // ---- 先清理所有旧的状态显示 ----
          if (scene._burnFlameGfx) { try { scene._burnFlameGfx.destroy() } catch (e) {}; scene._burnFlameGfx = null }
          if (scene._burnLayersText) { try { scene._burnLayersText.destroy() } catch (e) {}; scene._burnLayersText = null }
          if (scene._burnTimerText) { try { scene._burnTimerText.destroy() } catch (e) {}; scene._burnTimerText = null }
          scene._burnNextTickMs = 0
          if (scene._poisonIconGfx) { try { scene._poisonIconGfx.destroy() } catch (e) {}; scene._poisonIconGfx = null }
          if (scene._poisonLayersText) { try { scene._poisonLayersText.destroy() } catch (e) {}; scene._poisonLayersText = null }
          if (scene._poisonTimerText) { try { scene._poisonTimerText.destroy() } catch (e) {}; scene._poisonTimerText = null }
          scene._poisonNextTickMs = 0
          if (scene._bleedIconGfx) { try { scene._bleedIconGfx.destroy() } catch (e) {}; scene._bleedIconGfx = null }
          if (scene._bleedLayersText) { try { scene._bleedLayersText.destroy() } catch (e) {}; scene._bleedLayersText = null }

          // ---- 收集所有活跃状态，按固定顺序排列 ----
          // 排序规则：先减益后增益，同类型按名称排序
          const typeOrder = { 'BURN': 0, 'POISON': 1, 'BLEED': 2 }
          const orderedEffects = scene._activeEffects
            .filter(eff => eff && eff.layers > 0)
            .sort((a, b) => {
              const orderA = typeOrder[a.type] !== undefined ? typeOrder[a.type] : 99
              const orderB = typeOrder[b.type] !== undefined ? typeOrder[b.type] : 99
              if (orderA !== orderB) return orderA - orderB
              return (a.name || a.type).localeCompare(b.name || b.type)
            })

          if (orderedEffects.length === 0) return

          // ---- 顺序格子布局：每个状态占用一个格子，从左到右排列 ----
          const cellWidth = 40    // 每个状态格子的宽度
          const startX = 580      // 第一个格子中心X（固定）

          for (let i = 0; i < orderedEffects.length; i++) {
            const eff = orderedEffects[i]
            const cellCX = startX + i * cellWidth
            const cellCY = buffMidY + 6

            if (eff.type === 'BURN') {
              const layers = eff.layers || 0
              const nextTickIn = eff.nextTickIn || 3000

              // 记录用于实时倒计时
              scene._burnLastTickMs = Date.now()
              scene._burnNextTickMs = nextTickIn

              const flameSize = Math.min(3, 1 + layers * 0.5)
              const timerSec = nextTickIn / 1000

              scene.drawBurnFlame(cellCX, cellCY, flameSize, layers, timerSec)
            } else if (eff.type === 'POISON') {
              const layers = eff.layers || 0
              const nextTickIn = eff.nextTickIn || 1000

              // 记录用于实时倒计时
              scene._poisonLastTickMs = Date.now()
              scene._poisonNextTickMs = nextTickIn

              const timerSec = nextTickIn / 1000

              scene.drawPoisonIcon(cellCX, cellCY, layers, timerSec)
            } else if (eff.type === 'BLEED') {
              const layers = eff.layers || 0
              scene.drawBleedIcon(cellCX, cellCY, layers)
            }
          }
        }

        /**
         * 绘制中毒状态图标与层数标注。
         * @param {Phaser.Scene} scene
         * @param {number} cx      图标中心X
         * @param {number} cy      图标中心Y
         * @param {number} layers  中毒层数
         * @param {number} timerSec 距下次结算秒数
         */
        scene.drawPoisonIcon = function (cx, cy, layers, timerSec) {
          // 清理旧的中毒显示
          if (scene._poisonIconGfx) { try { scene._poisonIconGfx.destroy() } catch (e) {}; scene._poisonIconGfx = null }
          if (scene._poisonLayersText) { try { scene._poisonLayersText.destroy() } catch (e) {}; scene._poisonLayersText = null }
          if (scene._poisonTimerText) { try { scene._poisonTimerText.destroy() } catch (e) {}; scene._poisonTimerText = null }

          if (layers <= 0) return

          // 使用 💀 emoji 作为中毒图标
          const iconText = scene.add.text(cx, cy, '💀', {
            font: '20px Arial',
          }).setDepth(120).setOrigin(0.5, 0.5)
          scene._poisonIconGfx = iconText

          // 层数标注（图标右侧）
          const layersText = scene.add.text(cx + 11, cy, String(layers), {
            font: 'bold 14px Arial',
            fill: '#BB44FF',
            stroke: '#000000',
            strokeThickness: 3
          }).setDepth(121).setOrigin(0, 0.5)
          scene._poisonLayersText = layersText

          // 倒计时标注（图标下方）
          const timerText = scene.add.text(cx, cy + 16, Math.ceil(timerSec) + 's', {
            font: '10px Arial',
            fill: '#CC88FF',
            stroke: '#000000',
            strokeThickness: 2
          }).setDepth(121).setOrigin(0.5, 0)
          scene._poisonTimerText = timerText
        }

        /**
         * 绘制流血状态图标与层数标注。
         * 流血不由计时驱动，仅在攻击时触发，因此不显示倒计时。
         * @param {Phaser.Scene} scene
         * @param {number} cx      图标中心X
         * @param {number} cy      图标中心Y
         * @param {number} layers  流血层数
         */
        scene.drawBleedIcon = function (cx, cy, layers) {
          // 清理旧的流血显示
          if (scene._bleedIconGfx) { try { scene._bleedIconGfx.destroy() } catch (e) {}; scene._bleedIconGfx = null }
          if (scene._bleedLayersText) { try { scene._bleedLayersText.destroy() } catch (e) {}; scene._bleedLayersText = null }

          if (layers <= 0) return

          // 使用 🩸 emoji 作为流血图标
          const iconText = scene.add.text(cx, cy, '🩸', {
            font: '20px Arial',
          }).setDepth(120).setOrigin(0.5, 0.5)
          scene._bleedIconGfx = iconText

          // 层数标注（图标右侧）
          const layersText = scene.add.text(cx + 11, cy, String(layers), {
            font: 'bold 14px Arial',
            fill: '#FF2222',
            stroke: '#000000',
            strokeThickness: 3
          }).setDepth(121).setOrigin(0, 0.5)
          scene._bleedLayersText = layersText
        }

        scene.titleText = scene.add.text(20, 20, '', { font: '20px Arial', fill: '#ffffff' })
        scene.descText = scene.add.text(20, 50, '', { font: '14px Arial', fill: '#cccccc', wordWrap: { width: 760 } })

        scene.playerRadius = 10
        scene.player = scene.add.circle(400, 320, scene.playerRadius, 0x00aaff).setStrokeStyle(2, 0x000000)
        scene.playerLabel = scene.add.text(400 - 20, 320 + 18, 'You', { font: '12px Arial', fill: '#fff' })
        scene._roomBounds = { left: 16, top: 16, right: 800 - 16, bottom: 600 - 16 }

        scene.add.text(20, 560, 'WASD 移动 | J 攻击/长按蓄力 | Shift+方向+J 突刺 | 空格 互动 | H 月光波', { font: '14px Arial', fill: '#cccccc' })

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
            // if response contains room data, re-render (跳过商店/祭坛浮层)
            if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
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
              // 先通知小地图清空旧数据，再渲染房间
              try { window.dispatchEvent(new CustomEvent('game:reset')) } catch (e) {}
              try { window.dispatchEvent(new CustomEvent('game:update', { detail: j })) } catch (e) {}
              if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                scene.renderRoom(j.data)
              }
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
          if (scene.shopSellOverlay) { try { scene.shopSellOverlay.destroy() } catch (e) {}; scene.shopSellOverlay = null }
          if (scene.shopNpcCircle) { try { scene.shopNpcCircle.destroy() } catch (e) {}; scene.shopNpcCircle = null }
          if (scene.shopNpcLabel) { try { scene.shopNpcLabel.destroy() } catch (e) {}; scene.shopNpcLabel = null }
          scene.shopNpcData = null
          scene.shopIndicators.forEach(ind => { try { ind.destroy() } catch (e) {} })
          scene.shopIndicators = []
          // clear previous dropped items
          if (scene.droppedItemsGroup) {
            scene.droppedItemsGroup.clear(true, true)
          }
          scene.droppedItemsData = []
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
          // 火焰史莱姆不自爆，以普通怪物方式死亡
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
            const circleColor = 0xaa0000
            const circ = scene.add.circle(x, y, 20, circleColor).setStrokeStyle(2, 0x000000)
            const labelText = mon.name
            const label = scene.add.text(x - 32, y + 24, labelText, { font: '14px Arial', fill: '#fff' })

            // 血条背景（深色）— 放在怪物圆点上方
            const hpBarW = 54, hpBarH = 8
            const hpBarX = x - hpBarW / 2
            const hpBarY = y - 20 - hpBarH - 32
            const hpBg = scene.add.rectangle(hpBarX + hpBarW / 2, hpBarY + hpBarH / 2, hpBarW, hpBarH, 0x333333).setStrokeStyle(1, 0x666666)
            // 血条填充
            const monMaxHp = mon.maxHp || mon.hp || 100
            const hpRatio = Math.max(0, Math.min(1, (mon.hp || 0) / monMaxHp))
            let hpColor = 0x44cc44
            if (hpRatio < 0.3) hpColor = 0xcc2222
            else if (hpRatio < 0.6) hpColor = 0xccaa22
            const hpFill = scene.add.rectangle(hpBarX, hpBarY, hpBarW * hpRatio, hpBarH, hpColor).setOrigin(0, 0)
            // HP 数字文字（血条内部居中，加粗白字带描边）
            const hpNumText = scene.add.text(hpBarX + hpBarW / 2, hpBarY + hpBarH / 2, mon.hp + '/' + monMaxHp, {
              font: 'bold 11px Arial', fill: '#ffffff', stroke: '#000000', strokeThickness: 2
            }).setOrigin(0.5, 0.5)

            if (!scene.monstersGroup) scene.monstersGroup = scene.add.group()
            if (!scene.monstersData) scene.monstersData = []
            scene.monstersGroup.add(circ)
            scene.monstersGroup.add(label)
            scene.monstersGroup.add(hpBg)
            scene.monstersGroup.add(hpFill)
            scene.monstersGroup.add(hpNumText)
            scene.monstersData.push({
              name: mon.name, x, y, circ, label,
              hp: mon.hp, maxHp: monMaxHp, type: mon.type,
              hpBarBg: hpBg, hpBarFill: hpFill, hpNumText: hpNumText,
              hpBarW: hpBarW, hpBarH: hpBarH,
              defense: mon.defense || 0, magicResist: mon.magicResist || 0, speed: mon.speed || 100,
              specialType: mon.specialType || null,
              exploding: false,
              explodeNotified: false,
              explodeRange: 0,
              explodeRemaining: 0
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

          // ---------- 掉落物渲染 ----------
          const droppedItems = roomInfo.droppedItems || []
          if (!scene.droppedItemsGroup) scene.droppedItemsGroup = scene.add.group()
          // 使用房间名+物品名哈希缓存掉落物位置，保证每次渲染位置不变
          if (!scene._dropPosCache) scene._dropPosCache = {}
          scene.droppedItemsData = []
          droppedItems.forEach(drop => {
            // 为当前房间的每个掉落物生成固定坐标
            const cacheKey = (roomInfo.name || '') + '::' + drop.itemName
            let dx, dy
            if (scene._dropPosCache[cacheKey]) {
              dx = scene._dropPosCache[cacheKey].x
              dy = scene._dropPosCache[cacheKey].y
            } else {
              // 基于房间名+物品名生成确定性偏移
              const hash = (cacheKey.split('').reduce((acc, c) => acc * 31 + c.charCodeAt(0), 0) & 0x7fffffff)
              const offX = ((hash % 61) - 30)
              const offY = (((hash >> 8) % 61) - 30)
              dx = rectCenterX + offX
              dy = rectCenterY + offY
              scene._dropPosCache[cacheKey] = { x: dx, y: dy }
            }
            // 药水图标：彩色小瓶子形状
            const isLife = drop.itemName && drop.itemName.includes('生命')
            const color = isLife ? 0x44cc44 : 0x4488ff
            const icon = scene.add.circle(dx, dy, 12, color).setStrokeStyle(2, 0xffffff)
            icon.setDepth(60)
            // 标签
            const label = scene.add.text(dx - 20, dy + 16, drop.itemName, {
              font: '11px Arial', fill: '#ffffff', stroke: '#000000', strokeThickness: 2
            }).setDepth(61)
            scene.droppedItemsGroup.add(icon)
            scene.droppedItemsGroup.add(label)
            scene.droppedItemsData.push({
              itemName: drop.itemName,
              x: dx, y: dy,
              icon, label,
              nearPlayer: false
            })
          })

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
            scene.showShopSell()
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

        // ---------- 商店售卖界面浮层 ----------
        scene.showShopSell = function () {
          if (scene.shopSellOverlay) { try { scene.shopSellOverlay.destroy() } catch (e) {} }
          const overlay = scene.add.container(0, 0).setDepth(200)
          const bg = scene.add.rectangle(400, 300, 800, 600, 0x000000, 0.6)
          bg.setInteractive()
          const title = scene.add.text(400, 80, '售卖 — 点击物品出售', {
            font: 'bold 22px Arial', fill: '#FFD700'
          }).setOrigin(0.5)

          // 获取玩家当前货币
          const currentMoney = scene.playerStats.money || 0
          const moneyText = scene.add.text(400, 110, '当前金币：$' + currentMoney, {
            font: '16px Arial', fill: '#FFD700'
          }).setOrigin(0.5)

          // 获取背包物品列表（从后端或本地数据）
          async function loadAndShowSellItems() {
            try {
              const res = await fetch('/api/backpack')
              const j = await res.json()
              const items = (j && j.data && j.data.backpack) ? j.data.backpack : []

              // 构建物品列表
              const itemElements = []
              const startY = 150
              const rowH = 45

              for (let i = 0; i < items.length; i++) {
                const invItem = items[i]
                const y = startY + i * rowH

                // 物品名称
                const nameText = scene.add.text(120, y, invItem.name, {
                  font: '16px Arial', fill: '#ffffff'
                }).setOrigin(0, 0.5)

                // 数量
                const qtyText = scene.add.text(300, y, 'x' + invItem.quantity, {
                  font: '14px Arial', fill: '#aaaaaa'
                }).setOrigin(0, 0.5)

                // 售价（买价的一半）
                const price = Math.max(1, Math.floor((invItem.price || 0) / 2))
                const priceText = scene.add.text(380, y, '$' + price, {
                  font: 'bold 16px Arial', fill: '#FFD700'
                }).setOrigin(0, 0.5)

                // 出售按钮
                const sellBtn = scene.add.text(500, y, '[ 出售 ]', {
                  font: '15px Arial', fill: '#44cc44', backgroundColor: '#222222',
                  padding: { x: 8, y: 4 }
                }).setOrigin(0.5).setInteractive({ useHandCursor: true })
                sellBtn.on('pointerover', () => sellBtn.setStyle({ fill: '#ffffff', backgroundColor: '#445544' }))
                sellBtn.on('pointerout', () => sellBtn.setStyle({ fill: '#44cc44', backgroundColor: '#222222' }))

                // 已装备标记（不可出售）
                if (invItem.equipped) {
                  const equippedTag = scene.add.text(580, y, '已装备', {
                    font: '13px Arial', fill: '#ff8800'
                  }).setOrigin(0, 0.5)
                  itemElements.push(nameText, qtyText, priceText, sellBtn, equippedTag)
                } else {
                  sellBtn.on('pointerdown', async () => {
                    try {
                      const res = await fetch('/api/command', {
                        method: 'POST', headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ command: 'shop sell ' + invItem.name })
                      })
                      const j = await res.json()
                      emit('update', j)
                      // 刷新售卖界面
                      try { scene.shopSellOverlay.destroy() } catch (e) {}
                      scene.shopSellOverlay = null
                      scene.showShopSell()
                    } catch (e) {
                      emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                    }
                  })
                  itemElements.push(nameText, qtyText, priceText, sellBtn)
                }
              }

              // 如果没有物品
              if (items.length === 0) {
                const emptyText = scene.add.text(400, 300, '背包中没有可出售的物品', {
                  font: '18px Arial', fill: '#888888'
                }).setOrigin(0.5)
                itemElements.push(emptyText)
              }

              overlay.add([bg, title, moneyText, ...itemElements, endBtn])

              // 更新货币显示
              if (j && j.data && j.data.playerMoney !== undefined) {
                moneyText.setText('当前金币：$' + j.data.playerMoney)
              }
            } catch (e) {
              emit('update', { status: 'error', message: '无法获取背包数据: ' + e.message, data: null })
            }
          }

          // "结束售卖"按钮
          const endBtn = scene.add.text(400, 540, '结束售卖', {
            font: 'bold 20px Arial', fill: '#ff6666', backgroundColor: '#222222',
            padding: { x: 24, y: 8 }
          }).setOrigin(0.5).setInteractive({ useHandCursor: true })
          endBtn.on('pointerover', () => endBtn.setStyle({ fill: '#ffffff', backgroundColor: '#664444' }))
          endBtn.on('pointerout', () => endBtn.setStyle({ fill: '#ff6666', backgroundColor: '#222222' }))
          endBtn.on('pointerdown', () => {
            try { scene.shopSellOverlay.destroy() } catch (e) {}
            scene.shopSellOverlay = null
          })

          scene.shopSellOverlay = overlay
          loadAndShowSellItems()
        }

        // 键盘控制
        scene.keys = scene.input.keyboard.addKeys('W,A,S,D,SHIFT,J,SPACE,H,ONE,TWO,THREE')
        scene.baseMoveSpeed = 160
        scene.facingAngle = 0
        scene.attackConfig = {
          radius: 120,
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
        scene._attackOnCooldown = false  // 攻击冷却：动画期间禁止再次攻击
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

        // 蓄力攻击状态（长按J键）
        scene._chargeAttack = { active: false, startTime: 0, charged: false, chargeBarGfx: null, circleGfx: null }
        scene._chargeAttackPendingSend = false
        scene._jHeldSince = null  // J键按下时刻，用于区分短按（横扫/突刺）和长按（蓄力）

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
        scene.shopSellOverlay = null     // 售卖界面浮层
        // dropped items state
        scene.droppedItemsGroup = scene.add.group()
        scene.droppedItemsData = []       // [{ itemName, x, y, icon, label, nearPlayer }]
        scene._closestDropItem = null     // 最近的可拾取掉落物
        // monster AI state: cooldown tracking (ms timestamp per monster name)
        scene.monsterAttackCooldowns = {}  // { monName: lastAttackTime }

        // 背包暂停状态
        scene._backpackPaused = false
        scene._pendingKnockbacks = null  // 攻击击退待处理队列
        // 返回菜单暂停状态（防止销毁时视觉扭曲）
        scene._menuPaused = false
        window.addEventListener('backpack:toggle', function(e) {
          scene._backpackPaused = e.detail.visible
        })
        window.addEventListener('game:pause', function() {
          scene._menuPaused = true
          // 暂停场景的 update 事件
          scene.sys.events.pause()
          // 停止所有 tween 动画
          scene.tweens.killAll()
          // 停止所有时间事件
          scene.time.removeAllEvents()
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
          // 注意：如果有商店菜单、购物界面或博学祭坛浮层打开，不调用 renderRoom()
          // 因为 renderRoom() 会销毁这些覆盖层，导致菜单闪退
          if (!scene._backpackPaused && j && j.data && j.data.name && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
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

          // ---------- 中毒倒计时实时更新 ----------
          if (scene._poisonNextTickMs > 0) {
            const nowPoison = Date.now()
            const elapsed = nowPoison - scene._poisonLastTickMs
            const remaining = Math.max(0, scene._poisonNextTickMs - elapsed)
            scene._poisonNextTickMs = remaining
            scene._poisonLastTickMs = nowPoison
            // 更新倒计时文字
            if (scene._poisonTimerText && scene._poisonTimerText.active) {
              const timerSec = remaining / 1000
              scene._poisonTimerText.setText(Math.ceil(timerSec) + 's')
              // 小于1秒时变红色闪烁
              if (timerSec < 1) {
                const flash = Math.sin(nowPoison * 0.02) * 0.3 + 0.7
                scene._poisonTimerText.setAlpha(flash)
                scene._poisonTimerText.setColor('#FF4444')
              } else {
                scene._poisonTimerText.setAlpha(1)
                scene._poisonTimerText.setColor('#CC88FF')
              }
            }
            // 中毒图标呼吸闪烁效果
            if (scene._poisonIconGfx && scene._poisonIconGfx.active) {
              const pulse = 0.7 + Math.sin(nowPoison * 0.003) * 0.3
              scene._poisonIconGfx.setAlpha(pulse)
            }
            if (remaining <= 0) {
              // 倒计时归零：清理显示（下次后端数据到达时会重新评估）
              scene._poisonNextTickMs = 0
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
                  // ---- 火焰史莱姆不自爆，无需爆炸特效 ----
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
                  // 只要有房间数据就渲染房间，确保爆炸结算后怪物从画面消失
                  // 注意：如果有商店菜单、购物界面或博学祭坛浮层打开，不调用 renderRoom()
                  // 因为 renderRoom() 会销毁这些覆盖层，导致菜单闪退
                  if (j.data && j.data.name && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
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
            if (scene.keys.H && scene.keys.H.isDown && hasEnoughMp && !scene._waveCharging.active && !scene._wavePendingSend && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
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
                if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                  scene.renderRoom(j.data)
                }
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
          const isWaveCharging = scene._waveCharging.active && !scene._waveCharging.charged  // 月光波蓄力中（未满）禁止移动
          const isWaveAiming = scene._waveCharging.active && scene._waveCharging.charged      // 月光波满蓄力瞄准中：允许转向
          if (!isWaveCharging) {
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
            if (!isWaveAiming) {
              let speed = scene.baseMoveSpeed
              // 蓄力攻击期间移速减半
              if (scene._chargeAttack.active) speed = speed * 0.5
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
          const MONSTER_ATTACK_RANGE = 45    // 普通怪物攻击距离
          const MONSTER_ATTACK_COOLDOWN = 1500 // 普通怪物攻击间隔 (ms)
          // Boss 特殊参数
          const BOSS_ATTACK_RANGE = 90       // Boss攻击距离（普通怪物的2倍）
          const BOSS_ATTACK_COOLDOWN = 750   // Boss攻击间隔（普通怪物的一半）
          const pr = scene.playerRadius || 10

          const now = Date.now()

          for (const mon of scene.monstersData) {
            if (!mon || !mon.circ) continue

            const isBoss = (mon.type === 2)
            const attackRange = isBoss ? BOSS_ATTACK_RANGE : MONSTER_ATTACK_RANGE
            const attackCooldown = isBoss ? BOSS_ATTACK_COOLDOWN : MONSTER_ATTACK_COOLDOWN

            const dx = scene.player.x - mon.x
            const dy = scene.player.y - mon.y
            const dist = Math.sqrt(dx * dx + dy * dy)

            if (dist <= attackRange) {
              // 在攻击范围内 → 攻击玩家（每个怪物独立冷却，无全局锁）
              if (!scene.monsterAttackCooldowns[mon.name] || now - scene.monsterAttackCooldowns[mon.name] >= attackCooldown) {
                scene.monsterAttackCooldowns[mon.name] = now
                ;(async () => {
                  try {
                    const res = await fetch('/api/command', {
                      method: 'POST', headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ command: 'monsterattack ' + mon.name })
                    })
                    const j = await res.json()
                    emit('update', j)
                    if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                      scene.renderRoom(j.data)
                    }
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

          // ---------- 绘制怪物攻击范围圈（半透明圆环） ----------
          // 使用持久 Graphics 对象，每帧清空重绘
          if (!scene._monsterRangeGfx) {
            scene._monsterRangeGfx = scene.add.graphics().setDepth(3)
          }
          const rangeGfx = scene._monsterRangeGfx
          rangeGfx.clear()
          for (const mon of scene.monstersData) {
            if (!mon || !mon.circ) continue
            // 爆炸中的怪物不显示攻击范围
            if (mon.exploding) continue
            const isBoss = (mon.type === 2)
            const attackRange = isBoss ? BOSS_ATTACK_RANGE : MONSTER_ATTACK_RANGE
            // 只有当玩家在索敌范围内才显示攻击范围圈（减少视觉杂乱）
            const dx = scene.player.x - mon.x
            const dy = scene.player.y - mon.y
            const dist = Math.sqrt(dx * dx + dy * dy)
            if (dist <= MONSTER_DETECT_RANGE || isBoss) {
              // Boss 全图显示，普通怪物在索敌范围内才显示
              rangeGfx.lineStyle(1, 0xff4444, 0.25)
              rangeGfx.strokeCircle(mon.x, mon.y, attackRange)
              // 填充色（极淡）
              rangeGfx.fillStyle(0xff2222, 0.05)
              rangeGfx.fillCircle(mon.x, mon.y, attackRange)
            }
          }

          // handle attack key (J) pressed — enhanced sweep with particles, plus pierce on Shift+move
          // Also detect monsters in range and send damage commands to backend
          // ---------- 蓄力攻击（长按J键）状态机 ----------
          const CHARGE_ATTACK_DURATION = 1000  // 1秒蓄力
          const CHARGE_ENTRY_THRESHOLD = 200   // 按住200ms后才进入蓄力，短按留给横扫/突刺
          const chargeNowMs = Date.now()

          // 记录J键首次按下的时间，松开时根据时长决定横扫/突刺还是蓄力
          if (!scene._jHeldSince && scene.keys.J && scene.keys.J.isDown && !scene._attackOnCooldown
              && !scene._waveCharging.active && !scene._chargeAttack.active) {
            scene._jHeldSince = chargeNowMs
          }
          if (scene._jHeldSince && !(scene.keys.J && scene.keys.J.isDown)) {
            const heldDuration = chargeNowMs - scene._jHeldSince
            if (heldDuration < CHARGE_ENTRY_THRESHOLD && !scene._attackOnCooldown && !scene._chargeAttack.active) {
              // 短按（<200ms）→ 触发横扫或突刺
              scene._attackOnCooldown = true
              const cfg = scene.attackConfig || {}
              const isShiftMove = (scene.keys.SHIFT && scene.keys.SHIFT.isDown) && (scene.keys.W.isDown || scene.keys.A.isDown || scene.keys.S.isDown || scene.keys.D.isDown)
              const monsterPositions = []
              for (const mon of scene.monstersData) {
                if (mon && mon.name && !mon.exploding) { monsterPositions.push({ name: mon.name, x: mon.x, y: mon.y }) }
              }
              if (isShiftMove) {
                // 突刺特效
                const startX = scene.player.x, startY = scene.player.y
                const dx_ = Math.cos(scene.facingAngle), dy_ = Math.sin(scene.facingAngle)
                const dist = cfg.pierceDistance || 120
                const pr_ = scene.playerRadius || 10
                const targetX = Phaser.Math.Clamp(startX + dx_ * dist, rb.left + pr_, rb.right - pr_)
                const targetY = Phaser.Math.Clamp(startY + dy_ * dist, rb.top + pr_, rb.bottom - pr_)
                scene.tweens.add({ targets: scene.player, x: targetX, y: targetY, duration: cfg.pierceDuration || 100, ease: 'Cubic.easeOut' })
                const g2 = scene.add.graphics()
                const extra = cfg.pierceDistanceExpand || 1.0
                const effTX = Phaser.Math.Clamp(startX + dx_ * dist * extra, rb.left + pr_, rb.right - pr_)
                const effTY = Phaser.Math.Clamp(startY + dy_ * dist * extra, rb.top + pr_, rb.bottom - pr_)
                const mx_ = (startX + effTX) / 2, my_ = (startY + effTY) / 2
                const w_ = cfg.pierceWidth || 12
                g2.fillStyle(0xC0C0C0, cfg.mainAlpha || 0.95)
                g2.fillPoints([{x:effTX,y:effTY},{x:mx_+(-dy_)*(w_/2),y:my_+dx_*(w_/2)},{x:startX,y:startY},{x:mx_-(-dy_)*(w_/2),y:my_-dx_*(w_/2)}], true)
                scene.tweens.add({ targets: g2, alpha: 0, duration: cfg.pierceFade || 180, onComplete: () => { try { g2.destroy() } catch (e) {}; scene._attackOnCooldown = false } })
              } else {
                // 横扫特效
                const mainGfx = scene.add.graphics(), fireGfx = scene.add.graphics()
                const progress_ = { t: 0 }
                scene.drawArcSlash(mainGfx, 0); scene.drawFireDistortion(fireGfx, 0)
                scene.spawnAttackParticles(0, 20); scene.spawnShockwave()
                if (scene.cameras && scene.cameras.main) scene.cameras.main.shake(120, 0.005)
                scene.tweens.add({
                  targets: progress_, t: 1, duration: scene.attackConfig.sweepDuration || 160, ease: 'Cubic.easeOut',
                  onUpdate: () => { scene.drawArcSlash(mainGfx, progress_.t, 0.95); scene.drawFireDistortion(fireGfx, progress_.t) },
                  onComplete: () => {
                    scene.spawnAttackParticles(1, 15)
                    scene.tweens.add({ targets: mainGfx, alpha: 0, duration: 100, onComplete: () => mainGfx.destroy() })
                    scene.tweens.add({ targets: fireGfx, alpha: 0, duration: 150, onComplete: () => { fireGfx.destroy(); scene._attackOnCooldown = false } })
                  }
                })
              }
              // 发送攻击请求到后端
              ;(async () => {
                try {
                  const res = await fetch('/api/attack', { method: 'POST', headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({ attackType: isShiftMove ? 'pierce' : 'sweep', playerX: scene.player.x, playerY: scene.player.y, facingAngle: scene.facingAngle, monsters: monsterPositions }) })
                  const j = await res.json(); emit('update', j)
                  if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) scene.renderRoom(j.data)
                  if (j && j.message && j.message.includes('游戏结束')) scene.showGameOver()
                } catch (e) { emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null }) }
              })()
            }
            scene._jHeldSince = null
          }

          // 开始蓄力：J键持续按住超过阈值后才进入蓄力状态
          try {
            if (scene.keys.J && scene.keys.J.isDown && !scene._chargeAttack.active && !scene._attackOnCooldown
                && !scene._waveCharging.active && !scene._chargeAttackPendingSend
                && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay
                && scene._jHeldSince && (chargeNowMs - scene._jHeldSince) >= CHARGE_ENTRY_THRESHOLD) {
              scene._chargeAttack.active = true
              scene._chargeAttack.startTime = chargeNowMs
              scene._chargeAttack.charged = false
              scene._jHeldSince = null  // 清空，避免干扰
            }
          } catch (e) {}

          // 蓄力中处理
          if (scene._chargeAttack.active) {
            try {
              const jDown = scene.keys.J && scene.keys.J.isDown
              const elapsed = chargeNowMs - scene._chargeAttack.startTime
              const progress = Math.min(1, elapsed / CHARGE_ATTACK_DURATION)

              if (!jDown) {
                // J松开
                if (scene._chargeAttack.charged) {
                  // 满蓄力松开 → 发射蓄力攻击
                  if (!scene._chargeAttackPendingSend) {
                    scene._chargeAttackPendingSend = true
                    // 清理蓄力UI
                    try {
                      if (scene._chargeAttack.chargeBarGfx) { scene._chargeAttack.chargeBarGfx.destroy(); scene._chargeAttack.chargeBarGfx = null }
                      if (scene._chargeAttack.circleGfx) { scene._chargeAttack.circleGfx.destroy(); scene._chargeAttack.circleGfx = null }
                    } catch (e) {}

                    const CHARGE_VFX_RADIUS = 150
                    const vfxGfx = scene.add.graphics().setDepth(15)

                    const drawChargeCircle = (gfx, alpha) => {
                      gfx.clear()
                      gfx.lineStyle(4, 0xFFD700, alpha)
                      gfx.strokeCircle(scene.player.x, scene.player.y, CHARGE_VFX_RADIUS)
                      const segs = 96
                      const outer = [], inner = []
                      for (let s = 0; s <= segs; s++) {
                        const a = (s / segs) * Math.PI * 2
                        outer.push({ x: scene.player.x + Math.cos(a) * CHARGE_VFX_RADIUS, y: scene.player.y + Math.sin(a) * CHARGE_VFX_RADIUS })
                      }
                      for (let s = segs; s >= 0; s--) {
                        const a = (s / segs) * Math.PI * 2
                        inner.push({ x: scene.player.x + Math.cos(a) * CHARGE_VFX_RADIUS * 0.35, y: scene.player.y + Math.sin(a) * CHARGE_VFX_RADIUS * 0.35 })
                      }
                      gfx.fillStyle(0xFF6600, alpha * 0.55)
                      gfx.fillPoints(outer.concat(inner), true)
                    }

                    drawChargeCircle(vfxGfx, 1)
                    const shockwave1 = scene.add.circle(scene.player.x, scene.player.y, 20, 0xffffff, 0).setDepth(16)
                    shockwave1.setStrokeStyle(3, 0xFF8800)
                    scene.tweens.add({
                      targets: shockwave1, radius: CHARGE_VFX_RADIUS * 1.15, alpha: 0, duration: 250, ease: 'Cubic.easeOut',
                      onUpdate: () => { shockwave1.setStrokeStyle(3, 0xFF8800, shockwave1.alpha) },
                      onComplete: () => { shockwave1.destroy() }
                    })

                    // 60ms后第二圈
                    scene.time.delayedCall(60, () => {
                      const vfxGfx2 = scene.add.graphics().setDepth(15)
                      const drawChargeCircle2 = (gfx, alpha) => {
                        gfx.clear()
                        gfx.lineStyle(3, 0xFFAA00, alpha)
                        gfx.strokeCircle(scene.player.x, scene.player.y, CHARGE_VFX_RADIUS)
                        const segs = 96
                        const outer = [], inner = []
                        for (let s = 0; s <= segs; s++) {
                          const a = (s / segs) * Math.PI * 2
                          outer.push({ x: scene.player.x + Math.cos(a) * CHARGE_VFX_RADIUS, y: scene.player.y + Math.sin(a) * CHARGE_VFX_RADIUS })
                        }
                        for (let s = segs; s >= 0; s--) {
                          const a = (s / segs) * Math.PI * 2
                          inner.push({ x: scene.player.x + Math.cos(a) * CHARGE_VFX_RADIUS * 0.35, y: scene.player.y + Math.sin(a) * CHARGE_VFX_RADIUS * 0.35 })
                        }
                        gfx.fillStyle(0xFF4400, alpha * 0.45)
                        gfx.fillPoints(outer.concat(inner), true)
                      }
                      drawChargeCircle2(vfxGfx2, 1)
                      const shockwave2 = scene.add.circle(scene.player.x, scene.player.y, 20, 0xffffff, 0).setDepth(16)
                      shockwave2.setStrokeStyle(3, 0xFF6600)
                      scene.tweens.add({
                        targets: shockwave2, radius: CHARGE_VFX_RADIUS * 1.15, alpha: 0, duration: 250, ease: 'Cubic.easeOut',
                        onUpdate: () => { shockwave2.setStrokeStyle(3, 0xFF6600, shockwave2.alpha) },
                        onComplete: () => { shockwave2.destroy() }
                      })
                      scene.tweens.add({
                        targets: vfxGfx2, alpha: 0, duration: 350, delay: 100,
                        onComplete: () => { try { vfxGfx2.destroy() } catch (e) {} }
                      })
                    })

                    scene.tweens.add({
                      targets: vfxGfx, alpha: 0, duration: 500, delay: 50,
                      onComplete: () => { try { vfxGfx.destroy() } catch (e) {} }
                    })

                    if (scene.cameras && scene.cameras.main) {
                      scene.cameras.main.shake(200, 0.01)
                    }

                    // 火花粒子
                    for (let i = 0; i < 25; i++) {
                      const ang = Math.random() * Math.PI * 2
                      const dist = CHARGE_VFX_RADIUS * (0.3 + Math.random() * 0.7)
                      const px = scene.player.x + Math.cos(ang) * dist
                      const py = scene.player.y + Math.sin(ang) * dist
                      const spark = scene.add.circle(px, py, 1.5 + Math.random() * 3, Math.random() < 0.4 ? 0xFFEE88 : 0xFF4400).setDepth(17)
                      scene.tweens.add({
                        targets: spark, x: px + Math.cos(ang) * (60 + Math.random() * 60),
                        y: py + Math.sin(ang) * (60 + Math.random() * 60),
                        alpha: 0, scale: 0.1, duration: 200 + Math.random() * 300,
                        ease: 'Cubic.easeOut',
                        onComplete: () => { try { spark.destroy() } catch (e) {} }
                      })
                    }

                    // 发送蓄力攻击请求到后端
                    const monsterPositions = []
                    for (const mon of scene.monstersData) {
                      if (mon && mon.name && !mon.exploding) {
                        monsterPositions.push({ name: mon.name, x: mon.x, y: mon.y })
                      }
                    }
                    ;(async () => {
                      try {
                        const res = await fetch('/api/attack', {
                          method: 'POST', headers: { 'Content-Type': 'application/json' },
                          body: JSON.stringify({ attackType: 'charged',
                            playerX: scene.player.x, playerY: scene.player.y,
                            facingAngle: scene.facingAngle, monsters: monsterPositions })
                        })
                        const j = await res.json()
                        emit('update', j)
                        if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                          scene.renderRoom(j.data)
                        }
                        if (j && j.message && j.message.includes('游戏结束')) { scene.showGameOver() }
                      } catch (e) {
                        emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                      }
                      scene._chargeAttackPendingSend = false
                    })()
                  }
                } else {
                  // 未满蓄力松开 → 取消
                  try {
                    if (scene._chargeAttack.chargeBarGfx) { scene._chargeAttack.chargeBarGfx.destroy(); scene._chargeAttack.chargeBarGfx = null }
                    if (scene._chargeAttack.circleGfx) { scene._chargeAttack.circleGfx.destroy(); scene._chargeAttack.circleGfx = null }
                  } catch (e) {}
                }
                scene._chargeAttack.active = false
                scene._chargeAttack.charged = false
              } else if (!scene._chargeAttack.charged) {
                // 蓄力中（未满）→ 绘制进度条和蓄力圈
                if (!scene._chargeAttack.chargeBarGfx) { scene._chargeAttack.chargeBarGfx = scene.add.graphics().setDepth(150) }
                if (!scene._chargeAttack.circleGfx) { scene._chargeAttack.circleGfx = scene.add.graphics().setDepth(14) }
                const barGfx = scene._chargeAttack.chargeBarGfx
                const circleGfx = scene._chargeAttack.circleGfx

                barGfx.clear()
                const barW2 = 50, barH2 = 8
                const barX2 = scene.player.x - barW2 / 2
                const barY2 = scene.player.y + 30
                barGfx.fillStyle(0x222222, 0.8)
                barGfx.fillRect(barX2, barY2, barW2, barH2)
                barGfx.lineStyle(1, 0xaa6600, 0.9)
                barGfx.strokeRect(barX2, barY2, barW2, barH2)
                const fR = Math.floor(180 + progress * 75), fG = Math.floor(80 + progress * 100), fB = 20
                barGfx.fillStyle((fR << 16) | (fG << 8) | fB, 0.9)
                barGfx.fillRect(barX2, barY2, barW2 * progress, barH2)

                circleGfx.clear()
                const ca = 0.15 + progress * 0.4
                circleGfx.fillStyle(0xFF6600, ca * 0.3)
                circleGfx.fillCircle(scene.player.x, scene.player.y, 150)
                circleGfx.lineStyle(1.5, 0xFF8800, ca)
                circleGfx.strokeCircle(scene.player.x, scene.player.y, 150)
                for (let d = 0; d < 24; d++) {
                  circleGfx.beginPath()
                  circleGfx.arc(scene.player.x, scene.player.y, 150 * (0.5 + progress * 0.5),
                    (d / 24) * Math.PI * 2, ((d + 0.4) / 24) * Math.PI * 2, false)
                  circleGfx.strokePath()
                }
                if (progress >= 1) { scene._chargeAttack.charged = true }
              } else {
                // 已满蓄力 → 保持满蓄力UI脉冲
                if (!scene._chargeAttack.chargeBarGfx) { scene._chargeAttack.chargeBarGfx = scene.add.graphics().setDepth(150) }
                if (!scene._chargeAttack.circleGfx) { scene._chargeAttack.circleGfx = scene.add.graphics().setDepth(14) }
                const barGfx = scene._chargeAttack.chargeBarGfx
                const circleGfx = scene._chargeAttack.circleGfx

                barGfx.clear()
                const barW3 = 50, barH3 = 8
                const barX3 = scene.player.x - barW3 / 2
                const barY3 = scene.player.y + 30
                barGfx.fillStyle(0x222222, 0.8)
                barGfx.fillRect(barX3, barY3, barW3, barH3)
                barGfx.lineStyle(2, 0xFF4400, 1.0)
                barGfx.strokeRect(barX3, barY3, barW3, barH3)
                barGfx.fillStyle(0xFF4400, 0.5 + 0.5 * Math.sin(chargeNowMs * 0.01))
                barGfx.fillRect(barX3, barY3, barW3, barH3)

                circleGfx.clear()
                const pa = 0.5 + 0.3 * Math.sin(chargeNowMs * 0.008)
                circleGfx.fillStyle(0xFF6600, pa * 0.35)
                circleGfx.fillCircle(scene.player.x, scene.player.y, 150)
                circleGfx.lineStyle(2.5, 0xFFD700, pa)
                circleGfx.strokeCircle(scene.player.x, scene.player.y, 150)
                for (let d = 0; d < 24; d++) {
                  circleGfx.beginPath()
                  circleGfx.arc(scene.player.x, scene.player.y, 150,
                    (d / 24 + chargeNowMs * 0.0005) * Math.PI * 2,
                    ((d + 0.4) / 24 + chargeNowMs * 0.0005) * Math.PI * 2, false)
                  circleGfx.strokePath()
                }
              }
            } catch (e) {}
          }

          // 玩家边界限制
          const pr2 = scene.playerRadius || 14
          scene.player.x = Phaser.Math.Clamp(scene.player.x, rb.left + pr2, rb.right - pr2)
          scene.player.y = Phaser.Math.Clamp(scene.player.y, rb.top + pr2, rb.bottom - pr2)

          // ---------- 更新怪物血条位置 ----------
          for (const mon of scene.monstersData) {
            if (!mon || !mon.hpBarBg) continue
            const hpBarX = mon.x - mon.hpBarW / 2
            const hpBarY = mon.y - 20 - mon.hpBarH - 30
            const ratio = Math.max(0, Math.min(1, (mon.hp || 0) / Math.max(1, mon.maxHp || 1)))
            mon.hpBarBg.setPosition(hpBarX + mon.hpBarW / 2, hpBarY + mon.hpBarH / 2)
            let color = 0x44cc44
            if (ratio < 0.3) color = 0xcc2222
            else if (ratio < 0.6) color = 0xccaa22
            mon.hpBarFill.setFillStyle(color)
            mon.hpBarFill.setPosition(hpBarX, hpBarY)
            mon.hpBarFill.setDisplaySize(mon.hpBarW * ratio, mon.hpBarH)
            if (mon.hpNumText) {
              mon.hpNumText.setPosition(hpBarX + mon.hpBarW / 2, hpBarY + mon.hpBarH / 2)
              mon.hpNumText.setText((mon.hp || 0) + '/' + (mon.maxHp || 0))
            }
          }

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
                      if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                        scene.renderRoom(j.data)
                      }
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
          // 检查当前房间是否有存活怪物 — 有则禁止出门
          const hasAliveMonsters = scene.monstersData && scene.monstersData.some(m => m && m.hp > 0)
          let insideAnyDoor = false
          if (scene.doorRects && !scene._waveCharging.active) {
            for (const dr of scene.doorRects) {
              const b = dr.rect.getBounds()
              if (scene.player.x >= b.left && scene.player.x <= b.right && scene.player.y >= b.top && scene.player.y <= b.bottom) {
                insideAnyDoor = true
                // 有存活怪物时不允许通过门（突刺也可能会冲到门区域）
                if (hasAliveMonsters) {
                  // 将玩家推回房间内（沿门方向的反向）
                  const pushBack = 10
                  const opp = { north: { x: 0, y: pushBack }, south: { x: 0, y: -pushBack }, west: { x: pushBack, y: 0 }, east: { x: -pushBack, y: 0 } }
                  const p = opp[dr.dir] || { x: 0, y: 0 }
                  scene.player.x += p.x
                  scene.player.y += p.y
                } else if (scene.lastDoorEntered !== dr.dir) {
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

          // ---------- 掉落物交互检测 ----------
          const DROP_PICKUP_RANGE = 45
          scene._closestDropItem = null
          if (scene.droppedItemsData && scene.droppedItemsData.length > 0) {
            let closestDist = Infinity
            for (const drop of scene.droppedItemsData) {
              const dx = scene.player.x - drop.x
              const dy = scene.player.y - drop.y
              const dist = Math.sqrt(dx * dx + dy * dy)
              drop.nearPlayer = dist < DROP_PICKUP_RANGE
              if (drop.nearPlayer && dist < closestDist) {
                closestDist = dist
                scene._closestDropItem = drop
              }
            }
          }
          // 高亮最近的可拾取掉落物
          if (scene.droppedItemsData) {
            for (const drop of scene.droppedItemsData) {
              if (drop.icon && drop.icon.scene) {
                if (drop === scene._closestDropItem) {
                  drop.icon.setStrokeStyle(3, 0xFFD700) // 金色高亮
                  drop.icon.setScale(1.3)
                } else {
                  drop.icon.setStrokeStyle(2, 0xffffff)
                  drop.icon.setScale(1.0)
                }
              }
            }
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

          // SPACE 键交互（拾取 > 商店 > 祭坛, skip if charging）
          // ---- 1 键测试：施加一层中毒 ----
          try {
            if (scene.keys.ONE && Phaser.Input.Keyboard.JustDown(scene.keys.ONE) && !scene._waveCharging.active) {
              ;(async () => {
                try {
                  const res = await fetch('/api/command', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: 'test poison' })
                  })
                  const j = await res.json()
                  emit('update', j)
                  if (j && j.data) scene.renderRoom(j.data)
                } catch (e) {
                  emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                }
              })()
            }
          } catch (e) {}

          // ---- 2 键测试：施加一层烧伤 ----
          try {
            if (scene.keys.TWO && Phaser.Input.Keyboard.JustDown(scene.keys.TWO) && !scene._waveCharging.active) {
              ;(async () => {
                try {
                  const res = await fetch('/api/command', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: 'test burn' })
                  })
                  const j = await res.json()
                  emit('update', j)
                  if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                    scene.renderRoom(j.data)
                  }
                } catch (e) {
                  emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                }
              })()
            }
          } catch (e) {}
          // ---- 3 键测试：施加一层流血 ----
          try {
            if (scene.keys.THREE && Phaser.Input.Keyboard.JustDown(scene.keys.THREE) && !scene._waveCharging.active) {
              ;(async () => {
                try {
                  const res = await fetch('/api/command', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: 'test bleed' })
                  })
                  const j = await res.json()
                  emit('update', j)
                  if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                    scene.renderRoom(j.data)
                  }
                } catch (e) {
                  emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                }
              })()
            }
          } catch (e) {}

          // ---- 互动键 (SPACE) ----
          try {
            if (scene.keys.SPACE && Phaser.Input.Keyboard.JustDown(scene.keys.SPACE) && !scene._waveCharging.active) {
              if (scene._closestDropItem) {
                // 拾取掉落物
                const dropItem = scene._closestDropItem
                ;(async () => {
                  try {
                    const res = await fetch('/api/command', {
                      method: 'POST', headers: { 'Content-Type': 'application/json' },
                      body: JSON.stringify({ command: 'pickup ' + dropItem.itemName })
                    })
                    const j = await res.json()
                    emit('update', j)
                    if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
                      scene.renderRoom(j.data)
                    }
                  } catch (e) {
                    emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                  }
                })()
              } else if (nearShopNpc) {
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
                    if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
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
          if (j && j.data && !scene.shopMenuOverlay && !scene.shopBuyOverlay && !scene.shopSellOverlay && !scene.wisdomOverlay) {
            scene.renderRoom(j.data)
          }
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

  // 监听游戏重置事件，重新获取地图数据实现小地图同步更新
  window.addEventListener('game:reset', initMinimap)

  // 注册 B 键背包全局监听
  window.addEventListener('keydown', onKeyDown, true)
})

onBeforeUnmount(() => {
  if (game) {
    try { game.destroy(true) } catch (e) {}
    game = null
  }
  window.removeEventListener('minimap:update', onMinimapUpdate)
  window.removeEventListener('game:reset', initMinimap)
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

/* 已佩戴徽章 */
.slot-equipped-badge {
  position: absolute;
  bottom: 3px;
  left: 3px;
  font-size: 9px;
  color: #00ff88;
  text-shadow: 0 0 6px rgba(0, 255, 136, 0.6);
  font-weight: bold;
  background: rgba(0, 40, 20, 0.7);
  padding: 1px 4px;
  border-radius: 3px;
  border: 1px solid rgba(0, 255, 136, 0.4);
  z-index: 5;
}

/* 卸下按钮 */
.btn-unequip {
  padding: 8px 28px;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(180deg, #ff8800 0%, #cc6600 100%);
  color: #ffffff;
  border: 1px solid #cc7700;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
.btn-unequip:hover {
  background: linear-gradient(180deg, #ffaa33 0%, #dd7700 100%);
  box-shadow: 0 0 12px rgba(255, 136, 0, 0.5);
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
  color: #667766;
  border-color: rgba(100, 130, 100, 0.15);
  cursor: not-allowed;
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