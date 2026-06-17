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

## 快速测试 (curl)

```bash
# 查看游戏状态
curl http://localhost:8080/api/game

# 查看帮助
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"help\"}"

# 移动
curl -X POST http://localhost:8080/api/command -H "Content-Type: application/json" -d "{\"command\":\"go east\"}"

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

