# 数据库表设计

## 概述

当前游戏使用纯内存存储，重启即丢失所有数据。需要数据库持久化。分析现有模型后，设计以下表结构。

## 现有模型 -> 数据库映射

| 模型类 | 数据库表 | 说明 |
|--------|---------|------|
| `Player` | `game_save` (1行) | 玩家完整状态，一存档一行 |
| `Bag` → `List<Item>` | `bag_item` | 背包物品，一行一件物品 |
| `Money` | `game_save.money` | 金币，作为玩家表字段 |
| `Room` (状态) | `room_state` | 记录哪些房间已清空/祭坛已使用 |
| `Monster` | 不复用存储 | 怪物每次进房间时重新生成，只记HP |
| `ShopItem` | `shop_state` | 记录哪些商店物品已售出 |
| `DroppedItem` | 不复用存储 | 临时数据，不持久化 |

## 建表 SQL

### 1. game_save（核心存档表）

玩家所有属性打包存一行，`player_data` 用 JSON 存储复杂结构。

```sql
CREATE TABLE game_save (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(100)   NOT NULL,
    hp          INT            NOT NULL DEFAULT 100,
    max_hp      INT            NOT NULL DEFAULT 100,
    mp          INT            NOT NULL DEFAULT 100,
    max_mp      INT            NOT NULL DEFAULT 100,
    attack      INT            NOT NULL DEFAULT 20,
    magic_attack INT           NOT NULL DEFAULT 15,
    defense     INT            NOT NULL DEFAULT 10,
    magic_resist INT           NOT NULL DEFAULT 0,
    speed       INT            NOT NULL DEFAULT 100,
    dodge       INT            NOT NULL DEFAULT 0,
    money       INT            NOT NULL DEFAULT 50,
    current_room VARCHAR(100)  NOT NULL DEFAULT '房间1',  -- 玩家所在房间名
    -- 饰品/装备槽位
    equipped_cloak  VARCHAR(100),
    equipped_ring   VARCHAR(100),
    equipped_amulet VARCHAR(100),
    equipped_weapon VARCHAR(100),
    equipped_armor  VARCHAR(100),
    -- 后端状态（JSON）
    status_data   TEXT,       -- 烧伤/中毒/流血层数和计时 JSON
    room_data     TEXT,       -- 所有房间状态 JSON（供备选）
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. bag_item（背包物品表）

背包中每件物品一行。简单物品可以直接用 varchar 存名称，复杂物品存 JSON。

```sql
CREATE TABLE bag_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    save_id     BIGINT         NOT NULL,            -- 关联 game_save.id
    item_name   VARCHAR(100)   NOT NULL,            -- 物品名称（如"生命浆果"）
    description VARCHAR(255),                        -- 物品描述（可选）
    price       INT            NOT NULL DEFAULT 0,   -- 物品价格
    rarity      VARCHAR(50),                         -- 稀有度
    -- 从 ItemRegistry 可以自动推断 rarity/desc，但持久化避免重新查询
    FOREIGN KEY (save_id) REFERENCES game_save(id) ON DELETE CASCADE
);
```

> **设计思路**：背包物品只需存 `item_name`，其他字段（rarity/functionDesc/loreDesc）在 `Item.inferMetadata()` 中自动推断，无需冗余存储。但如果想存完整信息，也加了字段。

### 3. room_state（房间状态表）

记录房间的持久化状态——哪些怪物已清除、祭坛是否已用、商店商品是否已售。

```sql
CREATE TABLE room_state (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    save_id         BIGINT        NOT NULL,
    room_name       VARCHAR(100)  NOT NULL,
    monsters_cleared BOOLEAN      NOT NULL DEFAULT FALSE,
    altar_used      BOOLEAN      NOT NULL DEFAULT FALSE,
    shop_initialized BOOLEAN     NOT NULL DEFAULT FALSE,
    FOREIGN KEY (save_id) REFERENCES game_save(id) ON DELETE CASCADE,
    UNIQUE (save_id, room_name)  -- 每个存档每个房间一行
);
```

### 4. shop_state（商店售出状态）

记录哪些商店物品已被购买。

```sql
CREATE TABLE shop_state (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    save_id     BIGINT        NOT NULL,
    room_name   VARCHAR(100)  NOT NULL,
    item_name   VARCHAR(100)  NOT NULL,    -- 售出的物品名
    FOREIGN KEY (save_id) REFERENCES game_save(id) ON DELETE CASCADE
);
```

### 5. 可选：monster_state（怪物血量）

如果需要在退出/重进时保留怪物血量：

```sql
CREATE TABLE monster_state (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    save_id     BIGINT        NOT NULL,
    room_name   VARCHAR(100)  NOT NULL,
    monster_name VARCHAR(200) NOT NULL,
    hp          INT           NOT NULL,
    max_hp      INT           NOT NULL DEFAULT 0,  -- 构造在 Monster 内
    FOREIGN KEY (save_id) REFERENCES game_save(id) ON DELETE CASCADE
);
```

> **注意**：怪物在每次进房间时由 `Room.spawnMonsters()` 重新生成，血量是随机的。如果不需要精确恢复怪物血量，此表可以省略。

## 最小化表方案

如果追求最简实现，可以只建 **2 张表**：

| 表 | 说明 |
|----|------|
| `game_save` | 玩家全部状态（含 JSON 字段存背包和房间状态） |
| `bag_item` | 背包物品（也可以 JSON 放在 game_save 中） |

```sql
-- 极简方案：一张表搞定
CREATE TABLE game_save (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_json   TEXT NOT NULL,         -- 整个 Player 对象 JSON
    rooms_json    TEXT NOT NULL,         -- 所有 Room 状态的 JSON
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**优点**：1个表，2个 JSON 字段，所有数据一次存取  
**缺点**：不能做 SQL 查询（如"查看背包有多少物品"），但游戏存档不需要复杂查询

## 推荐方案

| 需求 | 推荐表数 | 涉及表 |
|------|---------|--------|
| 存档/读档 | 2张 | `game_save` + `bag_item` |
| 完整持久化 | 5张 | 上面全部 |
| 极简实现 | 1张 | `game_save` (JSON) |

**推荐从 2 表方案开始**（`game_save` + `bag_item`），满足核心需求且不复杂。
