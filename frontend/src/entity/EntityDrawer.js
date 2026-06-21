/**
 * EntityDrawer.js
 * 游戏实体形象绘制模块
 * 
 * 所有实体使用 Phaser.GameObjects.Graphics 在局部原点 (0,0) 绘制，
 * 绘制后通过 gfx.setPosition(x, y) 放置到场景中。
 * 绘制函数签名：drawXxx(gfx, s)，其中 s 为缩放系数。
 * 
 * 参考文档：plans/game-entity-designs.md
 */

// ==================== 工具函数 ====================

/**
 * 绘制一个填充圆
 */
function fillCircle(gfx, x, y, radius, color, alpha = 1) {
  gfx.fillStyle(color, alpha)
  gfx.fillCircle(x, y, radius)
}

/**
 * 绘制一个填充矩形
 */
function fillRect(gfx, x, y, w, h, color, alpha = 1) {
  gfx.fillStyle(color, alpha)
  gfx.fillRect(x, y, w, h)
}

/**
 * 绘制一个描边矩形
 */
function strokeRect(gfx, x, y, w, h, color, alpha = 1, lineWidth = 1) {
  gfx.lineStyle(lineWidth, color, alpha)
  gfx.strokeRect(x, y, w, h)
}

/**
 * 绘制一个填充三角形
 */
function fillTriangle(gfx, x1, y1, x2, y2, x3, y3, color, alpha = 1) {
  gfx.fillStyle(color, alpha)
  gfx.fillTriangle(x1, y1, x2, y2, x3, y3)
}

/**
 * 绘制线条
 */
function lineStyle(gfx, x1, y1, x2, y2, color, alpha = 1, lineWidth = 1) {
  gfx.lineStyle(lineWidth, color, alpha)
  gfx.beginPath()
  gfx.moveTo(x1, y1)
  gfx.lineTo(x2, y2)
  gfx.strokePath()
}

/**
 * 绘制中心在 (cx, cy) 的椭圆
 */
function fillEllipse(gfx, cx, cy, width, height, color, alpha = 1) {
  const points = 32
  const hw = width / 2
  const hh = height / 2
  gfx.fillStyle(color, alpha)
  gfx.beginPath()
  for (let i = 0; i < points; i++) {
    const t = (i / points) * Math.PI * 2
    const px = cx + Math.cos(t) * hw
    const py = cy + Math.sin(t) * hh
    if (i === 0) gfx.moveTo(px, py)
    else gfx.lineTo(px, py)
  }
  gfx.closePath()
  gfx.fillPath()
}

// ==================== 一、玩家（中世纪骑士） ====================

/**
 * 绘制骑士玩家
 * 使用 Container，包含 bodyGfx（固定朝右）和 swordGfx（可旋转）
 * @param {Phaser.Scene} scene
 * @param {number} x - 位置X
 * @param {number} y - 位置Y
 * @returns {Phaser.GameObjects.Container}
 */
export function createPlayerContainer(scene, x, y) {
  const container = scene.add.container(x, y)

  // 身体部分（固定朝右）
  const bodyGfx = scene.add.graphics()
  drawPlayerBody(bodyGfx, 1)
  container.add(bodyGfx)

  // 剑部分（局部原点在护手处，rotation 随移动方向旋转）
  const swordGfx = scene.add.graphics()
  drawPlayerSword(swordGfx, 1)
  swordGfx.setPosition(13, -2) // 肩膀处偏移
  container.add(swordGfx)

  container.bodyGfx = bodyGfx
  container.swordGfx = swordGfx

  return container
}

/**
 * 绘制骑士身体（固定朝右）
 */
function drawPlayerBody(gfx, s) {
  const S = (v) => v * s

  // ---- 头盔 ----
  // 头盔主体圆形
  fillCircle(gfx, 0, S(-13), S(6), 0x8899bb)
  // 面罩矩形
  fillRect(gfx, S(-3), S(-16), S(6), S(5), 0x224466)
  // 面罩T形呼吸口
  gfx.lineStyle(S(1), 0xcccccc, 1)
  gfx.beginPath()
  gfx.moveTo(0, S(-16))
  gfx.lineTo(0, S(-12))
  gfx.moveTo(S(-2), S(-14))
  gfx.lineTo(S(2), S(-14))
  gfx.strokePath()
  // 头顶盔冠
  fillRect(gfx, S(-2), S(-20), S(4), S(3), 0x556677)
  fillCircle(gfx, 0, S(-21), S(1.5), 0xcccccc)

  // ---- 护颈 ----
  fillRect(gfx, S(-3.5), S(-9.5), S(7), S(3), 0x8899bb)
  strokeRect(gfx, S(-3.5), S(-9.5), S(7), S(3), 0xcccccc, 1, S(0.5))

  // ---- 胸甲 ----
  fillRect(gfx, S(-8), S(-6), S(16), S(12), 0x8899bb)
  strokeRect(gfx, S(-8), S(-6), S(16), S(12), 0x556677, 1, S(0.5))
  // 中线竖纹
  gfx.lineStyle(S(0.5), 0x556677, 0.6)
  gfx.beginPath()
  gfx.moveTo(0, S(-6))
  gfx.lineTo(0, S(6))
  gfx.strokePath()
  // 上半部高光
  fillRect(gfx, S(-6), S(-5), S(12), S(3), 0xaabbcc, 0.3)

  // ---- 肩甲 ----
  fillRect(gfx, S(-10), S(-5), S(5), S(5), 0x556677)
  fillRect(gfx, S(5), S(-5), S(5), S(5), 0x556677)
  strokeRect(gfx, S(-10), S(-5), S(5), S(5), 0xcccccc, 1, S(0.3))
  strokeRect(gfx, S(5), S(-5), S(5), S(5), 0xcccccc, 1, S(0.3))

  // ---- 腰带 ----
  fillRect(gfx, S(-8), S(4), S(16), S(3), 0x664422)
  fillRect(gfx, S(-2), S(4), S(4), S(3), 0xccaa44)

  // ---- 腿甲 ----
  fillRect(gfx, S(-6), S(7), S(6), S(8), 0x8899bb)
  fillRect(gfx, S(0), S(7), S(6), S(8), 0x8899bb)
  strokeRect(gfx, S(-6), S(7), S(6), S(8), 0x556677, 1, S(0.5))
  strokeRect(gfx, S(0), S(7), S(6), S(8), 0x556677, 1, S(0.5))
  // 膝盖圆形金属纹
  fillCircle(gfx, S(-3), S(11), S(1.5), 0xcccccc, 0.5)
  fillCircle(gfx, S(3), S(11), S(1.5), 0xcccccc, 0.5)

  // ---- 铁靴 ----
  fillRect(gfx, S(-6), S(15), S(6), S(4), 0x556677)
  fillRect(gfx, S(0), S(15), S(6), S(4), 0x556677)
  strokeRect(gfx, S(-6), S(15), S(6), S(4), 0xcccccc, 1, S(0.3))
  strokeRect(gfx, S(0), S(15), S(6), S(4), 0xcccccc, 1, S(0.3))

  // ---- 盾牌（左臂） ----
  // 古铜主体：矩形+底部半圆
  const shieldX = S(-12), shieldY = S(-2)
  fillRect(gfx, shieldX, shieldY, S(6), S(10), 0x884422)
  // 底部半圆
  gfx.fillStyle(0x884422, 1)
  gfx.fillCircle(shieldX + S(3), shieldY + S(10), S(3))
  // 金色镶边
  gfx.lineStyle(S(0.5), 0xaa6633, 1)
  gfx.strokeRect(shieldX, shieldY, S(6), S(10))
  // 十字金属纹
  gfx.lineStyle(S(0.5), 0xaa6633, 0.5)
  gfx.beginPath()
  gfx.moveTo(shieldX + S(3), shieldY)
  gfx.lineTo(shieldX + S(3), shieldY + S(13))
  gfx.moveTo(shieldX, shieldY + S(5))
  gfx.lineTo(shieldX + S(6), shieldY + S(5))
  gfx.strokePath()

  // ---- 右手臂（固定铠甲袖） ----
  fillRect(gfx, S(8), S(-3), S(4), S(6), 0x8899bb)
  strokeRect(gfx, S(8), S(-3), S(4), S(6), 0x556677, 1, S(0.3))
}

/**
 * 绘制骑士剑（swordGfx）
 */
function drawPlayerSword(gfx, s) {
  const S = (v) => v * s

  // 铁手套（护手处小矩形）
  fillRect(gfx, S(-2), S(-2), S(4), S(4), 0x8899bb)

  // 剑格十字护手
  fillRect(gfx, S(-3), S(-1), S(6), S(2), 0x886633)

  // 剑柄（深棕线条向下）
  gfx.lineStyle(S(2), 0x664422, 1)
  gfx.beginPath()
  gfx.moveTo(0, S(1))
  gfx.lineTo(0, S(5))
  gfx.strokePath()

  // 剑柄圆头
  fillCircle(gfx, 0, S(6), S(1.5), 0x664422)

  // 剑刃（银白水平线向右延伸16像素）
  gfx.lineStyle(S(3), 0xdddddd, 1)
  gfx.beginPath()
  gfx.moveTo(0, 0)
  gfx.lineTo(S(16), 0)
  gfx.strokePath()

  // 剑刃高光（半透明白线）
  gfx.lineStyle(S(1), 0xffffff, 0.4)
  gfx.beginPath()
  gfx.moveTo(S(2), S(-0.5))
  gfx.lineTo(S(14), S(-0.5))
  gfx.strokePath()

  // 剑尖亮点
  fillCircle(gfx, S(16), 0, S(1.5), 0xffffff, 0.7)
}

// ==================== 二、普通怪物（6种） ====================

// ---- 1. 哥布林 (Goblin) ----
export function drawGoblin(gfx, s) {
  const S = (v) => v * s

  // 头部圆形
  fillCircle(gfx, 0, S(-3), S(8), 0x6b8e3a)
  // 左右尖耳
  fillTriangle(gfx, S(-7), S(-7), S(-9), S(-2), S(-4), S(-5), 0x4a6b2a)
  fillTriangle(gfx, S(7), S(-7), S(9), S(-2), S(4), S(-5), 0x4a6b2a)
  // 粗眉
  fillRect(gfx, S(-5), S(-5), S(4), S(1.5), 0x222222)
  fillRect(gfx, S(1), S(-5), S(4), S(1.5), 0x222222)
  // 血红眼
  fillCircle(gfx, S(-3), S(-3), S(2.5), 0xcc3333)
  fillCircle(gfx, S(3), S(-3), S(2.5), 0xcc3333)
  fillCircle(gfx, S(-3), S(-3), S(1.2), 0x111111)
  fillCircle(gfx, S(3), S(-3), S(1.2), 0x111111)
  // 鼻子
  fillTriangle(gfx, 0, S(0), S(-1.5), S(2), S(1.5), S(2), 0x5a7a2a)
  // 獠牙
  fillTriangle(gfx, S(-3), S(2), S(-4.5), S(5), S(-1.5), S(3), 0xd4c9a8)
  fillTriangle(gfx, S(3), S(2), S(4.5), S(5), S(1.5), S(3), 0xd4c9a8)
  // 身体
  fillRect(gfx, S(-4), S(4), S(8), S(9), 0x5a6b3a)
  // 布纹线
  gfx.lineStyle(S(0.5), 0x3a4a2a, 0.5)
  gfx.beginPath()
  gfx.moveTo(S(-3), S(6)); gfx.lineTo(S(3), S(6))
  gfx.moveTo(S(-2), S(8)); gfx.lineTo(S(2), S(8))
  gfx.strokePath()
  // 手臂
  fillRect(gfx, S(-6), S(5), S(3), S(5), 0x7a9a4a)
  fillRect(gfx, S(3), S(5), S(3), S(5), 0x7a9a4a)
  // 手
  fillCircle(gfx, S(-5), S(11), S(2.5), 0xd4c9a8)
  fillCircle(gfx, S(5), S(11), S(2.5), 0xd4c9a8)
  // 骨棒
  fillRect(gfx, S(4), S(8), S(2), S(8), 0x5a3a1a)
  fillCircle(gfx, S(5), S(17), S(3.5), 0x8a6a3a)
  // 腿
  fillRect(gfx, S(-3.5), S(13), S(3), S(5), 0x4a6b2a)
  fillRect(gfx, S(0.5), S(13), S(3), S(5), 0x4a6b2a)
  // 脚
  fillRect(gfx, S(-4), S(17), S(4), S(2), 0x3a4a1a)
  fillRect(gfx, S(0), S(17), S(4), S(2), 0x3a4a1a)
}

// ---- 2. 史莱姆 (Slime) ----
export function drawSlime(gfx, s) {
  const S = (v) => v * s
  const A = (v) => v

  // 主体圆形（半透明）
  gfx.fillStyle(0x4488dd, 0.7)
  gfx.fillCircle(0, S(1), S(10))
  // 下半身椭圆（摊开感）
  fillEllipse(gfx, 0, S(5), S(18), S(12), 0x55aaee, 0.5)
  // 体内气泡
  gfx.fillStyle(0x66bbff, 0.3)
  gfx.fillCircle(S(-3), S(-2), S(2))
  gfx.fillStyle(0x88ddff, 0.2)
  gfx.fillCircle(S(4), S(1), S(2.5))
  gfx.fillStyle(0x66bbff, 0.25)
  gfx.fillCircle(S(-1), S(4), S(1.8))
  // 眼白
  gfx.fillStyle(0xffffff, 1)
  gfx.fillCircle(S(-3.5), S(-2), S(2.5))
  gfx.fillCircle(S(3.5), S(-2), S(2.5))
  // 瞳孔
  gfx.fillStyle(0x111111, 1)
  gfx.fillCircle(S(-3.5), S(-2), S(1.5))
  gfx.fillCircle(S(3.5), S(-2), S(1.5))
  // 高光
  gfx.fillStyle(0xffffff, 0.6)
  gfx.fillCircle(S(-4.5), S(-3), S(0.8))
  gfx.fillCircle(S(2.5), S(-3), S(0.8))
  // 微笑开口
  gfx.lineStyle(S(1), 0x3388cc, 0.6)
  gfx.beginPath()
  gfx.arc(0, S(1), S(4), 0.2, Math.PI - 0.2, false)
  gfx.strokePath()
  // 嘴内部
  gfx.fillStyle(0x2266aa, 0.3)
  gfx.fillEllipse(0, S(2), S(4), S(1.5))
  // 底部黏液滴
  gfx.fillStyle(0x55aaee, 0.4)
  gfx.fillCircle(S(-4), S(9), S(2))
  gfx.fillCircle(S(5), S(8), S(1.5))
}

// ---- 3. 骷髅 (Skeleton) ----
export function drawSkeleton(gfx, s) {
  const S = (v) => v * s

  // 头骨
  fillCircle(gfx, 0, S(-5), S(7), 0xe8dcc8)
  // 眼窝
  fillCircle(gfx, S(-3), S(-6), S(2.5), 0x111111)
  fillCircle(gfx, S(3), S(-6), S(2.5), 0x111111)
  // 鼻洞
  fillTriangle(gfx, S(-1), S(-3), S(0), S(-1), S(1), S(-3), 0x111111)
  // 嘴裂
  fillRect(gfx, S(-3), S(-1), S(6), S(1.5), 0x111111)
  // 头骨顶部凸起
  fillRect(gfx, S(-2), S(-12), S(1.5), S(3), 0xe8dcc8)
  fillRect(gfx, S(0.5), S(-12), S(1.5), S(3), 0xe8dcc8)
  // 破烂黑衣
  fillRect(gfx, S(-4), S(1), S(8), S(9), 0x2a2a2a)
  // 衣纹线
  gfx.lineStyle(S(0.5), 0x111111, 0.5)
  gfx.beginPath()
  gfx.moveTo(S(-2), S(3)); gfx.lineTo(S(2), S(3))
  gfx.moveTo(S(-1), S(6)); gfx.lineTo(S(1), S(6))
  gfx.strokePath()
  // 手臂骨
  fillRect(gfx, S(-6), S(2), S(3), S(6), 0xc8b898)
  fillRect(gfx, S(3), S(2), S(3), S(6), 0xc8b898)
  // 手骨
  fillCircle(gfx, S(-5), S(9), S(2), 0xc8b898)
  fillCircle(gfx, S(5), S(9), S(2), 0xc8b898)
  // 腿骨
  fillRect(gfx, S(-3), S(10), S(2.5), S(6), 0xc8b898)
  fillRect(gfx, S(0.5), S(10), S(2.5), S(6), 0xc8b898)
  // 断剑（右手）
  fillRect(gfx, S(4), S(5), S(2), S(7), 0x888888)
  fillRect(gfx, S(4), S(5), S(2), S(2), 0x664422)
  // 眼窝亡灵红光
  fillCircle(gfx, S(-3), S(-6), S(2.2), 0xcc3333, 0.4)
  fillCircle(gfx, S(3), S(-6), S(2.2), 0xcc3333, 0.4)
}

// ---- 4. 狼人 (Werewolf) ----
export function drawWerewolf(gfx, s) {
  const S = (v) => v * s

  // 头部圆形
  fillCircle(gfx, 0, S(-4), S(9), 0x665544)
  // 尖耳
  fillTriangle(gfx, S(-6), S(-10), S(-8), S(-16), S(-3), S(-10), 0x443322)
  fillTriangle(gfx, S(6), S(-10), S(8), S(-16), S(3), S(-10), 0x443322)
  // 额头毛
  fillEllipse(gfx, 0, S(-9), S(8), S(5), 0x554433)
  // 血红眼
  fillCircle(gfx, S(-4), S(-5), S(3), 0xff2200)
  fillCircle(gfx, S(4), S(-5), S(3), 0xff2200)
  // 黄色瞳孔
  fillCircle(gfx, S(-4), S(-5), S(1.5), 0xffcc00)
  fillCircle(gfx, S(4), S(-5), S(1.5), 0xffcc00)
  // 鼻子
  fillEllipse(gfx, 0, S(-1), S(4), S(2.5), 0x332211)
  // 血口
  fillTriangle(gfx, 0, S(1), S(-5), S(4), S(5), S(4), 0xcc3333)
  // 獠牙
  fillTriangle(gfx, S(-3), S(2), S(-4.5), S(5), S(-1.5), S(3), 0xdddddd)
  fillTriangle(gfx, S(3), S(2), S(4.5), S(5), S(1.5), S(3), 0xdddddd)
  // 上身肌肉
  fillRect(gfx, S(-5), S(4), S(10), S(10), 0x554433)
  // 肩部横条
  fillRect(gfx, S(-6), S(4), S(12), S(3), 0x665544)
  // 前臂
  fillRect(gfx, S(-8), S(7), S(4), S(5), 0x443322)
  fillRect(gfx, S(4), S(7), S(4), S(5), 0x443322)
  // 爪子
  fillTriangle(gfx, S(-8), S(11), S(-9), S(14), S(-6), S(12), 0x332211)
  fillTriangle(gfx, S(8), S(11), S(9), S(14), S(6), S(12), 0x332211)
  // 腿
  fillRect(gfx, S(-4), S(14), S(3.5), S(6), 0x443322)
  fillRect(gfx, S(0.5), S(14), S(3.5), S(6), 0x443322)
  // 脚爪
  fillRect(gfx, S(-4.5), S(19), S(5), S(2), 0x332211)
  fillRect(gfx, S(-0.5), S(19), S(5), S(2), 0x332211)
  // 侧面鬃毛
  fillTriangle(gfx, S(-9), S(-6), S(-12), S(-2), S(-7), S(-2), 0x665544)
  fillTriangle(gfx, S(9), S(-6), S(12), S(-2), S(7), S(-2), 0x665544)
}

// ---- 5. 食人魔 (Ogre) ----
export function drawOgre(gfx, s) {
  const S = (v) => v * s

  // 大头
  fillCircle(gfx, 0, S(-4), S(10), 0x8a7a5a)
  // 角座 + 角
  fillRect(gfx, S(-5), S(-12), S(3), S(3), 0x5a4a2a)
  fillRect(gfx, S(2), S(-12), S(3), S(3), 0x5a4a2a)
  fillTriangle(gfx, S(-4), S(-13), S(-3), S(-19), S(-1), S(-13), 0x6a5a3a)
  fillTriangle(gfx, S(4), S(-13), S(3), S(-19), S(1), S(-13), 0x6a5a3a)
  // 橙色眼
  fillCircle(gfx, S(-4), S(-5), S(3), 0xcc4400)
  fillCircle(gfx, S(4), S(-5), S(3), 0xcc4400)
  fillCircle(gfx, S(-4), S(-5), S(1.5), 0x442200)
  fillCircle(gfx, S(4), S(-5), S(1.5), 0x442200)
  // 大鼻
  fillEllipse(gfx, 0, S(-1), S(5), S(3.5), 0x5a3a1a)
  // 嘴
  fillEllipse(gfx, 0, S(3), S(6), S(2.5), 0x3a2a1a)
  // 牙
  fillTriangle(gfx, S(-2), S(2), S(-3.5), S(4.5), S(-0.5), S(3), 0xcccc88)
  fillTriangle(gfx, S(2), S(2), S(3.5), S(4.5), S(0.5), S(3), 0xcccc88)
  // 肥壮身体
  fillRect(gfx, S(-6), S(5), S(12), S(12), 0x7a6a4a)
  // 粗臂
  fillRect(gfx, S(-8), S(6), S(4), S(7), 0x6a5a3a)
  fillRect(gfx, S(4), S(6), S(4), S(7), 0x6a5a3a)
  // 巨棒（右手）
  fillRect(gfx, S(5), S(7), S(2.5), S(10), 0x5a3a1a)
  fillEllipse(gfx, S(6.5), S(18), S(6), S(4), 0x4a2a0a)
  // 粗腿
  fillRect(gfx, S(-4.5), S(17), S(4), S(5), 0x6a5a3a)
  fillRect(gfx, S(0.5), S(17), S(4), S(5), 0x6a5a3a)
  // 大脚
  fillRect(gfx, S(-5.5), S(21), S(5), S(2), 0x5a4a2a)
  fillRect(gfx, S(0.5), S(21), S(5), S(2), 0x5a4a2a)
}

// ---- 6. 火焰史莱姆 (Flame Slime) ★特殊 ----
export function drawFlameSlime(gfx, s) {
  const S = (v) => v * s

  // 主体圆形
  gfx.fillStyle(0xff4400, 0.8)
  gfx.fillCircle(0, 0, S(10))
  // 下半熔岩椭圆
  fillEllipse(gfx, 0, S(3), S(18), S(11), 0xff6600, 0.6)
  // 内部发光
  gfx.fillStyle(0xffaa00, 0.4)
  gfx.fillCircle(0, S(-1), S(6))
  // 黄色眼睛
  gfx.fillStyle(0xffff44, 1)
  gfx.fillCircle(S(-3.5), S(-3), S(2.5))
  gfx.fillCircle(S(3.5), S(-3), S(2.5))
  // 红色瞳孔
  gfx.fillStyle(0xcc2200, 1)
  gfx.fillCircle(S(-3.5), S(-3), S(1.3))
  gfx.fillCircle(S(3.5), S(-3), S(1.3))
  // 嘴
  gfx.fillStyle(0xff8800, 0.5)
  gfx.fillEllipse(0, S(2), S(4), S(2))

  // 火焰尖刺（三层）
  // 外层（亮黄）
  gfx.fillStyle(0xffcc00, 1)
  fillTriangle(gfx, 0, S(-12), S(-5), S(-6), S(5), S(-6), 0xffcc00) // 顶部
  fillTriangle(gfx, S(-10), S(-4), S(-5), S(-1), S(-7), S(-8), 0xffcc00) // 左
  fillTriangle(gfx, S(10), S(-4), S(5), S(-1), S(7), S(-8), 0xffcc00) // 右
  // 中层（橙）
  gfx.fillStyle(0xff6600, 1)
  fillTriangle(gfx, 0, S(-10), S(-3), S(-5), S(3), S(-5), 0xff6600)
  fillTriangle(gfx, S(-8), S(-3), S(-4), S(-1), S(-5), S(-7), 0xff6600)
  fillTriangle(gfx, S(8), S(-3), S(4), S(-1), S(5), S(-7), 0xff6600)
  // 内层（红橙）
  gfx.fillStyle(0xff4400, 0.6)
  fillTriangle(gfx, 0, S(-8), S(-2), S(-4), S(2), S(-4), 0xff4400, 0.6)
  fillTriangle(gfx, S(-6), S(-2), S(-3), S(0), S(-4), S(-5), 0xff4400, 0.6)
  fillTriangle(gfx, S(6), S(-2), S(3), S(0), S(4), S(-5), 0xff4400, 0.6)

  // 火星
  gfx.fillStyle(0xffaa44, 0.3)
  gfx.fillCircle(S(-7), S(-9), S(1))
  gfx.fillCircle(S(6), S(-10), S(1.2))
  gfx.fillCircle(S(8), S(-6), S(0.8))
  gfx.fillCircle(S(-8), S(-7), S(0.8))
  // 底部熔岩滴
  gfx.fillStyle(0xff6600, 0.3)
  gfx.fillCircle(S(-5), S(9), S(2))
  gfx.fillCircle(S(6), S(8), S(1.5))
}

// ==================== 三、精英怪物（5种） ====================

// ---- 7. 暗影骑士 (Shadow Knight) ----
export function drawShadowKnight(gfx, s) {
  const S = (v) => v * s

  // 头盔
  fillCircle(gfx, 0, S(-5), S(7), 0x222233)
  // 头盔尖角
  fillRect(gfx, S(-4), S(-11), S(2), S(4), 0x111122)
  fillRect(gfx, S(2), S(-11), S(2), S(4), 0x111122)
  fillTriangle(gfx, S(-3), S(-12), S(-3), S(-15), S(-1), S(-12), 0x440000)
  fillTriangle(gfx, S(3), S(-12), S(3), S(-15), S(1), S(-12), 0x440000)
  // 暗红眼
  fillCircle(gfx, S(-3), S(-5), S(2), 0x660000)
  fillCircle(gfx, S(3), S(-5), S(2), 0x660000)
  fillCircle(gfx, S(-3), S(-5), S(1), 0xcc0000)
  fillCircle(gfx, S(3), S(-5), S(1), 0xcc0000)
  // 胸甲
  fillRect(gfx, S(-5), S(1), S(10), S(10), 0x334455)
  strokeRect(gfx, S(-5), S(1), S(10), S(10), 0x445566, 1, S(0.5))
  // 胸甲中线
  gfx.lineStyle(S(0.5), 0x222233, 0.6)
  gfx.beginPath()
  gfx.moveTo(0, S(1)); gfx.lineTo(0, S(11))
  gfx.strokePath()
  // 肩甲
  fillRect(gfx, S(-7), S(1), S(4), S(4), 0x445566)
  fillRect(gfx, S(3), S(1), S(4), S(4), 0x445566)
  // 肩刺
  fillTriangle(gfx, S(-8), S(2), S(-9), S(6), S(-6), S(3), 0x556677)
  fillTriangle(gfx, S(8), S(2), S(9), S(6), S(6), S(3), 0x556677)
  // 腰带
  fillRect(gfx, S(-5), S(10), S(10), S(2), 0x556677)
  // 腿甲
  fillRect(gfx, S(-4), S(12), S(3.5), S(6), 0x334455)
  fillRect(gfx, S(0.5), S(12), S(3.5), S(6), 0x334455)
  // 矛杆
  fillRect(gfx, S(6), S(-4), S(1.5), S(18), 0x886644)
  // 矛尖
  fillTriangle(gfx, S(6.5), S(-5), S(5), S(-1), S(8), S(-1), 0xcccccc)
  // 暗影光环
  gfx.fillStyle(0x660000, 0.15)
  gfx.fillCircle(0, S(4), S(16))
}

// ---- 8. 地狱犬 (Hell Hound) ----
export function drawHellHound(gfx, s) {
  const S = (v) => v * s

  // 两个头
  fillCircle(gfx, S(-6), S(-3), S(6), 0x883322)
  fillCircle(gfx, S(6), S(-3), S(6), 0x883322)
  // 耳朵
  fillEllipse(gfx, S(-9), S(-8), S(4), S(3), 0x662211)
  fillEllipse(gfx, S(3), S(-8), S(4), S(3), 0x662211)
  fillEllipse(gfx, S(-8), S(-8), S(4), S(3), 0x662211)
  fillEllipse(gfx, S(10), S(-8), S(4), S(3), 0x662211)
  // 橙眼（左右头各两只）
  fillCircle(gfx, S(-8), S(-4), S(2), 0xff4400)
  fillCircle(gfx, S(-4), S(-4), S(2), 0xff4400)
  fillCircle(gfx, S(4), S(-4), S(2), 0xff4400)
  fillCircle(gfx, S(8), S(-4), S(2), 0xff4400)
  fillCircle(gfx, S(-8), S(-4), S(1), 0xcc2200)
  fillCircle(gfx, S(-4), S(-4), S(1), 0xcc2200)
  fillCircle(gfx, S(4), S(-4), S(1), 0xcc2200)
  fillCircle(gfx, S(8), S(-4), S(1), 0xcc2200)
  // 身体
  fillRect(gfx, S(-12), S(1), S(24), S(9), 0x442211)
  // 背部横条
  fillRect(gfx, S(-12), S(1), S(24), S(2), 0x553322)
  // 铁链项圈
  fillRect(gfx, S(-12), S(0), S(24), S(2), 0x222222)
  // 铆钉
  fillCircle(gfx, S(-6), S(1), S(1.5), 0x666666)
  fillCircle(gfx, S(0), S(1), S(1.5), 0x666666)
  fillCircle(gfx, S(6), S(1), S(1.5), 0x666666)
  // 腿
  fillRect(gfx, S(-9), S(9), S(3), S(6), 0x883322)
  fillRect(gfx, S(-4), S(9), S(3), S(6), 0x883322)
  fillRect(gfx, S(1), S(9), S(3), S(6), 0x883322)
  fillRect(gfx, S(6), S(9), S(3), S(6), 0x883322)
  // 脚
  fillRect(gfx, S(-9.5), S(14), S(4), S(2), 0x662211)
  fillRect(gfx, S(-4.5), S(14), S(4), S(2), 0x662211)
  fillRect(gfx, S(0.5), S(14), S(4), S(2), 0x662211)
  fillRect(gfx, S(5.5), S(14), S(4), S(2), 0x662211)
  // 火焰吐息
  fillTriangle(gfx, S(-12), S(-3), S(-18), S(-1), S(-13), S(2), 0xff6600)
  fillTriangle(gfx, S(12), S(-3), S(18), S(-1), S(13), S(2), 0xff6600)
  // 地狱光
  gfx.fillStyle(0xff4400, 0.12)
  gfx.fillCircle(0, S(4), S(16))
}

// ---- 9. 石像鬼 (Gargoyle) ----
export function drawGargoyle(gfx, s) {
  const S = (v) => v * s

  // 石头头
  fillCircle(gfx, 0, S(-4), S(8), 0x7a7a8a)
  // 石角
  fillTriangle(gfx, S(-5), S(-8), S(-6), S(-15), S(-2), S(-9), 0x6a6a7a)
  fillTriangle(gfx, S(5), S(-8), S(6), S(-15), S(2), S(-9), 0x6a6a7a)
  fillRect(gfx, S(-5), S(-9), S(2), S(2), 0x5a5a6a)
  fillRect(gfx, S(3), S(-9), S(2), S(2), 0x5a5a6a)
  // 橙眼
  fillCircle(gfx, S(-3), S(-5), S(2.5), 0xcc4400)
  fillCircle(gfx, S(3), S(-5), S(2.5), 0xcc4400)
  fillCircle(gfx, S(-3), S(-5), S(1.2), 0x442200)
  fillCircle(gfx, S(3), S(-5), S(1.2), 0x442200)
  // 石身
  fillRect(gfx, S(-4), S(3), S(8), S(10), 0x5a5a6a)
  // 胸纹
  fillRect(gfx, S(-4), S(5), S(8), S(1.5), 0x4a4a5a)
  fillRect(gfx, S(-4), S(8), S(8), S(1.5), 0x4a4a5a)
  // 蝙蝠翼
  fillTriangle(gfx, S(-5), S(0), S(-16), S(-6), S(-9), S(8), 0x6a6a7a)
  fillTriangle(gfx, S(5), S(0), S(16), S(-6), S(9), S(8), 0x6a6a7a)
  fillTriangle(gfx, S(-5), S(2), S(-12), S(-2), S(-8), S(6), 0x5a5a6a)
  fillTriangle(gfx, S(5), S(2), S(12), S(-2), S(8), S(6), 0x5a5a6a)
  // 苔藓
  gfx.fillStyle(0x6a7a5a, 0.3)
  gfx.fillCircle(S(-2), S(5), S(2))
  gfx.fillCircle(S(3), S(8), S(2.5))
  // 石腿
  fillRect(gfx, S(-3.5), S(13), S(3), S(5), 0x4a4a5a)
  fillRect(gfx, S(0.5), S(13), S(3), S(5), 0x4a4a5a)
}

// ---- 10. 暗黑法师 (Dark Mage) ----
export function drawDarkMage(gfx, s) {
  const S = (v) => v * s

  // 兜帽头
  fillCircle(gfx, 0, S(-5), S(7), 0x332255)
  // 兜帽尖
  fillTriangle(gfx, S(-7), S(-8), S(7), S(-8), 0, S(-13), 0x221144)
  // 苍白脸
  fillCircle(gfx, 0, S(-3), S(4.5), 0xccbbaa)
  // 紫色发光眼
  fillCircle(gfx, S(-2.5), S(-4), S(2), 0x9933ff)
  fillCircle(gfx, S(2.5), S(-4), S(2), 0x9933ff)
  fillCircle(gfx, S(-2.5), S(-4), S(1), 0xcc66ff)
  fillCircle(gfx, S(2.5), S(-4), S(1), 0xcc66ff)
  // 嘴
  fillEllipse(gfx, 0, S(-1), S(3), S(1), 0x222222)
  // 长袍
  fillTriangle(gfx, S(-8), S(0), S(8), S(0), 0, S(16), 0x442266)
  // 腰带
  fillRect(gfx, S(-5), S(5), S(10), S(2), 0x553377)
  // 法杖
  fillRect(gfx, S(7), S(-6), S(2), S(16), 0x886644)
  // 法杖顶端紫焰
  fillCircle(gfx, S(8), S(-8), S(4), 0x9933ff)
  fillCircle(gfx, S(8), S(-8), S(3), 0xcc66ff)
  fillCircle(gfx, S(8), S(-8), S(1.5), 0xff88ff)
  // 暗紫光环
  gfx.fillStyle(0x6622aa, 0.15)
  gfx.fillCircle(0, S(4), S(16))
}

// ---- 11. 巨型蜘蛛 (Giant Spider) ----
export function drawGiantSpider(gfx, s) {
  const S = (v) => v * s

  // 腹部
  fillEllipse(gfx, 0, S(1), S(14), S(10), 0x332211)
  // 腹纹
  fillEllipse(gfx, 0, S(1), S(8), S(6), 0x442211)
  // 头胸部
  fillCircle(gfx, 0, S(-6), S(6), 0x553322)
  // 额头
  fillRect(gfx, S(-3), S(-10), S(6), S(3), 0x442211)
  // 红眼
  fillCircle(gfx, S(-3), S(-7), S(2.5), 0xcc2200)
  fillCircle(gfx, S(3), S(-7), S(2.5), 0xcc2200)
  fillCircle(gfx, S(-3), S(-7), S(1), 0xff4400)
  fillCircle(gfx, S(3), S(-7), S(1), 0xff4400)
  // 嘴 + 毒牙
  fillEllipse(gfx, 0, S(-4), S(3), S(1.5), 0x222222)
  fillTriangle(gfx, S(-2), S(-4), S(-3), S(-1), S(-1), S(-3), 0x222222)
  fillTriangle(gfx, S(2), S(-4), S(3), S(-1), S(1), S(-3), 0x222222)
  // 八条关节腿
  const legs = [
    { x1: -5, y1: -2, x2: -12, y2: -8 },
    { x1: -5, y1: 0, x2: -14, y2: -4 },
    { x1: -5, y1: 2, x2: -14, y2: 4 },
    { x1: -5, y1: 4, x2: -12, y2: 8 },
    { x1: 5, y1: -2, x2: 12, y2: -8 },
    { x1: 5, y1: 0, x2: 14, y2: -4 },
    { x1: 5, y1: 2, x2: 14, y2: 4 },
    { x1: 5, y1: 4, x2: 12, y2: 8 },
  ]
  for (const leg of legs) {
    gfx.lineStyle(S(2.5), 0x553322, 1)
    gfx.beginPath()
    gfx.moveTo(S(leg.x1), S(leg.y1))
    gfx.lineTo(S(leg.x2), S(leg.y2))
    gfx.strokePath()
    gfx.lineStyle(S(1), 0x442211, 0.5)
    gfx.beginPath()
    gfx.moveTo(S(leg.x1), S(leg.y1))
    gfx.lineTo(S(leg.x2), S(leg.y2))
    gfx.strokePath()
  }
}

// ==================== 四、Boss（5种） ====================

// ---- 12. 巨龙 (Dragon) ----
export function drawDragon(gfx, s) {
  const S = (v) => v * s

  // 龙头
  fillCircle(gfx, 0, S(-4), S(10), 0x226622)
  // 额头
  fillEllipse(gfx, 0, S(-9), S(8), S(5), 0x44aa44)
  // 龙角
  fillTriangle(gfx, S(-6), S(-10), S(-8), S(-18), S(-3), S(-11), 0x118811)
  fillTriangle(gfx, S(6), S(-10), S(8), S(-18), S(3), S(-11), 0x118811)
  // 金黄眼 + 竖瞳
  fillCircle(gfx, S(-4), S(-5), S(3), 0xffcc00)
  fillCircle(gfx, S(4), S(-5), S(3), 0xffcc00)
  fillRect(gfx, S(-4.5), S(-6), S(1), S(3), 0x442200) // 矩形竖瞳
  fillRect(gfx, S(3.5), S(-6), S(1), S(3), 0x442200)
  // 嘴部
  fillTriangle(gfx, 0, S(-1), S(-5), S(4), S(5), S(4), 0x116611)
  // 龙牙
  fillTriangle(gfx, S(-3), S(0), S(-4.5), S(4), S(-1.5), S(2), 0xcccc88)
  fillTriangle(gfx, S(3), S(0), S(4.5), S(4), S(1.5), S(2), 0xcccc88)
  // 龙身
  fillRect(gfx, S(-7), S(3), S(14), S(14), 0x226622)
  // 鳞纹
  fillRect(gfx, S(-7), S(6), S(14), S(1.5), 0x338833)
  fillRect(gfx, S(-7), S(10), S(14), S(1.5), 0x338833)
  fillRect(gfx, S(-7), S(14), S(14), S(1.5), 0x338833)
  // 巨大翼膜
  fillTriangle(gfx, S(-5), S(2), S(-18), S(-4), S(-8), S(12), 0x44aa44)
  fillTriangle(gfx, S(5), S(2), S(18), S(-4), S(8), S(12), 0x44aa44)
  // 翼骨
  gfx.lineStyle(S(1.5), 0x228822, 0.6)
  gfx.beginPath()
  gfx.moveTo(S(-4), S(2)); gfx.lineTo(S(-15), S(-2))
  gfx.moveTo(S(-4), S(4)); gfx.lineTo(S(-13), S(6))
  gfx.moveTo(S(4), S(2)); gfx.lineTo(S(15), S(-2))
  gfx.moveTo(S(4), S(4)); gfx.lineTo(S(13), S(6))
  gfx.strokePath()
  // 龙腿
  fillRect(gfx, S(-5), S(17), S(4), S(5), 0x115511)
  fillRect(gfx, S(1), S(17), S(4), S(5), 0x115511)
  // 龙爪
  fillRect(gfx, S(-6), S(21), S(5), S(2), 0x224422)
  fillRect(gfx, S(1), S(21), S(5), S(2), 0x224422)
  // 龙息光环
  gfx.fillStyle(0x44ff44, 0.1)
  gfx.fillCircle(0, S(6), S(20))
}

// ---- 13. 恶魔领主 (Demon Lord) ----
export function drawDemonLord(gfx, s) {
  const S = (v) => v * s

  // 头
  fillCircle(gfx, 0, S(-4), S(9), 0x882222)
  // 额头
  fillEllipse(gfx, 0, S(-9), S(8), S(5), 0x661111)
  // 巨弯角
  fillTriangle(gfx, S(-5), S(-10), S(-9), S(-22), S(-2), S(-12), 0x441111)
  fillTriangle(gfx, S(5), S(-10), S(9), S(-22), S(2), S(-12), 0x441111)
  // 角节
  fillRect(gfx, S(-7), S(-15), S(2), S(2), 0x553333)
  fillRect(gfx, S(5), S(-15), S(2), S(2), 0x553333)
  // 金黄眼 + 红瞳
  fillCircle(gfx, S(-4), S(-5), S(3), 0xffcc00)
  fillCircle(gfx, S(4), S(-5), S(3), 0xffcc00)
  fillCircle(gfx, S(-4), S(-5), S(1.5), 0xcc2200)
  fillCircle(gfx, S(4), S(-5), S(1.5), 0xcc2200)
  // 魔躯
  fillRect(gfx, S(-5), S(4), S(10), S(12), 0x993333)
  // 胸甲
  fillRect(gfx, S(-5), S(5), S(10), S(3), 0xaa4444)
  // 蝙蝠翼
  fillTriangle(gfx, S(-5), S(3), S(-16), S(-2), S(-10), S(10), 0xff6600)
  fillTriangle(gfx, S(5), S(3), S(16), S(-2), S(10), S(10), 0xff6600)
  fillTriangle(gfx, S(-5), S(5), S(-12), S(1), S(-8), S(8), 0xff4400)
  fillTriangle(gfx, S(5), S(5), S(12), S(1), S(8), S(8), 0xff4400)
  // 腿
  fillRect(gfx, S(-4), S(16), S(3.5), S(5), 0x661111)
  fillRect(gfx, S(0.5), S(16), S(3.5), S(5), 0x661111)
  // 羊蹄
  fillRect(gfx, S(-4.5), S(20), S(4), S(2), 0x771111)
  fillRect(gfx, S(0.5), S(20), S(4), S(2), 0x771111)
  // 头顶火焰
  fillTriangle(gfx, 0, S(-13), S(-5), S(-8), S(5), S(-8), 0xff4400)
  fillTriangle(gfx, 0, S(-11), S(-3), S(-7), S(3), S(-7), 0xff8800)
  // 地狱光环
  gfx.fillStyle(0xaa2222, 0.12)
  gfx.fillCircle(0, S(6), S(18))
}

// ---- 14. 巫妖王 (Lich King) ----
export function drawLichKing(gfx, s) {
  const S = (v) => v * s

  // 头骨
  fillCircle(gfx, 0, S(-4), S(8), 0xcccccc)
  fillCircle(gfx, 0, S(-6), S(6), 0xaaaaaa)
  // 眼窝
  fillCircle(gfx, S(-3), S(-5), S(2.5), 0x111111)
  fillCircle(gfx, S(3), S(-5), S(2.5), 0x111111)
  // 冰蓝发光瞳孔
  fillCircle(gfx, S(-3), S(-5), S(1.5), 0x4488ff)
  fillCircle(gfx, S(3), S(-5), S(1.5), 0x4488ff)
  fillCircle(gfx, S(-3), S(-5), S(0.7), 0x66aaff)
  fillCircle(gfx, S(3), S(-5), S(0.7), 0x66aaff)
  // 鼻洞
  fillTriangle(gfx, S(-1.5), S(-2), S(0), S(0), S(1.5), S(-2), 0x111111)
  // 嘴裂
  fillRect(gfx, S(-3), S(1), S(6), S(1.5), 0x111111)
  // 冰蓝皇冠
  fillTriangle(gfx, 0, S(-13), S(-5), S(-6), S(5), S(-6), 0x4488ff)
  fillRect(gfx, S(-5.5), S(-7), S(11), S(2), 0x66aaff)
  fillCircle(gfx, 0, S(-12), S(2), 0x88ddff)
  // 眉心符文
  fillRect(gfx, S(-1.5), S(-8), S(3), S(1.5), 0x88bbff)
  // 寒冰法袍
  fillTriangle(gfx, S(-9), S(2), S(9), S(2), 0, S(18), 0x334466)
  // 袍横纹
  fillRect(gfx, S(-6), S(6), S(12), S(1.5), 0x445577)
  fillRect(gfx, S(-5), S(10), S(10), S(1.5), 0x445577)
  // 冰法球
  fillCircle(gfx, S(7), S(3), S(5), 0x4488ff)
  fillCircle(gfx, S(7), S(3), S(3.5), 0x88ccff)
  fillCircle(gfx, S(7), S(3), S(2), 0xaaddff)
  // 寒冰光环
  gfx.fillStyle(0x4488ff, 0.1)
  gfx.fillCircle(0, S(6), S(18))
}

// ---- 15. 巨型石魔像 (Golem) ----
export function drawGolem(gfx, s) {
  const S = (v) => v * s

  // 石头头
  fillCircle(gfx, 0, S(-5), S(9), 0x7a7a7a)
  // 石角
  fillRect(gfx, S(-5), S(-12), S(3), S(4), 0x6a6a6a)
  fillRect(gfx, S(2), S(-12), S(3), S(4), 0x6a6a6a)
  fillRect(gfx, S(-4), S(-14), S(2), S(3), 0x5a5a5a)
  fillRect(gfx, S(2), S(-14), S(2), S(3), 0x5a5a5a)
  // 橙光眼
  fillCircle(gfx, S(-3.5), S(-6), S(2.5), 0xff8844)
  fillCircle(gfx, S(3.5), S(-6), S(2.5), 0xff8844)
  fillCircle(gfx, S(-3.5), S(-6), S(1.2), 0xffcc66)
  fillCircle(gfx, S(3.5), S(-6), S(1.2), 0xffcc66)
  // 巨石身体
  fillRect(gfx, S(-8), S(4), S(16), S(16), 0x666666)
  // 石臂
  fillRect(gfx, S(-10), S(6), S(4), S(8), 0x777777)
  fillRect(gfx, S(6), S(6), S(4), S(8), 0x777777)
  // 石拳
  fillCircle(gfx, S(-8), S(15), S(3.5), 0x888888)
  fillCircle(gfx, S(8), S(15), S(3.5), 0x888888)
  // 石腿
  fillRect(gfx, S(-5), S(20), S(4), S(6), 0x555555)
  fillRect(gfx, S(1), S(20), S(4), S(6), 0x555555)
  // 石纹裂隙
  gfx.lineStyle(S(0.5), 0x444444, 0.5)
  gfx.beginPath()
  gfx.moveTo(S(-3), S(6)); gfx.lineTo(S(2), S(10))
  gfx.moveTo(S(4), S(7)); gfx.lineTo(S(-1), S(13))
  gfx.moveTo(S(-5), S(12)); gfx.lineTo(S(0), S(17))
  gfx.strokePath()
  // 核心橙光
  gfx.fillStyle(0xff8844, 0.15)
  gfx.fillCircle(0, S(12), S(18))
}

// ---- 16. 暗影之王 (Shadow King) ----
export function drawShadowKing(gfx, s) {
  const S = (v) => v * s

  // 暗影头
  fillCircle(gfx, 0, S(-3), S(10), 0x332244)
  // 雾冠
  fillEllipse(gfx, 0, S(-7), S(12), S(5), 0x443355)
  // 紫色发光眼
  fillCircle(gfx, S(-4), S(-4), S(3), 0x8844ff)
  fillCircle(gfx, S(4), S(-4), S(3), 0x8844ff)
  fillCircle(gfx, S(-4), S(-4), S(1.5), 0xaa66ff)
  fillCircle(gfx, S(4), S(-4), S(1.5), 0xaa66ff)
  // 暗影王冠
  fillTriangle(gfx, 0, S(-19), S(-6), S(-10), S(6), S(-10), 0x221133)
  // 冠顶紫色宝石
  fillCircle(gfx, 0, S(-17), S(2.5), 0xaa66ff)
  // 暗影披风
  fillTriangle(gfx, S(-10), S(4), S(10), S(4), 0, S(19), 0x442266)
  // 腰带
  fillRect(gfx, S(-5), S(9), S(10), S(2), 0x553377)
  // 暗影核
  fillCircle(gfx, 0, S(5), S(5), 0x222244)
  // 三层光环
  const ringColors = [0x442266, 0x6633aa, 0x8844ff]
  const ringAlphas = [0.15, 0.08, 0.05]
  const ringRadii = [16, 20, 24]
  for (let i = 0; i < 3; i++) {
    gfx.fillStyle(ringColors[i], ringAlphas[i])
    gfx.fillCircle(0, S(4), S(ringRadii[i]))
  }
  // 冠光
  fillTriangle(gfx, 0, S(-10), S(-2), S(-6), S(2), S(-6), 0x8844ff, 0.3)
}

// ==================== 五、奇遇NPC（4种） ====================

// ---- 17. 宝箱 (Chest) ----
export function drawChest(gfx, s) {
  const S = (v) => v * s

  // 箱体
  fillRect(gfx, S(-9), S(3), S(18), S(13), 0xcc8822)
  // 箱底纹
  fillRect(gfx, S(-8), S(13), S(16), S(2), 0xbb7722)
  // 箱盖
  fillRect(gfx, S(-9), S(-6), S(18), S(9), 0xeeaa33)
  // 盖弧
  fillRect(gfx, S(-8), S(-6), S(16), S(3), 0xdd9933)
  // 锁扣
  fillRect(gfx, S(-2.5), S(0), S(5), S(3.5), 0xffcc44)
  // 红宝石
  fillCircle(gfx, 0, S(1.5), S(2), 0xcc2222)
  // 边框
  strokeRect(gfx, S(-9), S(3), S(18), S(13), 0x886611, 1, S(0.5))
  strokeRect(gfx, S(-9), S(-6), S(18), S(9), 0x886611, 1, S(0.5))
  // 中缝线
  gfx.lineStyle(S(0.5), 0x886611, 0.8)
  gfx.beginPath()
  gfx.moveTo(S(-9), S(3)); gfx.lineTo(S(9), S(3))
  gfx.strokePath()
  // 盖雕纹
  gfx.lineStyle(S(0.3), 0xaa8833, 0.5)
  gfx.beginPath()
  gfx.moveTo(S(-5), S(-3)); gfx.lineTo(S(5), S(-3))
  gfx.moveTo(S(-4), S(-1)); gfx.lineTo(S(4), S(-1))
  gfx.strokePath()
  // 光芒特效（两层三角从顶部发出）
  fillTriangle(gfx, 0, S(-10), S(-3), S(-6), S(3), S(-6), 0xffdd66, 0.3)
  fillTriangle(gfx, 0, S(-12), S(-2), S(-8), S(2), S(-8), 0xffee88, 0.2)
}

// ---- 18. 圣女 (Maiden) ----
export function drawMaiden(gfx, s) {
  const S = (v) => v * s

  // 脸
  fillCircle(gfx, 0, S(-6), S(5), 0xffeedd)
  // 金色头发
  fillEllipse(gfx, 0, S(-10), S(8), S(5), 0xffdd88)
  // 发帘
  fillRect(gfx, S(-4), S(-12), S(8), S(4), 0xddbb77)
  // 蓝眼
  fillCircle(gfx, S(-2), S(-7), S(1.5), 0x4488ff)
  fillCircle(gfx, S(2), S(-7), S(1.5), 0x4488ff)
  // 深蓝瞳孔
  fillCircle(gfx, S(-2), S(-7), S(0.8), 0x224488)
  fillCircle(gfx, S(2), S(-7), S(0.8), 0x224488)
  // 樱唇
  fillEllipse(gfx, 0, S(-4), S(2), S(1), 0xee8877)
  // 粉色裙子
  fillTriangle(gfx, S(-6), S(-2), S(6), S(-2), 0, S(13), 0xffccdd)
  // 浅色内衬
  fillTriangle(gfx, S(-4), S(-1), S(4), S(-1), 0, S(10), 0xffddee)
  // 白色领口
  fillRect(gfx, S(-2), S(-2), S(4), S(2), 0xffffff)
  // 三层柔和光环
  gfx.fillStyle(0xffffff, 0.08)
  gfx.fillCircle(0, S(0), S(14))
  gfx.fillStyle(0xffffff, 0.12)
  gfx.fillCircle(0, S(0), S(11))
  gfx.fillStyle(0xffffff, 0.06)
  gfx.fillCircle(0, S(0), S(9))
}

// ---- 19. 天使 (Angel) ----
export function drawAngel(gfx, s) {
  const S = (v) => v * s

  // 脸
  fillCircle(gfx, 0, S(-6), S(5), 0xffeeee)
  // 金色头发
  fillEllipse(gfx, 0, S(-10), S(8), S(5), 0xffdd88)
  // 蓝眼
  fillCircle(gfx, S(-2), S(-7), S(1.5), 0x4488ff)
  fillCircle(gfx, S(2), S(-7), S(1.5), 0x4488ff)
  fillCircle(gfx, S(-2), S(-7), S(0.8), 0x224488)
  fillCircle(gfx, S(2), S(-7), S(0.8), 0x224488)
  // 唇
  fillEllipse(gfx, 0, S(-4), S(2), S(1), 0xff8888)
  // 白色圣袍
  fillTriangle(gfx, S(-6), S(-2), S(6), S(-2), 0, S(14), 0xffffff)
  // 浅衬
  fillTriangle(gfx, S(-4), S(-1), S(4), S(-1), 0, S(10), 0xffeeff)
  // 巨大金色翅膀
  fillTriangle(gfx, S(-3), S(-2), S(-16), S(-6), S(-6), S(8), 0xffdd88)
  fillTriangle(gfx, S(3), S(-2), S(16), S(-6), S(6), S(8), 0xffdd88)
  fillTriangle(gfx, S(-3), S(0), S(-11), S(-2), S(-5), S(6), 0xffeeaa)
  fillTriangle(gfx, S(3), S(0), S(11), S(-2), S(5), S(6), 0xffeeaa)
  // 金色光环
  gfx.fillStyle(0xffdd44, 0.2)
  gfx.fillCircle(0, S(-9), S(7))
  // 圣光
  gfx.fillStyle(0xffffff, 0.06)
  gfx.fillCircle(0, S(2), S(16))
}

// ---- 20. 铁匠 (Blacksmith) ----
export function drawBlacksmith(gfx, s) {
  const S = (v) => v * s

  // 脸
  fillCircle(gfx, 0, S(-6), S(5), 0xddbb99)
  // 棕色乱发
  fillEllipse(gfx, 0, S(-10), S(8), S(5), 0x553322)
  // 大胡子
  fillRect(gfx, S(-3), S(-4), S(6), S(4), 0x886644)
  // 黑眼
  fillCircle(gfx, S(-2), S(-7), S(1.5), 0x222222)
  fillCircle(gfx, S(2), S(-7), S(1.5), 0x222222)
  // 皮围裙
  fillRect(gfx, S(-5), S(-2), S(10), S(10), 0x886644)
  // 蓝色衬衫
  fillRect(gfx, S(-5), S(-2), S(10), S(3), 0x4488cc)
  // 手臂
  fillRect(gfx, S(-7), S(-1), S(3), S(6), 0x886644)
  fillRect(gfx, S(4), S(-1), S(3), S(6), 0x886644)
  // 手
  fillCircle(gfx, S(-6), S(6), S(2.5), 0xddbb99)
  fillCircle(gfx, S(6), S(6), S(2.5), 0xddbb99)
  // 大铁锤（右手侧）
  fillRect(gfx, S(5), S(-4), S(5), S(4), 0x888888) // 锤头
  fillRect(gfx, S(6), S(-5), S(3), S(1.5), 0x666666) // 锤顶
  fillRect(gfx, S(6.5), S(0), S(2), S(8), 0x886644) // 锤柄
  // 腿
  fillRect(gfx, S(-3.5), S(8), S(3), S(5), 0x553322)
  fillRect(gfx, S(0.5), S(8), S(3), S(5), 0x553322)
  // 靴
  fillRect(gfx, S(-4), S(12), S(4), S(2), 0x553311)
  fillRect(gfx, S(0), S(12), S(4), S(2), 0x553311)
  // 炉火反光
  gfx.fillStyle(0xff6600, 0.15)
  gfx.fillCircle(0, S(4), S(12))
}

// ==================== 六、商人NPC（1种） ====================

// ---- 21. 商人 (Shop Merchant) ----
export function drawShopMerchant(gfx, s) {
  const S = (v) => v * s

  // 脸
  fillCircle(gfx, 0, S(-6), S(5), 0xddbb99)
  // 绿色尖帽
  fillTriangle(gfx, 0, S(-16), S(-5), S(-9), S(5), S(-9), 0x665544)
  // 帽檐
  fillRect(gfx, S(-6), S(-10), S(12), S(2), 0x447744)
  // 黑眼 + 精光
  fillCircle(gfx, S(-2), S(-7), S(1.5), 0x222222)
  fillCircle(gfx, S(2), S(-7), S(1.5), 0x222222)
  fillCircle(gfx, S(-2.5), S(-7.5), S(0.5), 0xffffff)
  fillCircle(gfx, S(1.5), S(-7.5), S(0.5), 0xffffff)
  // 金色倒三角鼻
  fillTriangle(gfx, 0, S(-4), S(-1.5), S(-5.5), S(1.5), S(-5.5), 0xcc8844)
  // 绿色长袍
  fillTriangle(gfx, S(-7), S(-1), S(7), S(-1), 0, S(14), 0x228833)
  // 袍内亮绿
  fillTriangle(gfx, S(-5), S(0), S(5), S(0), 0, S(11), 0x33aa44)
  // 金色钱袋
  fillEllipse(gfx, S(6), S(4), S(6), S(5), 0xffdd00)
  // 袋绳
  gfx.lineStyle(S(0.8), 0x886644, 1)
  gfx.beginPath()
  gfx.moveTo(S(6), S(2)); gfx.lineTo(S(5), S(-1))
  gfx.strokePath()
  // 手臂
  fillRect(gfx, S(-7), S(0), S(3), S(5), 0x886644)
  fillRect(gfx, S(4), S(-1), S(3), S(5), 0x886644)
  // 手
  fillCircle(gfx, S(-6), S(6), S(2), 0xddbb99)
  fillCircle(gfx, S(6), S(1), S(2), 0xddbb99)
  // 金链
  gfx.lineStyle(S(1), 0xffdd00, 0.8)
  gfx.beginPath()
  gfx.moveTo(S(-2), S(-2)); gfx.lineTo(S(0), S(-1)); gfx.lineTo(S(2), S(-2))
  gfx.strokePath()
  fillCircle(gfx, 0, S(-1), S(1.5), 0xffdd00)
  // 腿
  fillRect(gfx, S(-3), S(13), S(2.5), S(4), 0x553311)
  fillRect(gfx, S(0.5), S(13), S(2.5), S(4), 0x553311)
  // 靴
  fillRect(gfx, S(-3.5), S(16), S(3.5), S(2), 0x442211)
  fillRect(gfx, S(0), S(16), S(3.5), S(2), 0x442211)
}

// ==================== 七、物品图标绘制（掉落物/背包/商店用） ====================

// ---- 22. 药水 (Potion) ----
export function drawPotion(gfx, s) {
  const S = (v) => v * s
  // 瓶身
  fillRect(gfx, S(-4), S(-2), S(8), S(10), 0x88ccff, 0.9)
  // 瓶颈
  fillRect(gfx, S(-2), S(-5), S(4), S(4), 0x88ccff, 0.9)
  // 瓶塞
  fillRect(gfx, S(-2.5), S(-6), S(5), S(2), 0x8B4513)
  // 红色药水液体
  fillRect(gfx, S(-3), S(1), S(6), S(6), 0xff4444, 0.8)
  // 液体高光
  fillRect(gfx, S(-2), S(2), S(2), S(4), 0xff8888, 0.5)
  // 瓶口反光
  gfx.lineStyle(S(1), 0xffffff, 0.3)
  gfx.beginPath()
  gfx.moveTo(S(-3), S(-4)); gfx.lineTo(S(3), S(-4))
  gfx.strokePath()
}

// ---- 23. 铁剑 (Iron Sword) ----
export function drawIronSword(gfx, s) {
  const S = (v) => v * s
  // 剑刃
  fillTriangle(gfx, S(0), S(-16), S(-2), S(2), S(2), S(2), 0xC0C0C0)
  // 剑刃中线高光
  gfx.lineStyle(S(1), 0xffffff, 0.4)
  gfx.beginPath()
  gfx.moveTo(S(0), S(-15)); gfx.lineTo(S(0), S(1))
  gfx.strokePath()
  // 剑格（护手）
  fillRect(gfx, S(-5), S(2), S(10), S(2), 0x8B7355)
  // 剑柄
  fillRect(gfx, S(-1.5), S(4), S(3), S(6), 0x654321)
  // 剑柄缠绕纹
  gfx.lineStyle(S(0.8), 0x887766, 0.6)
  for (let i = 0; i < 3; i++) {
    const y = S(5 + i * 1.5)
    gfx.beginPath()
    gfx.moveTo(S(-1), y); gfx.lineTo(S(1), y)
    gfx.strokePath()
  }
  // 剑首（尾端圆球）
  fillCircle(gfx, 0, S(10), S(2), 0x8B7355)
}

// ---- 24. 铁盾 (Iron Shield) ----
export function drawIronShield(gfx, s) {
  const S = (v) => v * s
  // 盾牌外形
  fillEllipse(gfx, 0, 0, S(16), S(20), 0x888888)
  // 金属边框
  gfx.lineStyle(S(2), 0x666666, 1)
  gfx.strokeEllipse(0, 0, S(16), S(20))
  // 十字金属纹
  fillRect(gfx, S(-1), S(-9), S(2), S(18), 0xAAAAAA, 0.6)
  fillRect(gfx, S(-6), S(-1), S(12), S(2), 0xAAAAAA, 0.6)
  // 中心铆钉
  fillCircle(gfx, 0, 0, S(2.5), 0xBBBBBB)
  fillCircle(gfx, 0, 0, S(1.5), 0xDDDDDD)
  // 边缘铆钉
  const rivets = [
    [0, -8], [5, -5], [7, 0], [5, 5], [0, 8],
    [-5, 5], [-7, 0], [-5, -5]
  ]
  rivets.forEach(([rx, ry]) => {
    fillCircle(gfx, S(rx), S(ry), S(1), 0xAAAAAA)
  })
}

// ---- 25. 暗影披风 (Shadow Cloak) ----
export function drawShadowCloak(gfx, s) {
  const S = (v) => v * s
  // 披风主体（暗紫色）
  fillTriangle(gfx, S(-10), S(-8), S(10), S(-8), S(0), S(14), 0x2A0A3A)
  // 内层更深
  fillTriangle(gfx, S(-7), S(-5), S(7), S(-5), S(0), S(11), 0x1A0530)
  // 暗影能量纹路
  gfx.lineStyle(S(1), 0x6633AA, 0.3)
  gfx.beginPath()
  gfx.moveTo(S(-5), S(-3)); gfx.lineTo(S(-2), S(2)); gfx.lineTo(S(-4), S(7))
  gfx.strokePath()
  gfx.beginPath()
  gfx.moveTo(S(5), S(-3)); gfx.lineTo(S(2), S(2)); gfx.lineTo(S(4), S(7))
  gfx.strokePath()
  // 暗影雾气光晕
  fillCircle(gfx, S(0), S(2), S(10), 0x6633AA, 0.08)
  fillCircle(gfx, S(-3), S(0), S(6), 0x8844CC, 0.06)
  fillCircle(gfx, S(3), S(3), S(7), 0x8844CC, 0.06)
  // 领扣
  fillCircle(gfx, 0, S(-7), S(2), 0x888888)
  fillCircle(gfx, 0, S(-7), S(1), 0xAAAAAA)
}

// ---- 26. 生命戒指 (Life Ring) ----
export function drawLifeRing(gfx, s) {
  const S = (v) => v * s
  // 翠绿色光晕
  fillCircle(gfx, 0, 0, S(14), 0x44CC44, 0.08)
  fillCircle(gfx, 0, 0, S(12), 0x44CC44, 0.05)
  // 戒指外圈
  gfx.lineStyle(S(3), 0x88DD88, 1)
  gfx.strokeCircle(0, 0, S(8))
  // 戒指内圈
  gfx.lineStyle(S(1.5), 0xAAEEAA, 0.8)
  gfx.strokeCircle(0, 0, S(7))
  // 生命之树枝叶纹路
  gfx.lineStyle(S(1), 0x66BB66, 0.5)
  gfx.beginPath()
  gfx.moveTo(S(-3), S(-6)); gfx.lineTo(S(-5), S(-3)); gfx.lineTo(S(-6), S(0))
  gfx.strokePath()
  gfx.beginPath()
  gfx.moveTo(S(3), S(-6)); gfx.lineTo(S(5), S(-3)); gfx.lineTo(S(6), S(0))
  gfx.strokePath()
  gfx.beginPath()
  gfx.moveTo(S(-4), S(4)); gfx.lineTo(S(0), S(7)); gfx.lineTo(S(4), S(4))
  gfx.strokePath()
  // 绿叶点缀
  fillCircle(gfx, S(-5), S(-3), S(1.5), 0x88FF88, 0.7)
  fillCircle(gfx, S(5), S(-3), S(1.5), 0x88FF88, 0.7)
  fillCircle(gfx, S(0), S(7), S(1.5), 0x88FF88, 0.7)
  // 顶部绿色宝石
  fillCircle(gfx, 0, S(-8), S(2.5), 0x66EE66, 0.9)
  fillCircle(gfx, 0, S(-8), S(1.5), 0xAAFFAA, 0.7)
}

// ---- 27. 元素项链 (Element Necklace) ----
export function drawElementNecklace(gfx, s) {
  const S = (v) => v * s
  // 四色元素光晕
  fillCircle(gfx, S(-4), S(-2), S(8), 0xFF4444, 0.06)
  fillCircle(gfx, S(4), S(-2), S(8), 0x4444FF, 0.06)
  fillCircle(gfx, S(-3), S(5), S(8), 0xFFFF44, 0.06)
  fillCircle(gfx, S(3), S(5), S(8), 0x44FF44, 0.06)
  // 金色链子
  gfx.lineStyle(S(1.5), 0xDDAA44, 0.8)
  gfx.beginPath()
  gfx.moveTo(S(-6), S(-10)); gfx.lineTo(S(-3), S(-8)); gfx.lineTo(S(0), S(-9))
  gfx.lineTo(S(3), S(-8)); gfx.lineTo(S(6), S(-10))
  gfx.strokePath()
  // 吊坠底座
  fillEllipse(gfx, 0, S(2), S(10), S(10), 0x886644)
  // 四元素宝石
  fillCircle(gfx, S(-3), S(-1), S(3), 0xFF3333, 0.9)
  fillCircle(gfx, S(3), S(-1), S(3), 0x3333FF, 0.9)
  fillCircle(gfx, S(-2), S(5), S(3), 0xFFFF33, 0.9)
  fillCircle(gfx, S(2), S(5), S(3), 0x33FF33, 0.9)
  // 中心主石（白色）
  fillCircle(gfx, 0, S(2), S(2.5), 0xFFFFFF, 0.9)
  fillCircle(gfx, 0, S(2), S(1.5), 0xFFEE88, 0.8)
  // 宝石高光
  fillCircle(gfx, S(-3.5), S(-2), S(1), 0xFFFFFF, 0.3)
  fillCircle(gfx, S(2.5), S(-2), S(1), 0xFFFFFF, 0.3)
  fillCircle(gfx, S(-2.5), S(4), S(1), 0xFFFFFF, 0.3)
  fillCircle(gfx, S(1.5), S(4), S(1), 0xFFFFFF, 0.3)
}

// ==================== 七点五、星云传送门绘制 ====================

/**
 * 绘制星云状传送门
 * 使用多层半透明椭圆、星点和螺旋流线模拟流动的星云效果
 * @param {Phaser.GameObjects.Graphics} gfx
 * @param {number} w - 传送门宽度
 * @param {number} h - 传送门高度
 */
export function drawNebulaPortal(gfx, w, h) {
  // 从外到内的多层星云椭圆
  const layers = [
    { color: 0x6633cc, alpha: 0.20, sw: 1.00, sh: 1.00 },
    { color: 0xcc44aa, alpha: 0.18, sw: 0.88, sh: 0.88 },
    { color: 0x4488ff, alpha: 0.22, sw: 0.76, sh: 0.76 },
    { color: 0xaa66ff, alpha: 0.25, sw: 0.62, sh: 0.62 },
    { color: 0xff88cc, alpha: 0.20, sw: 0.48, sh: 0.48 },
    { color: 0xddbbff, alpha: 0.30, sw: 0.32, sh: 0.32 },
    { color: 0xffffff, alpha: 0.15, sw: 0.18, sh: 0.18 },
  ]

  layers.forEach(l => {
    fillEllipse(gfx, 0, 0, w * l.sw, h * l.sh, l.color, l.alpha)
  })

  // 散落星点
  const starColors = [0xffffff, 0xaaccff, 0xffddaa, 0xffaaff]
  const starSeeds = [0.123, 0.456, 0.789, 0.234, 0.567, 0.890, 0.345, 0.678]
  const hw = w / 2
  const hh = h / 2
  starSeeds.forEach((seed, i) => {
    const x = ((seed * 2.17) % 1 * 2 - 1) * hw * 0.7
    const y = ((seed * 3.71) % 1 * 2 - 1) * hh * 0.7
    const radius = 0.8 + ((seed * 5.33) % 3)
    const color = starColors[i % starColors.length]
    const alpha = 0.4 + ((seed * 7.19) % 1) * 0.5
    fillCircle(gfx, x, y, radius, color, alpha)
  })

  // 螺旋流线（旋转星云轨迹）
  const arcColor = 0xccaaff
  const arcAlpha = 0.25
  gfx.lineStyle(1.5, arcColor, arcAlpha)
  for (let a = 0; a < 3; a++) {
    const angleOffset = a * Math.PI * 2 / 3
    gfx.beginPath()
    for (let t = 0; t <= 20; t++) {
      const p = t / 20
      const ang = angleOffset + p * Math.PI * 1.5
      const baseRadius = Math.min(hw, hh) * (0.3 + p * 0.6)
      const px = Math.cos(ang) * baseRadius * (w > h ? 1 : 0.6)
      const py = Math.sin(ang) * baseRadius * (h > w ? 1 : 0.6)
      if (t === 0) gfx.moveTo(px, py)
      else gfx.lineTo(px, py)
    }
    gfx.strokePath()
  }

  // 外部柔光晕
  fillEllipse(gfx, 0, 0, w * 1.15, h * 1.15, 0x6633cc, 0.06)
}

// ==================== 八、怪物匹配与渲染调度 ====================

/**
 * 根据怪物名称和特殊类型获取对应的绘制函数
 * @param {string} monName - 怪物名称
 * @param {string} specialType - 特殊类型标识
 * @returns {Function|null} 绘制函数
 */
export function getMonsterDrawer(monName, specialType) {
  if (!monName) return null
  const nm = monName.toLowerCase()

  // 火焰史莱姆（特殊类型优先）
  if (specialType === 'FLAME_SLIME') return drawFlameSlime

  // 普通怪物
  if (nm.includes('哥布林') || nm.includes('goblin')) return drawGoblin
  if (nm.includes('史莱姆') || nm.includes('slime')) return drawSlime
  if (nm.includes('骷髅') || nm.includes('skeleton')) return drawSkeleton
  if (nm.includes('狼人') || nm.includes('werewolf')) return drawWerewolf
  if (nm.includes('食人魔') || nm.includes('ogre')) return drawOgre

  // 精英怪物
  if (nm.includes('暗影骑士') || (nm.includes('shadow') && !nm.includes('king'))) return drawShadowKnight
  if (nm.includes('地狱犬') || nm.includes('hell') || nm.includes('hound')) return drawHellHound
  if (nm.includes('石像鬼') || nm.includes('gargoyle')) return drawGargoyle
  if (nm.includes('暗黑法师') || nm.includes('dark mage') || nm.includes('dark') || nm.includes('mage')) return drawDarkMage
  if (nm.includes('巨型蜘蛛') || nm.includes('giant spider') || nm.includes('spider')) return drawGiantSpider

  // Boss
  if (nm.includes('巨龙') || nm.includes('dragon')) return drawDragon
  if (nm.includes('恶魔领主') || nm.includes('demon lord') || nm.includes('demon')) return drawDemonLord
  if (nm.includes('巫妖王') || nm.includes('lich')) return drawLichKing
  if (nm.includes('石魔像') || nm.includes('golem')) return drawGolem
  if (nm.includes('暗影之王') || (nm.includes('shadow') && nm.includes('king'))) return drawShadowKing

  // 默认哥布林
  return drawGoblin
}

/**
 * 根据奇遇事件类型获取对应的NPC绘制函数
 * @param {string} eventType - 事件类型 (CHEST, MAIDEN, ANGEL, BLACKSMITH)
 * @returns {Function|null} 绘制函数
 */
export function getEncounterDrawer(eventType) {
  const map = {
    CHEST: drawChest,
    MAIDEN: drawMaiden,
    ANGEL: drawAngel,
    BLACKSMITH: drawBlacksmith
  }
  return map[eventType] || null
}

/**
 * 根据物品名称获取对应的图标绘制函数
 * @param {string} itemName - 物品名称
 * @returns {Function|null} 绘制函数（签名: drawFn(gfx, scale)）
 */
export function getItemDrawer(itemName) {
  if (!itemName) return null
  if (itemName.includes('药水')) return drawPotion
  if (itemName.includes('铁剑') || (itemName.includes('剑') && itemName.includes('铁'))) return drawIronSword
  if (itemName.includes('铁盾') || (itemName.includes('盾') && itemName.includes('铁'))) return drawIronShield
  if (itemName.includes('暗影披风') || itemName.includes('披风')) return drawShadowCloak
  if (itemName.includes('生命戒指') || itemName.includes('戒指')) return drawLifeRing
  if (itemName.includes('元素项链') || itemName.includes('项链')) return drawElementNecklace
  // 生命浆果/魔力浆果/饼干等消耗品不覆盖，保持原来风格
  return null
}
