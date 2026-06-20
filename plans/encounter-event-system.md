# 奇遇事件系统（Encounter Event System）

## 概述

奇遇事件是游戏中 `ENCOUNTER` 类型房间内生成的随机可交互事件。每个奇遇房间在首次初始化时基于房间名哈希种子，按权重随机生成 4 种事件之一。玩家靠近 NPC 后按 **空格键** 触发交互，交互后事件标记为 `used`，NPC 图形变为半透明隐藏状态。

---

## 权重分布

| 事件 | 英文标识 | 权重 | 概率范围 |
|------|---------|------|---------|
| 🏆 宝箱 | `CHEST` | **40%** | `roll < 40` |
| 💖 圣女 | `MAIDEN` | **25%** | `40 ≤ roll < 65` |
| 🔨 铁匠 | `BLACKSMITH` | **25%** | `65 ≤ roll < 90` |
| 🕊️ 天使 | `ANGEL` | **10%** | `roll ≥ 90` |

> 权重实现在 [`Room.java:376-387`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java:376)

---

## 四种事件详解

### 1️⃣ 宝箱（CHEST）

| 属性 | 值 |
|------|-----|
| 颜色 | `0xDAA520`（金色） |
| 图标 | 💰 金色箱体 |
| 效果 | 获得 **200 枚钱币** |
| 后端方法 | [`InteractCommand.java:146-159`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:146) — `handleChest()` |
| 前端绘制 | [`EntityDrawer.js:922-954`](../frontend/src/entity/EntityDrawer.js:922) — `drawChest()` |
| 策略定位 | **经济收益**，概率最高，适合前期积累启动资金 |

**对话流程：**
1. 发现华丽宝箱
2. 打开瞬间金色光芒闪耀
3. "幸运的冒险者，这是属于你的财富"
4. 获得 200 枚钱币

---

### 2️⃣ 圣女（MAIDEN）

| 属性 | 值 |
|------|-----|
| 颜色 | `0xFFB6C1`（粉红） |
| 图标 | 👼 粉白长裙少女 |
| 效果 | **HP 回满 + MP 回满** |
| 后端方法 | [`InteractCommand.java:164-176`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:164) — `handleMaiden()` |
| 前端绘制 | [`EntityDrawer.js:957-987`](../frontend/src/entity/EntityDrawer.js:957) — `drawMaiden()` |
| 策略定位 | **续航保障**，残血残蓝时救急 |

**对话流程：**
1. 圣洁少女出现，周身散发柔和白色光芒
2. 少女微笑伸出双手，温暖光芒笼罩全身
3. "愿光明与你同在，勇敢的冒险者"
4. 生命值和魔力值完全恢复

---

### 3️⃣ 天使（ANGEL）

| 属性 | 值 |
|------|-----|
| 颜色 | `0xFFD700`（金色） |
| 图标 | 🕊️ 金色翅膀天使 |
| 效果 | 全属性上浮至 **150%**，持续 **30 秒** |
| 后端方法 | [`InteractCommand.java:181-192`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:181) — `handleAngel()` |
| 前端绘制 | [`EntityDrawer.js:990-1019`](../frontend/src/entity/EntityDrawer.js:990) — `drawAngel()` |
| Buff 实现 | [`Status.java:176-183`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/Status.java:176) — `applyAngelBuff()`，持续时间 `ANGEL_BUFF_DURATION = 30000L` |
| 视觉特效 | 角色身上浮现 **金色光芒** |
| 策略定位 | **爆发性增益**，概率最低（10%）效果最强，打 BOSS 前寻找天使房间收益最大 |

**对话流程：**
1. 耀眼的光芒从天而降，天使降临
2. 天使展开金色羽翼，圣洁光辉洒落
3. "勇敢的战士，接受我的祝福吧"
4. 所有属性提升至 150%

---

### 4️⃣ 铁匠（BLACKSMITH）

| 属性 | 值 |
|------|-----|
| 颜色 | `0x888888`（灰色） |
| 图标 | 🔨 铁匠锤砧 |
| 效果 | **强化一件装备/饰品，属性 +50%** |
| 前端绘制 | [`EntityDrawer.js:1022-1057`](../frontend/src/entity/EntityDrawer.js:1022) — `drawBlacksmith()` |

#### 两种情况

**情况 A — 有装备可强化：**

- 后端方法：[`showBlacksmithOptions()`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:197)
- 收集玩家背包/已装备中的所有饰品和装备
- 前端显示可点击物品列表按钮
- 玩家选择后执行：[`handleBlacksmithChoose()`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:249)

各物品强化效果：

| 物品 | 强化效果 |
|------|---------|
| 暗影披风 | 闪避 +7，速度 +10 |
| 生命戒指 | 最大生命 +25 |
| 元素项链 | 魔攻 +7，魔抗 +10 |
| 通用装备 | 攻击力 +15% |

强化后事件标记为 `used = true`。

**情况 B — 无装备可强化：**

- 铁匠拒绝："连件像样的家伙什儿都没有"
- **事件不消耗**（不标记 `used`），玩家获取装备后可再次回来
- 后端方法：返回空 `blacksmithItems` 列表

**对话流程：**
1. 老铁匠坐在熔炉旁敲打
2. 热情招呼选择要强化的装备
3. 熔炉火焰升腾，铁匠挥汗如雨
4. 强化完成："比原来强了一半还多！"

---

## 完整交互流程

```
进入奇遇房间
  │
  ├─ Room.initRandomEvent() → 基于房间名 hash 种子 + 权重随机
  │    └─ 决定 CHEST / MAIDEN / ANGEL / BLACKSMITH
  │
  ├─ 前端渲染 NPC 图形（encounterNpcCircle + Label + Indicators）
  │
  ├─ 玩家靠近 NPC（间距 < 60px）
  │    └─ 显示"按空格键交互"提示标签
  │
  ├─ 玩家按 SPACE
  │    ├─ 如果 dialog 已打开 → 关闭 dialog
  │    ├─ 如果 event.used → 提示"已经触发过了"
  │    └─ 否则 → 发送 interact event
  │         └─ 后端 InteractCommand.handleRandomEvent()
  │              ├─ CHEST → handleChest() → +200 coins
  │              ├─ MAIDEN → handleMaiden() → 满血满蓝
  │              ├─ ANGEL → handleAngel() → 天使 Buff 30s
  │              └─ BLACKSMITH
  │                   ├─ 无装备 → 提示无装备，不消耗 used
  │                   └─ 有装备 → 返回 blacksmithItems 列表
  │                        └─ 前端显示物品按钮
  │                             └─ 点击 → 发送 interact blacksmith <itemName>
  │                                  └─ handleBlacksmithChoose() → 强化 +50%
  │
  ├─ 前端弹出主题对话框（showEncounterDialog）
  │    ├─ 10 秒倒计时自动关闭
  │    ├─ 按 SPACE 提前关闭
  │    └─ 铁匠有装备时渲染物品选择按钮
  │
  └─ 交互后 event.used = true
       └─ renderRoom 检测到 used → NPC 半透明隐藏
```

---

## 核心代码文件索引

| 文件 | 作用 |
|------|------|
| [`RandomEventType.java`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/RandomEventType.java) | 枚举定义：4 种事件类型、显示名、颜色 |
| [`RandomEvent.java`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/RandomEvent.java) | 事件实体类：类型、used 标记、铁匠目标物品 |
| [`Room.java:367-393`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java:367) | 权重随机初始化 `initRandomEvent()` |
| [`Room.java:398-402`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java:398) | 标记已使用 `useRandomEvent()` |
| [`InteractCommand.java:101-141`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:101) | 奇遇交互入口 `handleRandomEvent()` |
| [`InteractCommand.java:146-159`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:146) | 宝箱处理 |
| [`InteractCommand.java:164-176`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:164) | 圣女处理 |
| [`InteractCommand.java:181-192`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:181) | 天使处理 |
| [`InteractCommand.java:197-244`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:197) | 铁匠展示可选物品 |
| [`InteractCommand.java:249-304`](../backend/src/main/java/cn/edu/whut/sept/zuul/command/InteractCommand.java:249) | 铁匠执行强化 |
| [`Status.java:176-183`](../backend/src/main/java/cn/edu/whut/sept/zuul/model/Status.java:176) | 天使祝福 Buff 实现 |
| [`EntityDrawer.js:922-1057`](../frontend/src/entity/EntityDrawer.js:922) | 四种 NPC 图形绘制 |
| [`GameCanvas.vue`](../frontend/src/components/GameCanvas.vue) | 前端渲染、交互逻辑、对话框系统 |
