<template>
  <div v-if="backpack.visible" class="backpack-overlay" @click.self="backpack.close">
    <div class="backpack-panel">
      <!-- 标题栏 -->
      <div class="backpack-header">
        <span class="backpack-title">🎒 背包</span>
        <span class="backpack-close" @click="backpack.close">✕</span>
      </div>

      <div class="backpack-body">
        <!-- 左侧：5x3 物品格子 -->
        <div class="backpack-left">
          <div
            v-for="i in 15"
            :key="'slot-' + i"
            class="backpack-slot"
            :class="{
              'has-item': backpack.getSlotItem(i - 1),
              'slot-selected': backpack.selectedSlot === i - 1,
              'slot-hovered': backpack.hoveredSlot === i - 1,
            }"
            @click="backpack.selectSlot(i - 1)"
            @mouseenter="backpack.hoveredSlot = i - 1"
            @mouseleave="backpack.hoveredSlot = null"
          >
            <!-- 物品简略图标 -->
            <!-- 生命浆果：三个红色重叠圆点 -->
            <div
              v-if="
                backpack.getSlotItem(i - 1) && backpack.getSlotItem(i - 1).name.includes('生命浆果')
              "
              class="slot-icon slot-icon-lifeberry"
            >
              <span class="berry-dot" style="left: 10px; top: 14px"></span>
              <span class="berry-dot" style="left: 20px; top: 10px"></span>
              <span class="berry-dot" style="left: 28px; top: 18px"></span>
            </div>
            <!-- 魔力浆果：三个蓝色重叠椭圆 -->
            <div
              v-else-if="
                backpack.getSlotItem(i - 1) && backpack.getSlotItem(i - 1).name.includes('魔力浆果')
              "
              class="slot-icon slot-icon-manaberry"
            >
              <span class="mana-ellipse" style="left: 10px; top: 16px"></span>
              <span class="mana-ellipse" style="left: 18px; top: 10px"></span>
              <span class="mana-ellipse" style="left: 26px; top: 18px"></span>
            </div>
            <!-- 其他物品：显示名称首字母 -->
            <div v-else-if="backpack.getSlotItem(i - 1)" class="slot-icon">
              {{ backpack.getSlotItem(i - 1).name.charAt(0) }}
            </div>
            <!-- 已佩戴标记 -->
            <span
              v-if="backpack.getSlotItem(i - 1) && backpack.getSlotItem(i - 1).equipped"
              class="slot-equipped-badge"
              >已佩戴</span
            >
            <!-- 物品数量角标 -->
            <span
              v-if="backpack.getSlotItem(i - 1) && !backpack.getSlotItem(i - 1).equipped"
              class="slot-qty"
              >x{{ backpack.getSlotItem(i - 1).quantity }}</span
            >
          </div>
        </div>

        <!-- 右侧：物品详情 -->
        <div class="backpack-right">
          <!-- 上方大图像框 -->
          <div class="detail-image-frame">
            <span v-if="!backpack.selectedItem" class="placeholder-text">选择物品查看详情</span>
            <div v-else class="detail-image-placeholder">
              {{ backpack.selectedItem.name.charAt(0) }}
            </div>
          </div>

          <!-- 下方物品信息 -->
          <div class="detail-info">
            <template v-if="backpack.selectedItem">
              <!-- 第一行：名称 + 编号 + 数量 -->
              <div class="detail-row1">
                <span
                  class="detail-name"
                  :style="{ color: backpack.rarityColor(backpack.selectedItem.rarity) }"
                >
                  {{ backpack.selectedItem.name }}
                </span>
                <span
                  class="detail-id"
                  :style="{ color: backpack.rarityColor(backpack.selectedItem.rarity) }"
                >
                  NO.{{ String(backpack.selectedItem.itemId).padStart(2, '0') }}
                </span>
                <span
                  class="detail-qty"
                  :style="{ color: backpack.rarityColor(backpack.selectedItem.rarity) }"
                >
                  拥有：{{ backpack.selectedItem.quantity }}
                </span>
              </div>
              <!-- 功能描述 -->
              <div class="detail-func">
                {{ backpack.selectedItem.functionDesc }}
              </div>
              <!-- 空行间距 -->
              <div class="detail-spacer"></div>
              <!-- 背景描述（斜体） -->
              <div class="detail-lore">"{{ backpack.selectedItem.loreDesc }}"</div>
            </template>
            <template v-else>
              <div class="detail-empty">← 点击左侧物品查看详情</div>
            </template>
          </div>

          <!-- 操作按钮 -->
          <div class="detail-buttons">
            <!-- 饰品已佩戴 → 显示"卸下"按钮 -->
            <button
              v-if="backpack.selectedItem && backpack.selectedItem.equipped"
              class="btn-unequip"
              @click="backpack.unequipItem"
            >
              卸下
            </button>
            <!-- 饰品未佩戴 → 显示"佩戴"按钮 -->
            <button
              v-else-if="backpack.selectedItem && backpack.isAccessory(backpack.selectedItem)"
              class="btn-use"
              @click="backpack.useItem"
            >
              佩戴
            </button>
            <!-- 普通消耗品 → 显示"使用"按钮 -->
            <button
              v-else
              class="btn-use"
              :disabled="!backpack.selectedItem"
              @click="backpack.useItem"
            >
              使用
            </button>
            <button
              class="btn-discard"
              :disabled="!backpack.selectedItem"
              @click="backpack.discardItem"
            >
              丢弃
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { useBackpack } from '../composables/useBackpack.js'

  const backpack = useBackpack()
</script>

<style scoped>
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
    box-shadow:
      0 0 40px rgba(0, 100, 200, 0.3),
      inset 0 0 20px rgba(0, 100, 200, 0.1);
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
    color: #ffd700;
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
    color: #ffd700;
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
    color: #ffd700;
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
    color: #ffd700;
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
</style>
