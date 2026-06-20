# Item 与 InventoryItem 合并分析

## 当前两个类的字段

| `Item`（后端的"实物"） | `InventoryItem`（前端展示用） |
|---|---|
| `name` | `itemId`（展示序号） |
| `description` | `name` |
| `price` ← 刚加的 | `rarity`（稀有度） |
| | `functionDesc`（功能描述） |
| | `loreDesc`（背景描述） |
| | `quantity`（拥有数量） |
| | `equipped`（是否佩戴） |

## 核心矛盾

**`Item` 存的是"个体"**，背包里有5个生命浆果，`Bag.inventory` 里就有5个 `Item` 对象。

**`InventoryItem` 是"合并后展示"**，`Bag.getBackpackItems()` 把5个同名 `Item` 合并成1个 `InventoryItem(quantity=5)`。

## 能否合并？

**技术上可以，但有代价。**

| 方面 | 现在（分开） | 合并后 |
|---|---|---|
| 背包存储 | `List<Item>`（每个1份） | `List<Item>`（每个1份，但多了rarity等字段） |
| 前端展示 | `getBackpackItems()` 动态生成 | `getBackpackItems()` 合并逻辑不变 |
| 稀有度映射 | `InventoryItem.fromItem()` 中的if-else链 | 移到 `Item` 构造器中自动填充 |
| 复杂度 | 清晰的两层分离 | 一个类承担两种职责 |

## 合并方案

把两个类的字段合并到 `Item`，然后删掉 `InventoryItem`：

```java
// 合并后的 Item
public class Item {
    // === 基础属性（原有）===
    private String name;
    private String description;
    private int price;

    // === 展示属性（从 InventoryItem 移过来）===
    private String rarity;
    private String functionDesc;
    private String loreDesc;
    private int quantity;     // 在 Bag 中总是1，在展示时用于合并计数
    private boolean equipped; // 仅在展示时有意义
    private int itemId;       // 仅在展示时使用

    public Item(String name, String description, int price) {
        // ... 自动根据名称推断 rarity, functionDesc, loreDesc
    }
}
```

## 影响范围

| 文件 | 改动 |
|---|---|
| [`Item.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Item.java) | 加6个字段+推断逻辑 |
| [`InventoryItem.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/InventoryItem.java) | **删除** |
| [`Bag.java`](backend/src/main/java/cn/edu/whut/sept/zuul/model/Bag.java) | `getBackpackItems()` 返回类型改为 `List<Item>`，合并逻辑调整 |
| [`GameService.java`](backend/src/main/java/cn/edu/whut/sept/zuul/service/GameService.java) | `backpack` 数据类型从 `InventoryItem` 改为 `Item`（JSON字段名不变） |
| 其他 **9个文件** | `Item` 引用不变，`InventoryItem` 的 import 改为 `Item` |

## 我的建议

**不建议合并**，原因：

1. **职责分离清晰**：`Item` 是"实际物品"，`InventoryItem` 是"展示名片"，各管各的
2. **`equipped` 和 `quantity` 在存储层无意义** — 背包里存的是单个物品，只有展示时才需要合并计数和佩戴标记
3. **改动量大**：涉及13+个文件，`InventoryItem` 被前端JSON直接引用，改完后端字段名可能影响前端
4. **风险**：`Bag` 的增删改查逻辑依赖 `Item` 作为"个体单位"，强行加 `quantity` 会混淆

如果你觉得类太多看着乱，一个折中方案是：**把 `InventoryItem` 改为 `Item` 的内部类**，或者把 `fromItem()` 的推断逻辑搬到 `Item` 中，保留 `InventoryItem` 作为纯粹的展示DTO。
