# ZUUL 游戏 API 文档

> **项目**: ZUUL — 失落的古迹  
> **版本**: v1.0.0  
> **更新日期**: 2026-06-20

---

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`

### 统一响应格式

```json
{
  "status": "success | error",
  "message": "提示信息或错误信息",
  "data": {}
}
```

### 统一房间响应数据字段

以下字段会在 `data` 中返回（除特别说明外，`GET /api/game`、`POST /api/command`、`POST /api/attack`、`POST /api/reset`、`POST /api/load` 均共用此格式）：

#### 房间信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 当前房间名称（如 `"CAMPFIRE_7"`、`"NORMAL_MONSTER_2"`） |
| `description` | string | 房间描述文本 |
| `exits` | string | 可用出口，空格分隔（如 `"east west"`） |
| `items` | array | 房间内地上的物品列表（字符串数组） |
| `monsters` | array | 房间内怪物列表（见下方怪物字段表） |
| `monstersCleared` | boolean | 怪物是否已被全部清除 |
| `hasAliveMonsters` | boolean | 是否有存活的怪物 |
| `isMonsterRoom` | boolean | 是否为怪物房间（需打怪才能离开） |
| `isTeleportRoom` | boolean | 是否为传送房间 |
| `roomType` | string | 房间类型（见下方类型表） |
| `altarUsed` | boolean | 篝火房间的祭坛是否已被使用 |
| `altars` | array | 篝火房间的祭坛列表（见下方Altar字段表） |
| `isShop` | boolean | 是否为商店房间 |
| `shopInitialized` | boolean | 商店商品是否已初始化 |
| `shopItems` | array | 商店商品列表（见下方ShopItem字段表） |
| `isEncounter` | boolean | 是否为奇遇房间 |
| `randomEvent` | object/null | 奇遇事件数据（见下方RandomEvent字段表） |
| `droppedItems` | array | 掉落物列表（见下方DroppedItem字段表） |

#### 玩家状态

| 字段 | 类型 | 说明 |
|------|------|------|
| `playerHp` | number | 玩家当前生命值 |
| `playerMaxHp` | number | 玩家最大生命值 |
| `playerMp` | number | 玩家当前魔力值 |
| `playerMaxMp` | number | 玩家最大魔力值 |
| `playerMoney` | number | 玩家当前货币 |
| `backpack` | array | 背包物品列表（见下方InventoryItem字段表） |
| `activeEffects` | array | 活跃状态效果列表 |
| `effectiveAttack` | number | 修正后攻击力（含装备+状态加成） |
| `effectiveDefense` | number | 修正后防御力 |
| `effectiveMagicAttack` | number | 修正后魔法攻击力 |
| `effectiveMagicResist` | number | 修正后魔法抗性 |
| `effectiveSpeed` | number | 修正后速度 |
| `effectiveDodge` | number | 修正后闪避率 |

#### 状态结算字段（攻击/命令响应中可能出现）

| 字段 | 类型 | 说明 |
|------|------|------|
| `hitCount` | number | （攻击响应）命中怪物数量 |
| `gameOver` | boolean | 玩家是否因流血/中毒等效果死亡 |
| `burnDamage` | number | （可选）烧伤结算伤害数值 |
| `burnLayers` | number | （可选）当前烧伤层数 |
| `poisonDamage` | number | （可选）中毒结算伤害数值 |
| `poisonLayers` | number | （可选）当前中毒层数 |
| `bleedLayers` | number | （可选）当前流血层数 |
| `angelBuffRemainingMs` | number | （可选）天使增益剩余毫秒数 |
| `ringRegen` | number | （可选）生命戒指被动回血量 |

---

#### 怪物字段 (monsters[].Monster)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 怪物全名（含唯一后缀，如 `"goblin#1234"`） |
| `description` | string | 怪物描述 |
| `hp` | number | 当前生命值 |
| `maxHp` | number | 最大生命值 |
| `attack` | number | 攻击力 |
| `defense` | number | 物理防御 |
| `magicResist` | number | 魔法抗性 |
| `speed` | number | 移动速度 |
| `type` | number | 怪物类型：`0`=普通, `1`=精英, `2`=Boss |
| `specialType` | string/null | 特殊类型（如 `"FLAME_SLIME"`） |

#### 祭坛字段 (altars[].Altar)

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | string | 祭坛类型：`HEAL` / `TRAIN` / `WISDOM` |
| `displayName` | string | 显示名称（如 `"治愈祭坛"`） |
| `activated` | boolean | 是否已激活 |
| `color` | number | 颜色值（ARGB格式：`65280`=绿, `16776960`=黄, `16711680`=红） |
| `pendingBoons` | array | 仅 WISDOM 祭坛有：待选择的3个随机恩赐列表（见WisdomBoon） |

#### Wisdom 恩赐字段 (altars[].pendingBoons[].WisdomBoon)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 恩赐英文名（如 `"ENDURANCE"`） |
| `displayName` | string | 恩赐中文名（如 `"坚忍"`） |
| `description` | string | 恩赐描述（如 `"提高30%血量上限"`） |

8种恩赐：

| 名称 | 中文名 | 效果 |
|------|--------|------|
| `ENDURANCE` | 坚忍 | 提高30%血量上限 |
| `VASTNESS` | 浩瀚 | 提高50%魔力上限 |
| `PREPARATION` | 整备 | 提高15点物防 |
| `GUARDIAN` | 守护 | 提高25点魔抗 |
| `VIGOR` | 强健 | 提高15点物攻 |
| `ERUDITION` | 博学 | 提高25点魔攻 |
| `AGILITY` | 敏捷 | 提高50点移速 |
| `GRACE` | 灵动 | 提高25点闪避率 |

#### 商店商品字段 (shopItems[].ShopItem)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 商品名称 |
| `price` | number | 商品价格 |
| `sold` | boolean | 是否已售罄 |

#### 奇遇事件字段 (randomEvent)

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | string | 事件类型：`WOODEN_CHEST` / `GOLDEN_CHEST` / `ELITE_ENEMY` / `FOUNTAIN` / `CHEST` / `MAIDEN` / `ANGEL` / `BLACKSMITH` |
| `used` | boolean | 是否已触发 |
| `additional` | string | （可选）额外信息 |

#### 掉落物字段 (droppedItems[].DroppedItem)

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 物品名称 |
| `x` | number | 掉落物 X 坐标 |
| `y` | number | 掉落物 Y 坐标 |

#### 背包物品字段 (backpack[].InventoryItem)

| 字段 | 类型 | 说明 |
|------|------|------|
| `itemId` | number | 物品唯一 ID |
| `name` | string | 物品名称 |
| `rarity` | string | 稀有度（`common` / `rare` / `epic` / `legendary`） |
| `quantity` | number | 数量 |
| `functionDesc` | string | 功能描述 |
| `loreDesc` | string | 背景描述 |
| `price` | number | 价格 |
| `equipped` | boolean | 是否已装备（仅饰品有效） |

#### 已注册物品一览 (ItemRegistry)

| 名称 | 类型 | 价格 | 效果 | 稀有度 |
|------|------|------|------|--------|
| 生命浆果 | 消耗品 | 25g | 回复25点生命值 | common |
| 魔力浆果 | 消耗品 | 25g | 回复25点魔力值 | common |
| 饼干 | 消耗品 | 40g | 提升最大生命值10点 | common |
| 药水 | 消耗品 | 35g | 回复30点魔力与20点生命 | common |
| 铁剑 | 装备(武器) | 80g | 物理攻击力+15 | common |
| 铁盾 | 装备(护甲) | 70g | 物理防御力+10 | common |
| 暗影披风 | 饰品(披风) | 200g | 闪避率+15%，移动速度+20 | rare |
| 生命戒指 | 饰品(戒指) | 150g | 最大生命值+50，每2秒恢复1点生命 | rare |
| 元素项链 | 饰品(项链) | 150g | 魔法攻击力+15，魔法抗性+20% | epic |

#### 活跃状态效果字段 (activeEffects[].StatusEffect)

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | string | 效果类型：`BURN` / `POISON` / `BLEED` / `ANGEL_BUFF` |
| `name` | string | 显示名称：`"烧伤"` / `"中毒"` / `"流血"` / `"天使祝福"` |
| `isDebuff` | boolean | 是否为减益效果 |
| `layers` | number | 当前层数 |
| `nextTickIn` | number | 下次触发间隔（毫秒） |
| `remainingMs` | number | 剩余持续时间（毫秒，仅ANGEL_BUFF） |

| 状态类型 | 触发机制 | 伤害计算 | 持续方式 |
|----------|----------|----------|----------|
| `BURN` (烧伤) | 每3秒触发 | 魔法伤害 = 层数 × (1 - 魔抗/100)，层数每轮减半 | 可叠加层数 |
| `POISON` (中毒) | 每秒触发 | 真实伤害 = min(层数, 当前HP-1)，层数每轮减1 | 可叠加层数 |
| `BLEED` (流血) | 攻击怪物时触发 | 2点真实伤害，层数每轮减1 | 可叠加层数 |
| `ANGEL_BUFF` (天使祝福) | 持续30秒 | 全属性×1.5倍增益 | 定时结束 |

---

#### 房间类型 (roomType)

| 值 | 说明 |
|----|------|
| `START_HALL` | 起始大厅 |
| `SHOP` | 商店房间 |
| `ENCOUNTER` | 奇遇房间 |
| `CAMPFIRE` | 篝火休息点 |
| `BOSS` | Boss 房间 |
| `ELITE_MONSTER` | 精英怪物房间 |
| `NORMAL_MONSTER` | 普通怪物房间 |

---

## 接口列表

---

### 1. `GET /api/game` — 获取当前游戏状态

无参数。

**响应示例**:

```json
{
  "status": "success",
  "message": "Current game status",
  "data": {
    "name": "CAMPFIRE_7",
    "description": "一个篝火房间，温暖的火光驱散了黑暗。",
    "exits": "east west",
    "items": [],
    "monsters": [],
    "monstersCleared": false,
    "hasAliveMonsters": false,
    "isMonsterRoom": false,
    "isTeleportRoom": false,
    "roomType": "CAMPFIRE",
    "altarUsed": false,
    "altars": [
      { "type": "HEAL", "displayName": "治愈祭坛", "activated": false, "color": 65280, "pendingBoons": [] },
      { "type": "TRAIN", "displayName": "训练祭坛", "activated": false, "color": 16776960, "pendingBoons": [] },
      { "type": "WISDOM", "displayName": "博学祭坛", "activated": false, "color": 16711680, "pendingBoons": [
          { "name": "ENDURANCE", "displayName": "坚忍", "description": "提高30%血量上限" },
          { "name": "VIGOR", "displayName": "强健", "description": "提高15点物攻" },
          { "name": "AGILITY", "displayName": "敏捷", "description": "提高50点移速" }
        ]
      }
    ],
    "isShop": false,
    "shopItems": [],
    "droppedItems": [],
    "isEncounter": false,
    "randomEvent": null,
    "playerHp": 100,
    "playerMaxHp": 100,
    "playerMp": 100,
    "playerMaxMp": 100,
    "playerMoney": 50,
    "backpack": [],
    "activeEffects": [],
    "effectiveAttack": 20,
    "effectiveDefense": 10,
    "effectiveMagicAttack": 15,
    "effectiveMagicResist": 0,
    "effectiveSpeed": 100,
    "effectiveDodge": 0
  }
}
```

---

### 2. `POST /api/command` — 执行游戏命令

**请求体**:

```json
{
  "command": "go east"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| command | string | 是 | 游戏命令字符串 |

**响应示例**（成功）:

```json
{
  "status": "success",
  "message": "你向东移动到了 NORMAL_MONSTER_2",
  "data": { "... 房间数据同上 ..." }
}
```

**响应示例**（错误）:

```json
{
  "status": "error",
  "message": "There is no door to the east!",
  "data": null
}
```

#### 支持的命令

| 命令 | 语法 | 说明 |
|------|------|------|
| go | `go <direction>` | 向指定方向移动（east / west / south / north） |
| help | `help` | 显示所有可用命令 |
| quit | `quit` | 退出游戏（标记游戏结束） |
| take / pickup | `take <itemName>` | 从当前房间拾取物品放入背包 |
| drop | `drop <itemName>` | 从背包丢弃物品到当前房间 |
| items | `items` | 查看当前房间和背包中的物品信息 |
| attack | `attack <monsterName>` | 攻击指定怪物（旧版命令行，新用 POST /api/attack） |
| interact | `interact <target>` | 与场景交互（见下方交互命令表） |
| shop | `shop buy <itemName>` / `shop sell <itemName>` | 商店购买/出售物品 |
| bag | `bag use <itemName>` / `bag discard <itemName>` / `bag unequip <itemName>` | 背包操作 |
| wave | `wave <monsterName>` | 月光波技能（消耗30MP，单体魔法攻击） |
| test | `test poison` / `test burn` / `test bleed` | 调试命令：施加一层状态效果 |

##### 交互命令 (interact) 详情

| 语法 | 可用场景 | 说明 |
|------|----------|------|
| `interact heal` | CAMPFIRE | 治愈祭坛：回复50% HP/MP |
| `interact train` | CAMPFIRE | 训练祭坛：+25% 攻击/魔攻/防御，+10魔抗 |
| `interact wisdom` | CAMPFIRE | 博学祭坛：展示3个随机恩赐选项 |
| `interact wisdom <boonName>` | CAMPFIRE | 选择指定的恩赐（如 `interact wisdom ENDURANCE`） |
| `interact event` | ENCOUNTER(-BLACKSMITH) | 触发奇遇事件/与铁匠对话 |
| `interact <itemName>` | ENCOUNTER(BLACKSMITH) | 选择铁匠要强化的装备（150%属性提升） |

##### 背包命令 (bag) 详情

| 语法 | 说明 |
|------|------|
| `bag use 生命浆果` | 使用消耗品（回复25HP） |
| `bag use 魔力浆果` | 使用消耗品（回复25MP） |
| `bag use 饼干` | 使用消耗品（+10 maxHP） |
| `bag use 药水` | 使用消耗品（+20HP +30MP） |
| `bag use 铁剑` | 装备武器（+15 atk） |
| `bag use 铁盾` | 装备护甲（+10 def） |
| `bag use 暗影披风` | 装备披风（+15 dodge, +20 speed） |
| `bag use 生命戒指` | 装备戒指（+50 maxHp, 每2秒回血） |
| `bag use 元素项链` | 装备项链（+15 magicAtk, +20 magicResist） |
| `bag unequip <itemName>` | 卸下已装备的饰品/武器/护甲 |
| `bag discard <itemName>` | 从背包中丢弃指定物品（全部数量） |

---

### 3. `POST /api/reset` — 重置游戏

无参数。重置后游戏将使用新的随机种子生成全新的地图。

**响应示例**: 同 `GET /api/game` 的响应格式。

重置后：
- 玩家位于新地图的起始房间（START_HALL）
- 背包清空（初始无物品）
- 玩家属性恢复初始值（HP: 100, MP: 100, 攻击: 20, 防御: 10, 魔攻: 15, 魔抗: 0, 速度: 100, 闪避: 0, 金钱: 50）
- 所有房间怪物重新生成

---

### 4. `POST /api/attack` — 执行玩家攻击

后端统一做空间命中判定，前端传入攻击类型、玩家坐标/朝向、怪物坐标快照。

**请求体**:

```json
{
  "attackType": "sweep",
  "playerX": 400.0,
  "playerY": 320.0,
  "facingAngle": 1.57,
  "startX": 400.0,
  "startY": 320.0,
  "endX": 520.0,
  "endY": 320.0,
  "monsters": [
    { "name": "goblin#1234", "x": 300.0, "y": 160.0 },
    { "name": "slime#5678", "x": 350.0, "y": 200.0 }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| attackType | string | 是 | `sweep`（扇形扫击）、`pierce`（直线突刺）或 `charged`（蓄力360°范围） |
| playerX | number | 是 | 玩家当前 X 坐标（画布坐标） |
| playerY | number | 是 | 玩家当前 Y 坐标（画布坐标） |
| facingAngle | number | 是 | 玩家面朝角度（弧度，与 Phaser facingAngle 一致） |
| startX | number | 否 | 突刺起点 X（仅 pierce 使用，与 playerX 等同可省略） |
| startY | number | 否 | 突刺起点 Y（仅 pierce 使用，与 playerY 等同可省略） |
| endX | number | 否 | 突刺终点 X（仅 pierce 使用，建议提供） |
| endY | number | 否 | 突刺终点 Y（仅 pierce 使用，建议提供） |
| monsters | array | 是 | 当前房间存活怪物的名称与坐标快照列表 |

**monsters[].MonsterPosition 字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| name | string | 怪物名称（与后端一致） |
| x | number | 怪物 X 坐标 |
| y | number | 怪物 Y 坐标 |

**攻击判定参数**:

| 攻击类型 | 参数 | 说明 |
|----------|------|------|
| `sweep`（扇形扫击） | 半径 120px，角度 135° | 以玩家朝向为中心的扇形范围 |
| `pierce`（直线突刺） | 判定宽度 21px（半径） | 从起点到终点的线段距离判定 |
| `charged`（蓄力攻击） | 半径 150px，360° | 全方向范围攻击 |

**响应示例**（命中）:

```json
{
  "status": "success",
  "message": "你对 goblin#1234 造成了 15 点伤害。\n你击败了 goblin#1234！\n获得了 $15 货币！(余额: $65)",
  "data": {
    "name": "NORMAL_MONSTER_2",
    "monsters": [],
    "hitCount": 1,
    "playerHp": 100,
    "playerMaxHp": 100,
    "playerMp": 100,
    "playerMaxMp": 100,
    "playerMoney": 65,
    "activeEffects": [],
    "...": "其他房间字段同上"
  }
}
```

**响应示例**（落空）:

```json
{
  "status": "success",
  "message": "攻击落空了，没有命中任何怪物。",
  "data": {
    "hitCount": 0,
    "...": "其他房间字段同上"
  }
}
```

**命中判定后附加的 data 字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `hitCount` | number | 命中怪物数量 |
| `gameOver` | boolean | 玩家是否因流血等效果死亡 |
| `burnDamage` | number | （可选）烧伤结算伤害 |
| `burnLayers` | number | （可选）当前烧伤层数 |
| `poisonDamage` | number | （可选）中毒结算伤害 |
| `poisonLayers` | number | （可选）当前中毒层数 |
| `bleedLayers` | number | （可选）当前流血层数 |
| `angelBuffRemainingMs` | number | （可选）天使buff剩余毫秒数 |
| `ringRegen` | number | （可选）生命戒指被动回血量 |

---

### 5. `POST /api/save` — 保存游戏

保存当前游戏状态到数据库存档槽位。若未指定 saveId 则自动分配新 ID。

**请求体**:

```json
{
  "saveId": 1
}
```

或（新建存档）:

```json
{}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| saveId | number | 否 | 存档 ID（不传或 null 表示新建） |

**响应示例**:

```json
{
  "status": "success",
  "message": "游戏已保存 (ID: 1)",
  "data": {
    "id": 1,
    "playerName": "冒险者",
    "hp": 100,
    "maxHp": 100,
    "mp": 100,
    "maxMp": 100,
    "money": 50,
    "currentRoom": "START_HALL_3",
    "updatedAt": "2026-06-20T21:30:00.000+08:00"
  }
}
```

---

### 6. `POST /api/load` — 加载存档

从数据库加载指定 ID 的存档，替换当前游戏状态。

**请求体**:

```json
{
  "saveId": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| saveId | number | 是 | 要加载的存档 ID |

**响应示例**:

```json
{
  "status": "success",
  "data": {
    "name": "CAMPFIRE_7",
    "description": "...",
    "playerHp": 85,
    "playerMaxHp": 120,
    "playerMp": 60,
    "playerMaxMp": 110,
    "playerMoney": 230,
    "backpack": [...],
    "...": "其他房间字段同上"
  }
}
```

---

### 7. `GET /api/saves` — 获取存档列表

获取所有存档的摘要信息。

**响应示例**:

```json
[
  {
    "id": 1,
    "playerName": "冒险者",
    "hp": 100,
    "maxHp": 100,
    "mp": 100,
    "maxMp": 100,
    "money": 50,
    "currentRoom": "START_HALL_3",
    "updatedAt": "2026-06-20T21:30:00.000+08:00"
  },
  {
    "id": 2,
    "playerName": "冒险者",
    "hp": 75,
    "maxHp": 120,
    "money": 185,
    "currentRoom": "SHOP_5",
    "updatedAt": "2026-06-20T21:35:00.000+08:00"
  }
]
```

---

### 8. `POST /api/deleteSave` — 删除存档

删除指定 ID 的存档。

**请求体**:

```json
{
  "saveId": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| saveId | number | 是 | 要删除的存档 ID |

**响应示例**:

```json
{
  "status": "success",
  "message": "存档已删除"
}
```

---

### 9. `GET /api/map` — 获取全地图数据

获取整个游戏地图的拓扑结构，供前端小地图渲染使用。

**响应示例**:

```json
{
  "status": "success",
  "data": {
    "rooms": [
      {
        "name": "START_HALL_1",
        "exits": { "east": "NORMAL_MONSTER_2", "south": "SHOP_3" },
        "roomType": "START_HALL"
      },
      {
        "name": "NORMAL_MONSTER_2",
        "exits": { "west": "START_HALL_1", "east": "CAMPFIRE_4", "south": "ENCOUNTER_5" },
        "roomType": "NORMAL_MONSTER"
      }
    ],
    "startRoomName": "START_HALL_1"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| rooms | array | 所有房间的列表 |
| rooms[].name | string | 房间名称 |
| rooms[].exits | object | 出口映射，键为方向，值为邻居房间名称 |
| rooms[].roomType | string | 房间类型 |
| startRoomName | string | 起始房间名称 |

---

### 10. `GET /api/backpack` — 获取背包数据

获取玩家背包中的完整物品列表。

> 注意：此接口目前返回与 `GET /api/game` 相同的完整游戏状态响应（含 backpack 字段），后续可能独立。

**响应示例**:

```json
{
  "status": "success",
  "data": {
    "name": "CAMPFIRE_7",
    "backpack": [
      { "itemId": 0, "name": "生命浆果", "rarity": "common", "quantity": 5, "functionDesc": "红彤彤的浆果，使用后可回复25点生命值。", "loreDesc": "红彤彤的浆果，使用后可回复25点生命值。", "price": 25, "equipped": false },
      { "itemId": 1, "name": "魔力浆果", "rarity": "common", "quantity": 3, "functionDesc": "散发着魔力波动的浆果，使用后可回复25点魔力值。", "loreDesc": "散发着魔力波动的浆果，使用后可回复25点魔力值。", "price": 25, "equipped": false },
      { "itemId": 2, "name": "铁剑", "rarity": "common", "quantity": 1, "functionDesc": "一把锋利的铁剑，装备后物理攻击力+15。", "loreDesc": "一把锋利的铁剑，装备后物理攻击力+15。", "price": 80, "equipped": true },
      { "itemId": 3, "name": "铁盾", "rarity": "common", "quantity": 1, "functionDesc": "坚固的铁盾，装备后物理防御力+10。", "loreDesc": "坚固的铁盾，装备后物理防御力+10。", "price": 70, "equipped": false },
      { "itemId": 4, "name": "暗影披风", "rarity": "rare", "quantity": 1, "functionDesc": "由暗影魔龙的鳞片编织而成，佩戴后闪避率+15%，移动速度+20。", "loreDesc": "由暗影魔龙的鳞片编织而成，佩戴后闪避率+15%，移动速度+20。", "price": 200, "equipped": false },
      { "itemId": 5, "name": "生命戒指", "rarity": "rare", "quantity": 1, "functionDesc": "精灵族工匠以生命之树的枝干打造，佩戴后最大生命值+50，每2秒恢复1点生命。", "loreDesc": "精灵族工匠以生命之树的枝干打造，佩戴后最大生命值+50，每2秒恢复1点生命。", "price": 150, "equipped": false },
      { "itemId": 6, "name": "元素项链", "rarity": "epic", "quantity": 1, "functionDesc": "蕴含着四种元素之力的古老吊坠，佩戴后魔法攻击力+15，魔法抗性+20%。", "loreDesc": "蕴含着四种元素之力的古老吊坠，佩戴后魔法攻击力+15，魔法抗性+20%。", "price": 150, "equipped": false },
      { "itemId": 7, "name": "药水", "rarity": "common", "quantity": 2, "functionDesc": "装在玻璃瓶中的红色液体，使用后立即回复30点魔力与20点生命。", "loreDesc": "装在玻璃瓶中的红色液体，使用后立即回复30点魔力与20点生命。", "price": 35, "equipped": false }
    ],
    "...": "其他房间字段"
  }
}
```

---

## 快速测试 (cURL)

```bash
# 查看游戏状态
curl http://localhost:8080/api/game

# 查看帮助
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"help\"}"

# 移动
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"go east\"}"

# 拾取物品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"take 生命浆果\"}"

# 丢弃物品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"drop 铁剑\"}"

# 查看物品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"items\"}"

# 使用消耗品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"bag use 生命浆果\"}"

# 装备物品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"bag use 铁剑\"}"

# 卸下装备
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"bag unequip 铁剑\"}"

# 丢弃物品（背包）
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"bag discard 药水\"}"

# 攻击（扇形扫击）
curl -X POST http://localhost:8080/api/attack -H "Content-Type: application/json" -d "{\"attackType\":\"sweep\",\"playerX\":400,\"playerY\":320,\"facingAngle\":0,\"monsters\":[{\"name\":\"goblin#1234\",\"x\":300,\"y\":160}]}"

# 攻击（直线突刺）
curl -X POST http://localhost:8080/api/attack -H "Content-Type: application/json" -d "{\"attackType\":\"pierce\",\"playerX\":400,\"playerY\":320,\"facingAngle\":0,\"startX\":400,\"startY\":320,\"endX\":520,\"endY\":320,\"monsters\":[{\"name\":\"goblin#1234\",\"x\":500,\"y\":320}]}"

# 攻击（蓄力360°）
curl -X POST http://localhost:8080/api/attack -H "Content-Type: application/json" -d "{\"attackType\":\"charged\",\"playerX\":400,\"playerY\":320,\"facingAngle\":0,\"monsters\":[{\"name\":\"goblin#1234\",\"x\":350,\"y\":280}]}"

# 月光波技能
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"wave goblin#1234\"}"

# 与治愈祭坛交互
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"interact heal\"}"

# 博学祭坛——查看选项
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"interact wisdom\"}"

# 博学祭坛——选择恩赐
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"interact wisdom ENDURANCE\"}"

# 商店购买
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"shop buy 铁剑\"}"

# 商店出售
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"shop sell 生命浆果\"}"

# 测试状态效果
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"test poison\"}"

# 重置游戏
curl -X POST http://localhost:8080/api/reset

# 获取地图数据
curl http://localhost:8080/api/map

# 获取背包数据
curl http://localhost:8080/api/backpack

# 保存游戏（新建）
curl -X POST http://localhost:8080/api/save -H "Content-Type: application/json" -d "{}"

# 保存游戏（指定槽位）
curl -X POST http://localhost:8080/api/save -H "Content-Type: application/json" -d "{\"saveId\":1}"

# 获取存档列表
curl http://localhost:8080/api/saves

# 加载存档
curl -X POST http://localhost:8080/api/load -H "Content-Type: application/json" -d "{\"saveId\":1}"

# 删除存档
curl -X POST http://localhost:8080/api/deleteSave -H "Content-Type: application/json" -d "{\"saveId\":1}"

# 退出游戏
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"quit\"}"
```

---

## 游戏地图

本项目采用程序化随机地图生成，每次游戏都会生成不同的地图布局。

地图生成参数：
- **网格尺寸**: 10（宽）× 15（高）个房间
- **房间总数**: 约 30-50 个（视随机生成结果）
- **房间类型**: 7 种（START_HALL / SHOP / ENCOUNTER / CAMPFIRE / BOSS / ELITE_MONSTER / NORMAL_MONSTER）
- **出口方向**: east（东）、west（西）、south（南）、north（北）
- **传送房间**: 每局随机标记 1 个传送房间

### 房间类型说明

| 类型 | 说明 |
|------|------|
| START_HALL | 起始大厅，玩家初始位置 |
| SHOP | 商店房间，可与商人交易（购买/出售） |
| ENCOUNTER | 奇遇房间，触发随机事件（宝箱/喷泉/精英敌人/铁匠/天使等） |
| CAMPFIRE | 篝火房间，包含治愈/训练/博学三种祭坛 |
| BOSS | Boss 战房间 |
| ELITE_MONSTER | 精英怪物房间 |
| NORMAL_MONSTER | 普通怪物房间，击败后解锁出口 |

### 地图数据接口

使用 `GET /api/map` 获取完整的地图拓扑信息，前端根据房间名称进行布局计算。
