# Zuul Game API 文档

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

---

## 接口列表

### 1. GET /api/game — 获取当前游戏状态

无参数。

**响应示例（success）**:

```json
{
  "status": "success",
  "message": "Current game status",
  "data": {
    "name": "Outside Building",
    "description": "Outside the main building of the university",
    "exits": "east",
    "items": [
      {
        "name": "rock",
        "description": "A heavy rock",
        "weight": 2.5
      }
    ],
    "isTeleportRoom": false
  }
}
```

---

### 2. POST /api/command — 执行游戏命令

**请求体**:

```json
{
  "command": "go east"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| command | string | 是 | 游戏命令字符串，如 "go east"、"help" 等 |

**响应示例（success）**:

```json
{
  "status": "success",
  "message": "You move to the Lobby",
  "data": {
    "name": "Lobby",
    "description": "The lobby of the building",
    "exits": "south north west",
    "items": [],
    "isTeleportRoom": false
  }
}
```

**响应示例（error）**:

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
| back | `back` | 返回上一个房间 |
| help | `help` | 显示所有可用命令 |
| quit | `quit` | 退出游戏（标记游戏结束） |
| take | `take <itemName>` | 从当前房间拾取物品放入背包（受负重限制） |
| drop | `drop <itemName>` | 从背包丢弃物品到当前房间 |
| items | `items` | 查看当前房间物品、背包物品及负重信息 |
| eat | `eat cookie` | 吃掉魔法饼干，增加最大负重 5.0 kg |

**命令详解**

**go <direction>**

- direction 可选值：`east`、`west`、`south`、`north`
- 如果目标房间是 Lab（实验室），会触发随机传送

**take <itemName>**

- 物品重量 + 背包当前重量 ≤ 最大负重时才能拾取
- 初始最大负重：`10.0 kg`

**items**

```
Room items:
  - pen (A metal pen, weight: 0.2)

Inventory (0.0/10.0 kg):
  (empty)
```

**eat cookie**

- 需要背包中有名为 `cookie` 的物品
- 吃掉后 cookie 被消耗，最大负重增加 5.0 kg

**quit**

```json
{
  "status": "success",
  "message": "Game over! You quit the game.",
  "data": null
}
```

> **注意**: 游戏结束后需要先重置才能继续操作，quit 后所有命令（除 help）均会返回错误。

---

### 3. POST /api/reset — 重置游戏

无参数。

**响应示例**

```json
{
  "status": "success",
  "message": "Game reset successfully",
  "data": {
    "name": "Outside Building",
    "description": "Outside the main building of the university",
    "exits": "east",
    "items": [
      {
        "name": "rock",
        "description": "A heavy rock",
        "weight": 2.5
      }
    ],
    "isTeleportRoom": false
  }
}
```

重置后游戏回到初始状态：

- 玩家位于 Outside Building
- 背包清空，负重恢复为 10.0 kg
- 所有物品回归原位

---

### 4. POST /api/attack — 执行玩家攻击（扇形扫击 / 直线突刺）

后端统一做空间命中判定，前端只需传入攻击类型、玩家坐标/朝向、怪物坐标快照。

**请求体**:

```json
{
  "attackType": "sweep | pierce",
  "playerX": 400.0,
  "playerY": 320.0,
  "facingAngle": 1.57,
  "monsters": [
    { "name": "goblin#1234", "x": 300.0, "y": 160.0 },
    { "name": "slime#5678", "x": 350.0, "y": 200.0 }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| attackType | string | 是 | 攻击类型：`sweep`（扇形扫击）或 `pierce`（直线突刺） |
| playerX | number | 是 | 玩家当前 X 坐标（画布坐标） |
| playerY | number | 是 | 玩家当前 Y 坐标（画布坐标） |
| facingAngle | number | 是 | 玩家面朝角度（弧度，与 Phaser facingAngle 一致） |
| monsters | array | 是 | 当前房间存活怪物的名称与坐标快照列表 |
| monsters[].name | string | 是 | 怪物名称（与后端一致） |
| monsters[].x | number | 是 | 怪物 X 坐标 |
| monsters[].y | number | 是 | 怪物 Y 坐标 |

**攻击判定参数（后端硬编码，与前端配置一致）**:

- 扇形扫击：半径 `120px`，角度 `135°`
- 直线突刺：距离 `120px`，宽度 `12 + 30px`

**响应示例（success）**:

```json
{
  "status": "success",
  "message": "你对 goblin#1234 造成了 20 点伤害。\n你击败了 goblin#1234！\n获得了 $15 货币！(余额: $65)",
  "data": {
    "name": "Room1",
    "description": "...",
    "exits": "east west",
    "monsters": [],
    "hitCount": 1,
    "playerHp": 100,
    "playerMaxHp": 100,
    "playerMp": 100,
    "playerMaxMp": 100,
    "playerMoney": 65,
    "activeEffects": []
  }
}
```

**响应示例（落空）**:

```json
{
  "status": "success",
  "message": "攻击落空了，没有命中任何怪物。",
  "data": {
    "hitCount": 0,
    ...
  }
}
```

---

## 快速测试 (curl)

```bash
# 查看游戏状态
curl http://localhost:8080/api/game

# 查看帮助
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"help\"}"

# 移动
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"go east\"}"

# 攻击（扇形扫击）
curl -X POST http://localhost:8080/api/attack -H "Content-Type: application/json" -d "{\"attackType\":\"sweep\",\"playerX\":400,\"playerY\":320,\"facingAngle\":0,\"monsters\":[{\"name\":\"goblin#1234\",\"x\":300,\"y\":160}]}"

# 攻击（直线突刺）
curl -X POST http://localhost:8080/api/attack -H "Content-Type: application/json" -d "{\"attackType\":\"pierce\",\"playerX\":400,\"playerY\":320,\"facingAngle\":0,\"monsters\":[{\"name\":\"goblin#1234\",\"x\":500,\"y\":320}]}"

# 查看物品和背包
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"items\"}"

# 拾取物品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"take rock\"}"

# 丢弃物品
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"drop rock\"}"

# 吃魔法饼干
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"eat cookie\"}"

# 重置游戏
curl -X POST http://localhost:8080/api/reset

# 退出游戏
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"quit\"}"
```

---

## 游戏地图

```
                    [Lab]
                      ^
                      |
    [Outside] <--> [Lobby] <--> [Office]
                      |
                      v
                  [Library]
```

- 初始位置：Outside Building
- Lab（实验室）是 **传送房间**，进入后会随机传送到其他房间
- Library（图书馆）内有 **魔法饼干** 可供探索

