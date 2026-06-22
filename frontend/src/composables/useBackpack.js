/**
 * 背包功能全自治 Composable（单例模式）
 *
 * 管理背包的全部状态、API 调用、事件分发。
 * 所有组件通过 `useBackpack()` 获取同一个实例。
 *
 * 用法：
 *   import { useBackpack } from '../composables/useBackpack.js'
 *   const backpack = useBackpack()
 *   // backpack.visible, backpack.items, backpack.toggle(), ...
 */

import { ref, computed, reactive } from 'vue'

// ==================== 模块级单例状态 ====================
const visible = ref(false)
const items = ref([]) // [{ itemId, name, rarity, functionDesc, loreDesc, quantity, equipped, ... }]
const selectedSlot = ref(null) // 选中格子索引 (0-14) 或 null
const hoveredSlot = ref(null) // 鼠标悬停格子索引

// ==================== 计算属性 ====================
const selectedItem = computed(() => {
  if (selectedSlot.value === null) return null
  return selectedSlot.value < items.value.length ? items.value[selectedSlot.value] : null
})

// ==================== 私有工具函数 ====================

/** 稀有度 → 颜色 */
function rarityColor(rarity) {
  switch (rarity) {
    case 'legendary':
      return '#FF6600'
    case 'epic':
      return '#CC44FF'
    case 'rare':
      return '#4488FF'
    default:
      return '#FFD700' // common → 金色
  }
}

/** 判断是否为饰品（可佩戴/卸下） */
function isAccessory(item) {
  if (!item || !item.name) return false
  const name = item.name
  return name.includes('暗影披风') || name.includes('生命戒指') || name.includes('元素项链')
}

/** 获取指定格子的物品 */
function getSlotItem(slotIndex) {
  return slotIndex < items.value.length ? items.value[slotIndex] : null
}

/** 选中格子 */
function selectSlot(slotIndex) {
  const item = getSlotItem(slotIndex)
  if (item) {
    selectedSlot.value = slotIndex
  } else {
    selectedSlot.value = null
  }
}

// ==================== 公开方法 ====================

/** 从后端刷新背包数据 */
async function refresh() {
  try {
    const res = await fetch('/api/backpack')
    const j = await res.json()
    const data = j?.data?.backpack || []
    if (data.length > 0) {
      items.value = data
      console.log(
        '[Backpack] items loaded:',
        data.length,
        data.map((it) => ({ name: it.name, rarity: it.rarity, qty: it.quantity })),
      )
    }
  } catch (e) {
    console.warn('[Backpack] 无法获取背包数据', e)
  }
}

/** 打开背包（刷新数据并重置选择） */
async function open() {
  await refresh()
  selectedSlot.value = null
  hoveredSlot.value = null
}

/** 关闭背包 */
function close() {
  visible.value = false
  window.dispatchEvent(new CustomEvent('backpack:toggle', { detail: { visible: false } }))
}

/** 切换背包显示/隐藏 */
async function toggle() {
  visible.value = !visible.value
  if (visible.value) {
    await open()
  }
  window.dispatchEvent(new CustomEvent('backpack:toggle', { detail: { visible: visible.value } }))
}

/**
 * 使用物品（饰品则为佩戴）
 * 向后端发送 bag use 命令，更新本地状态，通知游戏场景。
 */
async function useItem() {
  const item = selectedItem.value
  if (!item) return

  try {
    const res = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: 'bag use ' + item.name }),
    })
    const j = await res.json()

    // 通知游戏场景更新
    window.dispatchEvent(new CustomEvent('game:update', { detail: j }))

    // 更新本地背包数据
    if (j?.data?.backpack) {
      items.value = j.data.backpack
    }

    // 保持当前选中（如果物品仍然存在）
    const stillExists = items.value.find((it) => it.name === item.name)
    if (!stillExists) {
      selectedSlot.value = null
    } else {
      const idx = items.value.indexOf(stillExists)
      selectedSlot.value = idx
    }
  } catch (e) {
    console.warn('使用物品失败', e)
  }
}

/** 卸下饰品 */
async function unequipItem() {
  const item = selectedItem.value
  if (!item) return

  try {
    const res = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: 'bag unequip ' + item.name }),
    })
    const j = await res.json()

    window.dispatchEvent(new CustomEvent('game:update', { detail: j }))

    if (j?.data?.backpack) {
      items.value = j.data.backpack
    }

    // 保持选中
    const stillExists = items.value.find((it) => it.name === item.name)
    if (stillExists) {
      const idx = items.value.indexOf(stillExists)
      selectedSlot.value = idx
    }
  } catch (e) {
    console.warn('卸下饰品失败', e)
  }
}

/** 丢弃物品 */
async function discardItem() {
  const item = selectedItem.value
  if (!item) return
  if (!confirm('确定要丢弃全部 "' + item.name + '" 吗？')) return

  try {
    const res = await fetch('/api/command', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: 'bag discard ' + item.name }),
    })
    const j = await res.json()

    window.dispatchEvent(new CustomEvent('game:update', { detail: j }))

    if (j?.data?.backpack) {
      items.value = j.data.backpack
    }

    selectedSlot.value = null
  } catch (e) {
    console.warn('丢弃物品失败', e)
  }
}

// ==================== 单例导出 ====================

let _instance = null

export function useBackpack() {
  if (_instance) return _instance

  _instance = reactive({
    // 响应式状态（reactive 自动解包嵌套 ref）
    visible,
    items,
    selectedSlot,
    hoveredSlot,
    selectedItem,

    // 方法
    toggle,
    open,
    close,
    refresh,
    useItem,
    unequipItem,
    discardItem,

    // 工具函数
    getSlotItem,
    selectSlot,
    isAccessory,
    rarityColor,
  })

  return _instance
}
