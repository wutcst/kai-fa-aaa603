# 装备系统改造方案（铁剑/铁盾 → 可佩戴/可卸下）

## 目标
铁剑和铁盾现在 `bag use` 后直接加属性+被消耗（消失）。
改为像饰品一样：佩戴→加属性→物品留在背包显示"已装备"→可卸下→移除属性。

## 改动点

### 1. Player.java — 新增武器/护甲槽位 + 装备/卸下逻辑
- 新增 `equippedWeapon`、`equippedArmor` 字段
- `getItemSlot()` 识别铁剑→"weapon"、铁盾→"armor"
- `equipItem()` 新增 weapon/armor 分支
- `unequipItem()` 新增 weapon/armor 分支
- `isEquipped()` 通过 getItemSlot 自动适配所有类型

### 2. BagCommand.java — 装备走饰品路径
- `executeUse()` 中删除剑/盾的消耗品分支
- 剑/盾被 `getItemSlot()` 识别后自动走饰品佩戴路径

### 3. Bag.java — 装备不合并
- `getBackpackItems()` 中把剑/盾也像饰品一样单独占格子

## 文件变更清单
| 文件 | 改动 |
|---|---|
| `Player.java` | 新增字段 + equip/unequip 逻辑 |
| `BagCommand.java` | 删除剑/盾消耗品分支 |
| `Bag.java` | 装备不合并 |
