# 商店物品系统设计方案

## 一、问题分析

当前商店有两个问题已修复（闪退），还有一个核心问题未解决：商店中的商品是伪随机生成的，如"商品12"、"商品37"，不是游戏中真实存在的道具。

### 现有的相关类结构

| 类 | 职责 | 是否需修改 |
|---|---|---|
| [`Item.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Item.java) | 物品（name + description），在背包中实际持有 | ✅ 需要 |
| [`InventoryItem.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/InventoryItem.java) | 背包展示用（稀有度、描述、数量），`fromItem()` 已涵盖所有物品映射 | ❌ 不变 |
| [`ShopItem.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/ShopItem.java) | 商店商品（name + price + sold） | ✅ 需要 |
| [`Room.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java) | `initShopItems()` 生成"商品X" | ✅ 需要 |
| [`ShopCommand.java`](backend/src/main/java/cn/edu/whut/sept/zuul/command/ShopCommand.java) | 购买逻辑 | ⚠️ 微调 |

## 二、设计方案

### 方案概述：物品注册表 + 加权随机选取

```
┌─────────────────────┐
│    ItemRegistry      │  ← 静态物品注册表，存所有可购买物品
│  (static catalog)    │
├─────────────────────┤
│  - 生命浆果  $25     │
│  - 魔力浆果  $25     │
│  - 饼干       $40    │
│  - 药水       $35    │
│  - 铁剑       $80    │
│  - 铁盾       $70    │
│  - 暗影披风  $200    │  ← 传奇饰品，高价格
│  - 生命戒指  $150    │
│  - 元素项链  $150    │
│  - 魔法卷轴   $60    │
└─────────┬───────────┘
          │
          ▼
┌──────────────────────────┐
│  Room.initShopItems()     │  ← 从注册表中随机选6个
│  for each SHOP room       │
│    pick 6 random items    │
│    create ShopItem(item)  │
└─────────┬────────────────┘
          │
          ▼
┌──────────────────────────┐
│  ShopCommand.execute()    │  ← 购买时用Item创建实例
│  shopItem.getItem()  →   │
│  new Item(name, desc,    │
│        price) + addToBag │
└──────────────────────────┘
```

### 步骤详解

#### Step 1: 给 Item 类添加 price 属性

在 [`Item.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Item.java) 中新增 `price` 字段，提供全参构造器和修改现有构造器：

```java
public class Item {
    private String name;
    private String description;
    private int price;       // 新增：价格（0表示不可购买）

    public Item(String name) {
        this.name = name;
        this.description = "普通的" + name;
        this.price = 0;
    }

    // 新建全参构造器
    public Item(String name, String description, int price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
```

#### Step 2: 创建物品注册表 ItemRegistry

新建 [`ItemRegistry.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/ItemRegistry.java)，作为**静态的物品目录**，存储所有游戏中可获取/可购买的物品原型：

```java
public class ItemRegistry {
    // 存储所有可购买物品，Map<物品名称, Item原型>
    private static final Map<String, Item> CATALOG = new LinkedHashMap<>();
    // 物品名称列表（用于随机选取）
    private static final List<String> ITEM_NAMES = new ArrayList<>();

    static {
        // 消耗品
        register("生命浆果", "红彤彤的浆果，使用后可回复25点生命值。", 25);
        register("魔力浆果", "散发着魔力波动的浆果，使用后可回复25点魔力值。", 25);
        register("饼干", "散发着魔法光芒的饼干，使用后提升最大生命值10点。", 40);
        register("药水", "装在玻璃瓶中的红色液体，使用后立即回复30点魔力与20点生命。", 35);
        register("魔法卷轴", "记载着古老咒语的卷轴，使用后可释放强力魔法。", 60);
        // 装备
        register("铁剑", "一把锋利的铁剑，装备后物理攻击力+15。", 80);
        register("铁盾", "坚固的铁盾，装备后物理防御力+10。", 70);
        register("法杖", "蕴含魔法之力的法杖，装备后魔法攻击力+12。", 85);
        // 饰品（高价值）
        register("暗影披风", "由暗影魔龙的鳞片编织而成，佩戴后闪避率+15%，移动速度+20。", 200);
        register("生命戒指", "精灵族工匠以生命之树的枝干打造，佩戴后最大生命值+50，每2秒恢复1点生命。", 150);
        register("元素项链", "蕴含着四种元素之力的古老吊坠，佩戴后魔法攻击力+15，魔法抗性+20%。", 150);
    }

    private static void register(String name, String desc, int price) {
        Item item = new Item(name, desc, price);
        CATALOG.put(name, item);
        ITEM_NAMES.add(name);
    }

    public static Item getItem(String name) { return CATALOG.get(name); }
    public static List<String> getAllNames() { return new ArrayList<>(ITEM_NAMES); }
    public static int getPrice(String name) {
        Item item = CATALOG.get(name);
        return item != null ? item.getPrice() : 0;
    }
    public static boolean exists(String name) { return CATALOG.containsKey(name); }
}
```

> **说明**：`InventoryItem.fromItem()` 已经根据名称映射稀有度和描述，所以注册表中的物品描述可以精简，与 `fromItem()` 保持一致即可。

#### Step 3: 改造 ShopItem，让其持有 Item 引用

修改 [`ShopItem.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/ShopItem.java)，让 `ShopItem` 直接持有 `Item` 引用（从注册表获取），而不是只存一个名字字符串：

```java
public class ShopItem {
    private Item item;          // 商品对应的实际物品（来自注册表）
    private int price;          // 价格
    private boolean sold;       // 是否已售出

    // 构造时从注册表获取Item
    public ShopItem(String itemName) {
        Item catalogItem = ItemRegistry.getItem(itemName);
        this.item = catalogItem != null ? catalogItem : new Item(itemName);
        this.price = catalogItem != null ? catalogItem.getPrice() : 50;
        this.sold = false;
    }

    // 快捷方法
    public String getName() { return item.getName(); }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", item.getName());
        map.put("price", price);
        map.put("sold", sold);
        map.put("description", item.getDescription());
        return map;
    }
}
```

#### Step 4: 改造 Room.initShopItems()

修改 [`Room.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java:229) 的 `initShopItems()`，改为从注册表中随机选取**不同类别**的物品（消耗品、装备、饰品混合），确保商店商品多样性：

```java
public void initShopItems() {
    if (this.shopInitialized) return;
    if (this.shopItems == null) this.shopItems = new ArrayList<>();

    Random rnd = new Random(this.name.hashCode()); // 相同房间每次结果一致
    List<String> allItems = ItemRegistry.getAllNames();
    List<String> selected = new ArrayList<>();

    if (allItems.size() <= 6) {
        selected.addAll(allItems); // 不够6个就全拿
    } else {
        // 从注册表中随机选取6个不同的物品
        List<String> shuffled = new ArrayList<>(allItems);
        Collections.shuffle(shuffled, rnd);
        selected = shuffled.subList(0, 6);
    }

    for (String itemName : selected) {
        this.shopItems.add(new ShopItem(itemName));
    }
    this.shopInitialized = true;
}
```

#### Step 5: 微调 ShopCommand.execute()

在 [`ShopCommand.java`](backend/src/main/java/cn/edu/whut/sept/zuul/command/ShopCommand.java) 中，购买时使用 `ItemRegistry.getItem()` 创建真正的物品：

```java
// 在购买成功后的 item 创建部分
Item item = ItemRegistry.getItem(itemName);
if (item == null) {
    item = new Item(itemName); // fallback
}
player.getBag().addItem(item);
```

### 三、前端影响评估

**前端无需修改**，因为：

1. 前端 `showShopBuy()` 已经通过 `scene.shopNpcData.shopItems` 获取数据
2. 商品显示只依赖 `item.name` 和 `item.price`（在 `toMap()` 中已包含）
3. 购买时发送 `shop buy {{item.name}}`，现在物品名是"生命浆果"、"铁剑"等真实名称
4. 购买成功后的背包显示完全由后端 `InventoryItem.fromItem()` 处理，自动映射稀有度和描述

### 四、涉及的文件清单

| 文件 | 修改类型 | 说明 |
|---|---|---|
| [`backend/src/main/java/cn/edu/whut/sept/zuul/model/Item.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Item.java) | 修改 | 新增 `price` 属性 + 全参构造器 |
| [`backend/src/main/java/cn/edu/whut/sept/zuul/model/ItemRegistry.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/ItemRegistry.java) | **新建** | 静态物品注册表 |
| [`backend/src/main/java/cn/edu/whut/sept/zuul/model/ShopItem.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/ShopItem.java) | 修改 | 持有 `Item` 引用，从注册表构造 |
| [`backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Room.java) | 修改 | `initShopItems()` 改为从注册表随机选6个 |
| [`backend/src/main/java/cn/edu/whut/sept/zuul/command/ShopCommand.java`](backend/src/main/java/cn/edu/whut/sept/zuul/command/ShopCommand.java) | 修改 | 购买时使用注册表中的 Item 原型 |

### 五、执行顺序

1. 修改 `Item.java` → 添加 `price` 字段
2. 新建 `ItemRegistry.java` → 静态注册表
3. 修改 `ShopItem.java` → 持有 Item 引用
4. 修改 `Room.java` → `initShopItems()` 逻辑
5. 修改 `ShopCommand.java` → 购买逻辑微调
