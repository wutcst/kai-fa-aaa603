# GameCanvas.vue 重构方案

## 当前文件结构分析

文件总行数：**~4205 行**

### 按功能区域拆分

| 区域 | 行数范围 | 行数 | 说明 |
|------|---------|------|------|
| **Template** | 1-146 | ~146 | 游戏容器 + 小地图 + 背包UI |
| **Script - Vue 背包逻辑** | 148-365 | ~218 | 背包状态、使用/卸下/丢弃、B键监听 |
| **Script - 小地图** | 367-541 | ~175 | 地图布局、画布绘制、初始化 |
| **Script - Phaser 场景** | 543-3838 | ~3296 | **整个游戏引擎逻辑** |
| **Script - 生命周期** | 3840-3858 | ~19 | onBeforeUnmount 清理 |
| **Style** | 3861-4204 | ~344 | 背包UI样式 |

### Phaser 场景内部（3296行）

| 子模块 | 行数 | 说明 |
|--------|------|------|
| preload | ~15 | 资源加载 |
| create - 初始化 | ~1300 | 背景、HP/MP条、Buff、renderRoom、祭坛、商店UI、攻击特效 |
| create - update 循环 | ~1200 | 烧伤/中毒倒计时、轮询、蓄力、移动、怪物AI、攻击、击退、月光波、碰撞检测、交互 |
| create - 事件 & 初始加载 | ~50 | 事件监听、获取初始状态 |
| destroy | ~5 | 清理 |
| Phaser game 初始化 | ~40 | new Phaser.Game + 小地图/事件注册 |

## 重构方案

### 总体思路：按职责拆分为独立文件

```
frontend/src/
├── components/
│   └── GameCanvas.vue          ← 精简后（~800行）
├── game/
│   ├── index.js                ← Phaser Game 启动
│   ├── scene/
│   │   ├── BootScene.js        ← preload
│   │   └── GameScene.js        ← 主游戏场景（拆分后~1500行）
│   ├── ui/
│   │   ├── HUD.js              ← HP/MP 状态条
│   │   ├── BuffDisplay.js      ← 烧伤/中毒显示
│   │   ├── ShopUI.js           ← 商店菜单/购买/售卖
│   │   ├── AltarUI.js          ← 祭坛交互浮层
│   │   └── Minimap.js          ← 小地图（从Vue移到Phaser）
│   ├── systems/
│   │   ├── MonsterAI.js        ← 怪物索敌、追击、攻击
│   │   ├── AttackSystem.js     ← 攻击命中、击退
│   │   ├── WaveSystem.js       ← 月光波弹射
│   │   ├── MovementSystem.js   ← WASD 移动
│   │   └── CollisionSystem.js  ← 门检测、碰撞回避
│   └── helpers/
│       ├── GrassGenerator.js   ← 草地纹理生成
│       └── VisualEffects.js    ← 攻击特效、粒子、火焰
```

### 第一步：拆分 Phaser 场景（影响最小、收益最大）

不改变 Vue 架构，只把 Phaser 内部的 `create()` 代码拆到多个 JS 文件。

#### 新建文件清单

| 文件 | 职责 | 大约行数 |
|------|------|---------|
| `src/game/helpers/GrassGenerator.js` | 草地纹理生成、`createSeamlessGrass` | 60 |
| `src/game/helpers/VisualEffects.js` | `drawArcSlash`, `drawFireDistortion`, `spawnShockwave`, `spawnAttackParticles` | 100 |
| `src/game/ui/HUD.js` | HP/MP/金钱条、`updatePlayerBars` | 100 |
| `src/game/ui/BuffDisplay.js` | 烧伤火焰、中毒图标、`updateBuffDisplay` | 120 |
| `src/game/ui/ShopUI.js` | `showShopMenu`, `showShopBuy`, `showShopSell` | 200 |
| `src/game/ui/AltarUI.js` | `showWisdomOverlay`, `spawnAltarGlow` | 80 |
| `src/game/systems/MonsterAI.js` | 怪物AI循环（索敌、追击、攻击） | 80 |
| `src/game/systems/AttackSystem.js` | J键攻击、突刺、横扫、击退 | 250 |
| `src/game/systems/WaveSystem.js` | 月光波蓄力、发射、弹射运动 | 150 |
| `src/game/systems/MovementSystem.js` | WASD移动、房门检测 | 60 |
| `src/game/systems/CollisionSystem.js` | 祭坛/商店NPC碰撞回避、掉落物检测 | 80 |

#### 拆分后 GameScene.js 结构

```javascript
// GameScene.js (约1500行)
import { createSeamlessGrass } from '../helpers/GrassGenerator.js'
import { drawArcSlash, ... } from '../helpers/VisualEffects.js'
import { createHUD, updatePlayerBars } from '../ui/HUD.js'
// ... 更多 import

class GameScene extends Phaser.Scene {
  create() {
    // 1. 初始化容器、组（~50行）
    this.initGroups()
    
    // 2. 生成草地纹理（~10行）
    createSeamlessGrass(this, ...)
    
    // 3. 创建背景（~30行）
    this.createBackground()
    
    // 4. 创建HUD（~10行）
    createHUD(this)
    
    // 5. 定义 renderRoom（~200行）
    this.renderRoom = function(roomInfo) { ... }
    
    // 6. 定义商店/祭坛UI（~30行，具体实现在各UI模块）
    ShopUI.init(this)
    AltarUI.init(this)
    
    // 7. 定义攻击/月光波系统（~30行）
    AttackSystem.init(this)
    WaveSystem.init(this)
    
    // 8. 键盘控制 + update循环（~500行）
    // 键盘控制
    // update: 倒计时 → 轮询 → 蓄力 → 移动 → 怪物AI → 攻击 → 击退 → 月光波 → 碰撞 → 交互
    
    // 9. 获取初始状态（~15行）
  }
}
```

### 第二步：拆分 Vue 背包 UI（可选）

如果背包UI继续膨胀，也可以拆成子组件：

| 组件 | 说明 |
|------|------|
| `BackpackPanel.vue` | 背包面板整体 |
| `BackpackSlot.vue` | 单个物品格子 |
| `ItemDetail.vue` | 物品详情面板 |

## 建议执行顺序

1. **先拆分 Phaser 场景**（10个新文件，GameCanvas.vue 从4200行降到~800行）
2. **编译验证**确保不影响功能
3. **根据情况决定**是否继续拆分 Vue 背包组件

每个系统独立文件更易于维护、测试和后续扩展。如果需要开始实施，我切换到 Code 模式逐步完成。
