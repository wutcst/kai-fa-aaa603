# 数据库持久化实现文档（完整版）

## 架构概览

5 张 H2 表 + 种子确定性地图生成

| 表 | Entity | 存储内容 |
|---|--------|---------|
| `game_save` | GameSaveEntity | 玩家全属性、位置、装备槽位、地图种子、状态效果JSON |
| `bag_item` | BagItemEntity | 背包中每件物品名称 |
| `room_state` | RoomStateEntity | 怪物清除/祭坛使用/随机事件使用 |
| `shop_state` | ShopStateEntity | 商店已售出物品 |
| `monster_state` | MonsterStateEntity | 存活怪物的当前血量/最大血量/类型 |

## 需要新建的文件（11个）

### Entity（5个，放在 backend/src/main/java/cn/edu/whut/sept/zuul/model/）

1. **GameSaveEntity.java** — 主存档表
   - @Entity @Table("game_save")
   - **@Id private Long id（无 @GeneratedValue，手动指定 1/2/3）**
   - 字段：playerName, hp, maxHp, mp, maxMp, attack, magicAttack, defense, magicResist, speed, dodge, money, currentRoom, mapSeed, equippedCloak/Ring/Amulet/Weapon/Armor, statusJson(TEXT), createdAt, updatedAt
   - @PrePersist/@PreUpdate 自动时间戳

2. **BagItemEntity.java** — 背包物品表
   - @Entity @Table("bag_item")
   - 字段：id(自增), saveId, itemName

3. **RoomStateEntity.java** — 房间状态表
   - @Entity @Table("room_state", uniqueConstraints={@UniqueConstraint(columnNames={"save_id","room_name"})})
   - 字段：id(自增), saveId, roomName, monstersCleared, altarUsed, randomEventUsed

4. **ShopStateEntity.java** — 商店售出表
   - @Entity @Table("shop_state")
   - 字段：id(自增), saveId, roomName, itemName

5. **MonsterStateEntity.java** — 怪物血量表
   - @Entity @Table("monster_state", uniqueConstraints={@UniqueConstraint(columnNames={"save_id","room_name","monster_name"})})
   - 字段：id(自增), saveId, roomName, monsterName, hp, maxHp, monsterType

### Repository（5个，放在 backend/src/main/java/cn/edu/whut/sept/zuul/repository/）

6. **GameSaveRepository.java** — extends JpaRepository<GameSaveEntity, Long>
7. **BagItemRepository.java** — + findBySaveId + @Transactional deleteBySaveId
8. **RoomStateRepository.java** — + findBySaveId + @Transactional deleteBySaveId
9. **ShopStateRepository.java** — + findBySaveId + @Transactional deleteBySaveId
10. **MonsterStateRepository.java** — + findBySaveId + @Transactional deleteBySaveId

### Service（1个）

11. **SaveService.java** — @Service，@Autowired 5个Repository
   - @Transactional saveGame(Game, saveId): 覆盖保存逻辑
   - @Transactional loadGame(saveId): 重建Game
   - listSaves(): findAll
   - @Transactional deleteSave(saveId): 级联删除

## 需要修改的文件（6个）

12. **backend/pom.xml** — 添加：
    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    ```

13. **backend/src/main/resources/application.properties**（注意路径：src/main/resources/）：
    ```properties
    spring.datasource.url=jdbc:h2:file:./data/zuul_save;DB_CLOSE_DELAY=-1
    spring.datasource.driver-class-name=org.h2.Driver
    spring.datasource.username=sa
    spring.datasource.password=
    spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=false
    spring.h2.console.enabled=true
    ```

14. **Game.java** — 添加 `private long mapSeed;`
    - 无参构造 → `this(new Random().nextLong())`
    - 带参构造 → `public Game(long seed) { this.mapSeed=seed; createRooms(); ... }`
    - `createRooms()` → `GenerateRoom.generate(10, 15, mapSeed)`

15. **GenerateRoom.java** — `generate(int, int)` → `generate(int, int, long seed)`
    - `new Random()` → `new Random(seed)`

16. **Room.java** — `spawnMonsters()`：
    - `new Random()` → `new Random(this.name.hashCode())`
    - 保证同房间名生成相同怪物（存档恢复关键）

17. **Status.java** — `StatusManager` 类添加 `@Setter`（Lombok）
    - 从 `@Getter` 改为 `@Getter @Setter`

## 前端修改（1个）

18. **frontend/src/App.vue** — 完整重写
    - 添加 3 槽位保存/读档弹窗
    - 每个槽位卡片显示：玩家名、HP、位置、金钱、时间
    - 有数据槽位显示"删除"按钮
    - `showSaveSlots()` / `showLoadSlots()` / `deleteSlot()`
    - `@load-game` 事件改为 `showLoadSlots`
    - "保存游戏"按钮改为 `showSaveSlots`

## 关键实现细节

### 覆盖保存（SaveService.saveGame）
```
if existsById(saveId):
    deleteAll bag_item where saveId
    deleteAll room_state where saveId
    deleteAll shop_state where saveId
    deleteAll monster_state where saveId
    deleteById(saveId)
    flush()

new GameSaveEntity() → setId(saveId) → set各项属性 → save → flush
遍历背包物品 → 逐个save BagItemEntity
遍历房间状态 → 逐个save RoomStateEntity
遍历存活怪物 → 逐个save MonsterStateEntity
遍历商店售出 → 逐个save ShopStateEntity
```

### 读档（SaveService.loadGame）
```
entity = findById(saveId)
game = new Game(entity.mapSeed)          // 种子重建地图
恢复玩家属性（直接用setter设置）
money.setAmount(entity.money)            // 不用reset+add
恢复背包（clear + addItem逐个）
恢复装备槽位（直接set，不调equipItem避免双重加成）
从背包移除已装备物品
恢复状态效果（deserializeStatus JSON）
恢复怪物血量（spawnMonsters + 按名称匹配 setHp/setMaxHp）
恢复房间状态（setMonstersCleared/setAltarUsed/initRandomEvent+setUsed）
恢复商店售出（setSold true）
定位到存档房间（setCurrentRoom）
```

### 金币恢复
```java
player.getMoney().setAmount(entity.getMoney()); // 直接设值
// 不能用 reset() 再 add()，reset 先设 50 再加 = 翻倍
```

### 随机事件恢复
```java
room.initRandomEvent(); // 先确保事件已初始化
if (room.getRandomEvent() != null) {
    room.getRandomEvent().setUsed(true);
}
```
