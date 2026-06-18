import { reactive } from 'vue'
import { api } from '../api/gameApi.js'

export function useSlotDialog() {
  const slotDialog = reactive({
    visible: false,
    title: '',
    saves: {},      // { 1: {...}, 2: {...}, 3: {...} }
    callback: null, // (slotNumber) => {}
  })

  // 关闭槽位弹窗
  function closeSlotDialog() {
    slotDialog.visible = false
  }

  // 从后端拉取存档列表
  async function fetchSaves() {
    try {
      const list = await api.getSaves()
      const map = {}
      for (const s of list) {
        map[s.id] = s
      }
      return map
    } catch (e) {
      console.error('获取存档列表失败:', e)
      return {}
    }
  }

  // 删除指定槽位的存档
  async function deleteSlot(slotNum) {
    if (!confirm('确定要删除 存档 ' + slotNum + ' 吗？此操作不可恢复！')) return
    try {
      await api.deleteSave(slotNum)
      slotDialog.saves = await fetchSaves()
    } catch (e) {
      alert('删除失败: ' + e.message)
    }
  }

  // 显示保存槽位弹窗
  async function showSaveSlots() {
    slotDialog.saves = await fetchSaves()
    slotDialog.title = '💾 选择一个槽位保存'
    slotDialog.callback = async (slotNum) => {
      closeSlotDialog()
      try {
        await api.save(slotNum)
        alert('游戏已保存到 存档 ' + slotNum + '！')
      } catch (e) {
        alert('保存失败: ' + e.message)
      }
    }
    slotDialog.visible = true
  }

  // 显示读档槽位弹窗
  async function showLoadSlots(onLoaded) {
    slotDialog.saves = await fetchSaves()
    slotDialog.title = '📂 选择一个存档读取'
    slotDialog.callback = async (slotNum) => {
      if (!slotDialog.saves[slotNum]) {
        alert('该存档为空！')
        return
      }
      closeSlotDialog()
      try {
        const data = await api.load(slotNum)
        if (onLoaded) await onLoaded(slotNum, data)
      } catch (e) {
        alert('加载失败: ' + e.message)
      }
    }
    slotDialog.visible = true
  }

  return {
    slotDialog,
    fetchSaves,
    deleteSlot,
    showSaveSlots,
    showLoadSlots,
    closeSlotDialog,
  }
}
