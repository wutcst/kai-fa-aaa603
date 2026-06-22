/**
 * GameCanvas 全局常量集中管理
 * 提取自 GameCanvas.vue 中分散的硬编码常量
 */

// ==================== 攻击系统配置 ====================
export const ATTACK_CONFIG = {
  SWEEP: {
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
  },
  PIERCE: {
    distance: 120,
    distanceExpand: 1.15,
    duration: 100,
    fade: 180,
    width: 14,
    invincibilityMs: 180,
  },
  CHARGE: {
    duration: 1000,
    entryThreshold: 200,
    vfxRadius: 150,
  },
}

// ==================== 月光波配置 ====================
export const WAVE_CONFIG = {
  chargeDuration: 1000,
  mpCost: 20,
  projectileSpeed: 420,
  maxBounces: 2,
  projectileRadius: 27,
  collisionRangeExtra: 30,
  knockbackForce: 40,
  trailMaxLength: 12,
}

// ==================== 风隐配置 ====================
export const WIND_CLOAK_CONFIG = {
  chargeDuration: 1000,
  mpPerSec: 2,
  speedMultiplier: 2,
  playerAlpha: 0.4,
}

// ==================== 寒冰风暴配置 ====================
export const ICE_STORM_CONFIG = {
  chargeDuration: 1500,
  mpCost: 25,
  slowDuration: 10000,
  hitCount: 3,
  damageRatio: 1.0,
  vfxRadius: 400,
}

// ==================== 怪物 AI 配置 ====================
export const MONSTER_CONFIG = {
  DETECT_RANGE: 350,
  ATTACK_RANGE: 50,
  ATTACK_COOLDOWN: 1200,
  ELITE_DETECT_RANGE: 500,
  ELITE_ATTACK_RANGE: 60,
  ELITE_ATTACK_COOLDOWN: 1000,
  BOSS_ATTACK_RANGE: 75,
  BOSS_ATTACK_COOLDOWN: 750,
  // 特殊普通怪物参数（后端下发的个体值优先）
  WEREWOLF_ATTACK_RANGE: 35,
  WEREWOLF_ATTACK_COOLDOWN: 600,
  OGRE_ATTACK_COOLDOWN: 2000,
  SLIME_ATTACK_COOLDOWN: 1000,
  COLLISION_RADIUS: 20,
  PIERCE_PUSH_DISTANCE: 55,
  PIERCE_PUSH_DURATION: 130,
  WAVE_KNOCKBACK_DURATION: 150,
}

// ==================== 玩家配置 ====================
export const PLAYER_CONFIG = {
  radius: 10,
  boundsRadius: 14,
  baseMoveSpeed: 160,
  chargeMoveSpeedMultiplier: 0.5,
  shiftSpeedMultiplier: 2,
}

// ==================== 交互距离 ====================
export const INTERACT_RANGES = {
  ALTAR: 50,
  SHOP: 50,
  ENCOUNTER: 50,
  DROP_PICKUP: 45,
}

// ==================== 玩家状态条配置 ====================
export const PLAYER_BAR_CONFIG = {
  barX: 570,
  barW: 210,
  barH: 18,
  hpY: 15,
  mpY: 39,
  depth: 100,
  moneyIconX: 455,
  moneyIconY: 23,
  moneyTextX: 475,
  moneyTextY: 24,
  hpLabelX: 540,
  mpLabelX: 540,
  hpColors: { high: 0x22aa22, medium: 0xccaa22, low: 0xcc2222 },
  mpColor: 0x2244cc,
}

// ==================== 怪物血条配置 ====================
export const MONSTER_HP_BAR_CONFIG = {
  barW: 54,
  barH: 8,
  yOffset: -58,
  colors: { high: 0x44cc44, medium: 0xccaa22, low: 0xcc2222 },
  thresholds: { low: 0.3, medium: 0.6 },
}

// ==================== 房间类型颜色（小地图） ====================
export const ROOM_TYPE_COLORS = {
  START_HALL: { fill: 'rgba(255, 215, 0, 0.85)', stroke: '#FFD700' },
  SHOP: { fill: 'rgba(0, 191, 255, 0.75)', stroke: '#00BFFF' },
  ENCOUNTER: { fill: 'rgba(186, 85, 211, 0.75)', stroke: '#BA55D3' },
  CAMPFIRE: { fill: 'rgba(255, 140, 0, 0.75)', stroke: '#FF8C00' },
  BOSS: { fill: 'rgba(220, 20, 60, 0.80)', stroke: '#DC143C' },
  ELITE_MONSTER: { fill: 'rgba(255, 99, 71, 0.75)', stroke: '#FF6347' },
  NORMAL_MONSTER: { fill: 'rgba(100, 149, 237, 0.7)', stroke: 'rgba(255,255,255,0.8)' },
  CURRENT_ROOM: { fill: 'rgba(255, 215, 0, 0.9)', stroke: '#FFD700' },
}

// ==================== Buff/Debuff 显示配置 ====================
export const BUFF_DISPLAY_CONFIG = {
  typeOrder: { BURN: 0, POISON: 1, BLEED: 2 },
  cellWidth: 40,
  startX: 580,
  cellCYOffset: 6,
  burnIcon: '🔥',
  poisonIcon: '💀',
  bleedIcon: '🩸',
  burnFlashThreshold: 3,
  poisonFlashThreshold: 1,
  iconFontSize: '20px Arial',
  layersFont: 'bold 14px Arial',
  timerFont: '10px Arial',
}

// ==================== 草地纹理配置 ====================
export const GRASS_CONFIG = {
  tileSize: 128,
  farSeed: 12345,
  farDensity: 260,
  nearSeed: 54321,
  nearDensity: 200,
  nearAlpha: 0.88,
  nearScale: 1.02,
}

// ==================== 背景图映射 ====================
export const BG_ROOM_TYPE_MAPPING = {
  START_HALL: 'bg0',
  SHOP: 'bg1',
  ENCOUNTER: 'bg2',
  BOSS: 'bg3',
}

export const BG_CONFIG = { maxBgCount: 4, parallax: { farFactor: 0.35, nearFactor: 0.72 } }

// ==================== 小地图配置 ====================
export const MINIMAP_CONFIG = {
  width: 160,
  height: 160,
  margin: 14,
  maxCellSize: 36,
  rectSizeRatio: 0.75,
  dirVec: {
    north: { dx: 0, dy: -1 },
    south: { dx: 0, dy: 1 },
    west: { dx: -1, dy: 0 },
    east: { dx: 1, dy: 0 },
  },
}

// ==================== 游戏画布配置 ====================
export const CANVAS_CONFIG = {
  width: 800,
  height: 600,
  backgroundColor: '#333333',
  playerStartX: 400,
  playerStartY: 320,
}

// ==================== 门配置 ====================
export const DOOR_CONFIG = {
  hW: 120,
  hH: 40,
  vW: 40,
  vH: 120,
  outsideOffset: -10,
  fillColor: 0x664422,
  strokeColor: 0x222222,
}

// ==================== 房间渲染配置 ====================
export const ROOM_RENDER_CONFIG = {
  defaultWidth: 650,
  defaultHeight: 450,
  strokeColor: 0xffffff,
  strokeAlpha: 0.9,
  fillColor: 0x000000,
  fillAlpha: 0.06,
  darkBgColor: 0x111111,
}

// ==================== 物品渲染配置 ====================
export const ITEM_RENDER_CONFIG = {
  startX: 520,
  startY: 360,
  spacingX: 60,
  spacingY: 60,
  cols: 2,
  circleRadius: 20,
  circleColor: 0x8b5a2b,
}

// ==================== 怪物渲染配置 ====================
export const MONSTER_RENDER_CONFIG = {
  scale: 1.5,
  labelYOffset: 34,
  // 房间边距（怪物不靠近墙壁和门区域）
  marginX: 100,
  marginY: 110,
  // 列数规则：[怪物数区间] → 列数，按上限匹配
  colRules: [
    { maxCount: 2, cols: 2 },
    { maxCount: 3, cols: 3 },
    { maxCount: 5, cols: 3 },
    { maxCount: Infinity, cols: 4 },
  ],
  // 抖动半径占格子尺寸的比例上限
  jitterRatio: 0.3,
}

// ==================== 祭坛渲染配置 ====================
export const ALTAR_RENDER_CONFIG = {
  size: 36,
  spacing: 100,
  order: ['HEAL', 'TRAIN', 'WISDOM'],
  usedGrayColor: 0x666666,
  inactiveStroke: 0x444444,
  activeStroke: 0xffffff,
}

// ==================== 轮询配置 ====================
export const POLL_CONFIG = { combatInterval: 500, idleInterval: 2000 }

// ==================== 特效配置 ====================
export const EFFECTS_CONFIG = {
  altarGlow: { ringCount: 4, defaultDuration: 5000 },
  cameraShake: {
    sweep: { duration: 120, intensity: 0.005 },
    wave: { duration: 180, intensity: 0.008 },
    chargeAttack: { duration: 200, intensity: 0.01 },
  },
}

// ==================== 浮层深度配置 ====================
export const OVERLAY_CONFIG = { gameOver: { depth: 9999 } }
