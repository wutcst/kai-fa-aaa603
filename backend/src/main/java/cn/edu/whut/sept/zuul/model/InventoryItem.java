package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 背包物品DTO（展示层）：直接从Item拷贝数据，不再做if-else推断。
 * Item自身已包含稀有度、功能描述、背景描述等信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    private int itemId;          // 物品编号 NO.XX
    private String name;         // 物品名称
    private String rarity;       // 稀有度（从Item直接拷贝）
    private String functionDesc; // 功能描述（从Item直接拷贝）
    private String loreDesc;     // 背景描述（从Item直接拷贝）
    private int quantity;        // 拥有数量
    private boolean equipped;    // 是否已佩戴（仅饰品使用）
    private int price;           // 物品价格（用于商店售卖）

    /**
     * 根据原始Item和数量创建InventoryItem，从Item直接拷贝metadata（未佩戴）
     */
    public static InventoryItem fromItem(Item item, int quantity, int itemId) {
        return fromItem(item, quantity, itemId, false);
    }

    /**
     * 根据原始Item和数量创建InventoryItem，从Item直接拷贝metadata
     * @param equipped 是否已佩戴
     */
    public static InventoryItem fromItem(Item item, int quantity, int itemId, boolean equipped) {
        InventoryItem inv = new InventoryItem();
        inv.itemId = itemId;
        inv.name = item.getName();
        inv.quantity = quantity;
        inv.equipped = equipped;
        // 直接从Item拷贝稀有度、描述和价格，Item构造时已自动推断
        inv.rarity = item.getRarity() != null ? item.getRarity() : "common";
        inv.functionDesc = item.getFunctionDesc() != null ? item.getFunctionDesc() : "一件普通的物品。";
        inv.loreDesc = item.getLoreDesc() != null ? item.getLoreDesc() : "一件普通的物品。";
        inv.price = item.getPrice();
        return inv;
    }
}
