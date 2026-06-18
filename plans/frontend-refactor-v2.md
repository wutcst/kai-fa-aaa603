# 前端重构完整方案（修订版）

## 当前状态分析

### 已完成：模块文件创建（9个文件）

```
frontend/src/game/
├── helpers/
│   └── VisualEffects.js     (117行) — 攻击特效
├── ui/
│   ├── HUD.js               (93行)  — HP/MP条
│   ├── BuffDisplay.js       (150行) — 烧伤/中毒
│   ├── AltarUI.js           (105行) — 祭坛浮层
│   └── ShopUI.js            (276行) — 商店菜单
└── systems/
    ├── MovementSystem.js    (46行)  — WASD移动
    ├── MonsterAI.js         (73行)  — 怪物AI
    └── CollisionSystem.js   (198行) — 碰撞检测
```

### 已添加到 GameCanvas.vue 的导入

```javascript
import { drawArcSlash, ... } from '../game/helpers/VisualEffects.js'
import { createHUD, updatePlayerBars as hudUpdatePlayerBars } from '../game/ui/HUD.js'
import { initBuffDisplay, updateBuffDisplay as ... } from '../game/ui/BuffDisplay.js'
import { showShopMenu, showShopBuy, showShopSell } from '../game/ui/ShopUI.js'
import { spawnAltarGlow, showWisdomOverlay } from '../game/ui/AltarUI.js'
import { handleMovement } from '../game/systems/MovementSystem.js'
import { updateMonsterAI } from '../game/systems/MonsterAI.js'
import { updateCollisions, ... } from '../game/systems/CollisionSystem.js'
```

### 未完成：内联代码替换（核心任务）

**GameCanvas.vue 中仍有大量内联代码未被替换为模块调用**，这是重构的核心工作。

## 文件尺寸现状

| 文件 | 当前行数 | 目标行数 |
|------|---------|---------|
| GameCanvas.vue | **~4276行** | **~1500行**（减少65%）|
| 9个模块文件 | ~1100行总计 | ~1100行（不变）|

**重构后总计：~2600行**（原来4276行）

## 需要替换的内联代码清单（按优先级排序）

### 高优先级（安全，无副作用）

| # | 内联代码位置 | 行数 | 替换为 | 风险 |
|---|------------|------|--------|------|
| 1 | HP/MP 状态条创建（~54行） | 757-809 | `createHUD(scene)` | 低 |
| 2 | scene.updatePlayerBars 函数（~45行） | 814-858 | `hudUpdatePlayerBars(scene, ...)` | 低 |
| 3 | Buff 初始化（~18行） | 788-805 | `initBuffDisplay(scene)` | 低 |
| 4 | Buff 更新函数（~85行） | 862-947 | `buffUpdateBuffDisplay(scene, effects)` | 低 |

### 中优先级（需要传递 emit 参数）

| # | 内联代码位置 | 行数 | 替换为 | 风险 |
|---|------------|------|--------|------|
| 5 | showShopMenu（~55行） | ~1470行 | `showShopMenu(scene, emit)` | 中 |
| 6 | showShopBuy（~170行） | ~1505行 | `showShopBuy(scene, emit)` | 中 |
| 7 | showShopSell（~200行） | ~1927行 | `showShopSell(scene, emit)` | 中 |
| 8 | showWisdomOverlay（~35行） | ~1414行 | `showWisdomOverlay(scene)` | 低 |
| 9 | spawnAltarGlow（~25行） | ~1387行 | `spawnAltarGlow(...)` | 低 |

### 次高优先级（AI/Movement 系统替换）

| # | 内联代码位置 | 行数 | 替换为 | 风险 |
|---|------------|------|--------|------|
| 10 | WASD移动逻辑（~28行） | ~2292行 | `handleMovement(scene, dt)` | 中 |
| 11 | 怪物AI循环（~70行） | ~2314行 | `updateMonsterAI(scene, dt, emit)` | 中 |
| 12 | 碰撞回避+门检测（~80行） | ~2568+行 | `updateCollisions(scene)` | 中 |
| 13 | 祭坛交互（~40行） | ~2609行 | `updateAltarInteraction(scene)` | 中 |
| 14 | 掉落物交互（~30行） | ~2649行 | `updateDropInteraction(scene)` | 中 |
| 15 | 商店NPC交互（~35行） | ~2679行 | `updateShopInteraction(scene)` | 中 |

### 低优先级（攻击/月光波系统—高度耦合不拆）

| # | 内联代码位置 | 行数 | 处理方式 | 风险 |
|---|------------|------|----------|------|
| 16 | J键攻击（~250行） | ~2730行 | **暂不拆分** | 高 |
| 17 | 月光波（~150行） | ~2980行 | **暂不拆分** | 高 |
| 18 | 攻击特效函数（~200行） | ~1649行 | **已提取但内联版本仍保留** | 低 |

## 执行策略

### 策略：分 4 轮逐步替换，每轮编译验证

```
第1轮：替换 HP/MP 条 + Buff（高优，无副作用）
第2轮：替换 商店UI + 祭坛UI（中优，需传 emit）
第3轮：替换 Movement + MonsterAI + Collision（中优）
第4轮：替换 特效函数引用，清理冗余代码
```

### 每轮的具体操作

**第1轮** — 修改 GameCanvas.vue 中4处代码块，每处替换后独立验证

**第2轮** — 修改5处店铺/祭坛代码，注意保留 `emit` 参数传递

**第3轮** — 替换 update 循环中的6个代码段，注意 `dt` 变量作用域

**第4轮** — 最终整理，清理注释，编译验证

## 回退方案

如果 apply_diff 在某个步骤失败：
1. 记下失败的行号和目标内容
2. 用 read_file 确认当前文件内容
3. 缩小 SEARCH 块到3-5行，逐步替换
4. 或者直接用 VS Code 手动编辑那一段

## 预估结果

| 轮次 | 预计减少行数 | GameCanvas.vue 剩余 |
|------|------------|-------------------|
| 第1轮 | -~200行 | ~4076行 |
| 第2轮 | -~460行 | ~3616行 |
| 第3轮 | -~260行 | ~3356行 |
| 第4轮 | -~100行 | ~3256行 |

> **注意**：攻击系统（~250行）和月光波（~150行）高度耦合，本次不拆分。其他模块全部替换后 GameCanvas.vue 降至约 **3200行**。
