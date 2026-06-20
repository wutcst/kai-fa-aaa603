# 数据库集成可行性分析

## 当前架构

| 层级 | 技术栈 | 状态 |
|------|--------|------|
| 框架 | Spring Boot 3.2.0 | ✅ 已就绪 |
| 依赖 | `spring-boot-starter-web` + Lombok | ⚠️ 需加 JPA/JDBC |
| 服务层 | `@Service GameService` 单例 | ✅ 结构清晰 |
| 控制器 | `@RestController GameController` | ✅ REST API 标准 |
| 数据模型 | 纯 POJO (Player/Monster/Room/Item) | ✅ 适合加 `@Entity` |
| 数据库 | **无（纯内存）** | ❌ 重启丢失 |

## 需要添加的依赖

```xml
<!-- pom.xml 新增 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>  <!-- 开发用内存数据库 -->
</dependency>
<!-- 或 MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

## 需要持久化的数据

| 数据 | 优先级 | 当前模型 | 数据库对应 |
|------|--------|---------|-----------|
| **玩家状态** | 🔴 高 | `Player` (HP/MP/属性/饰品) | `player` 表 |
| **背包物品** | 🔴 高 | `Bag` → `List<Item>` | `bag_item` 关联表 |
| **金币** | 🔴 高 | `Money` 内嵌在 Player | `player.money` 字段 |
| **存档/读档** | 🔴 高 | 无 | 新增 `save` 表 |
| **怪物数据** | 🟡 中 | `Monster` (HP/位置等) | `room_monster` 表 |
| **房间状态** | 🟡 中 | `Room` (清空/祭坛使用) | `room_state` 表 |
| **掉落物** | 🟢 低 | `DroppedItem` (临时) | 通常不持久 |
| **商店状态** | 🟢 低 | `ShopItem` (售出标记) | `shop_state` 表 |

## 实现方案

### 方案A：轻量级（仅存档系统）

只持久化玩家存档，加 1个表 `game_save`：

```sql
CREATE TABLE game_save (
  id BIGINT PRIMARY KEY,
  player_name VARCHAR(100),
  player_data JSON,   -- 用 JSON 存完整玩家状态
  room_name VARCHAR(100),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

- **改动文件**：3-4个（pom.xml + application.properties + SaveRepository + GameService）
- **难度**：⭐ 低（1小时内完成）

### 方案B：完整ORM（JPA实体映射）

将 `Player`、`Item`、`Monster` 等模型改为 `@Entity`，用 Spring Data JPA 全自动管理：

```
模型层改动：
Player → @Entity + @Table("player")
  ├── @OneToMany Bag (背包)
  ├── @Embedded Money
  └── @ManyToOne Room

Room → @Entity  
  ├── @OneToMany Monster
  └── @OneToMany DroppedItem
```

- **改动文件**：12-15个（全部模型类 + pom.xml + 配置 + Repository接口）
- **难度**：⭐⭐⭐ 中 （3-5小时）
- **风险**：Room 有循环引用（`Map<String, Room> exits`），需要 `@JsonIgnore` 处理

### 方案C：MyBatis XML 映射

不改变现有 POJO，用 MyBatis XML 手动写 SQL 映射：

- **改动文件**：8-10个
- **难度**：⭐⭐ 中
- **优势**：不侵入现有模型类

## 关键挑战

| 挑战 | 说明 | 解决方案 |
|------|------|---------|
| **单例 Game 实例** | `GameService` 中 `private final Game game` | 每次请求从数据库加载/保存 |
| **Room 循环引用** | `Map<String, Room> exits` 在 JPA 中会无限递归 | `@JsonIgnore` 或用 ID 引用 |
| **前端轮询** | 每500ms 调 `GET /api/game` | 读操作不加数据库，只在关键时写 |
| **Lombok @Data** | JPA 实体推荐用 `@Getter/@Setter` 而非 `@Data` | 将 `@Data` 改为 `@Getter @Setter` |
| **无事务管理** | 当前代码无 `@Transactional` | 只在存档/读档时加事务 |

## 推荐实施路径

```
第1步：加依赖 + 配置 → pom.xml + application.properties (5分钟)
第2步：创建存档表 → 方案A的 game_save 表 (10分钟)  
第3步：在 GameService 加 save/load 方法 (30分钟)
第4步：加 @PostConstruct 自动加载存档 (15分钟)
第5步：测试 → 重启服务器后数据仍保留
```

## 结论

**容易实现。** Spring Boot 3.2.0 + 现有的 Model-Service-Controller 分层结构非常适合加数据库。最推荐从**方案A（JSON存档）**开始，只涉及3-4个文件修改，1小时内可以跑通。后续如果需要更复杂的查询（如排行榜、历史记录），再升级到方案B（JPA）。
