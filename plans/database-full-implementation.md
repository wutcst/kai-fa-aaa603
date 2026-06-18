# 完整数据库实现方案

## 游戏数据全景

| 数据 | 需持久化 | 对应表 |
|------|---------|--------|
| 玩家HP/MP/属性 | ✅ | player 表字段 |
| 金钱 | ✅ | player.money |
| 装备槽位(5个) | ✅ | player 表字段 |
| 背包物品 | ✅ | bag_item 表 |
| 烧伤/中毒/流血状态 | ✅ | player.status_json (JSON) |
| 当前房间 | ✅ | player.current_room |
| 房间怪物清除标记 | ✅ | room_state 表 |
| 祭坛已使用标记 | ✅ | room_state 表 |
| 商店已售出标记 | ✅ | shop_state 表 |
| 奇遇事件已使用 | ✅ | room_state 表 |
| 怪物HP/位置 | ❌ | 进房间重新生成 |
| 掉落物 | ❌ | 临时数据 |

## 需要建的 4 张表

1. **player** — 玩家全属性 + 装备槽位 + 状态JSON + 当前房间
2. **bag_item** — 背包中的物品（一对多关联）
3. **room_state** — 每个房间的状态标记（已清除/已用祭坛/已用奇遇）
4. **shop_state** — 商店已售出的物品列表

## 需要改/新建的文件

| 文件 | 改动 |
|------|------|
| `pom.xml` | +JPA+H2 依赖 |
| `application.properties` | +数据库配置 |
| 新建 `PlayerEntity.java` | JPA 实体 |
| 新建 `BagItemEntity.java` | JPA 实体 |
| 新建 `RoomStateEntity.java` | JPA 实体 |
| 新建 `ShopStateEntity.java` | JPA 实体 |
| 新建 `XxxRepository.java` | Spring Data JPA 接口 x4 |
| 新建 `SaveLoadService.java` | 存档读档逻辑 |
| `GameService.java` | 关键操作后调 save() |
| `Game.java` | 加 loadGame() 恢复方法 |

**共约 10 个文件，~350 行代码**

## 数据流

保存时: player表(1行) + bag_item表(N行) + room_state表(M行) + shop_state表(K行)
加载时: 读出所有行 → 还原内存中的 Game 对象 → 恢复游戏状态
