package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 背包物品类：带编号、稀有度、功能描述、背景描述等UI展示字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    private int itemId;          // 物品编号 NO.XX
    private String name;         // 物品名称
    private String rarity;       // 稀有度（common/rare/epic/legendary）
    private String functionDesc; // 功能描述
    private String loreDesc;     // 背景描述
    private double weight;       // 单件重量
    private int quantity;        // 拥有数量

    /**
     * 根据原始Item和数量创建InventoryItem，自动推断稀有度和描述
     */
    public static InventoryItem fromItem(Item item, int quantity, int itemId) {
        InventoryItem inv = new InventoryItem();
        inv.itemId = itemId;
        inv.name = item.getName();
        inv.weight = item.getWeight();
        inv.quantity = quantity;

        // 根据物品名称推断稀有度和描述
        String lowerName = item.getName().toLowerCase();
        if (lowerName.contains("生命浆果")) {
            inv.rarity = "lifeBerry";
            inv.functionDesc = "使用后，回复25点血量。";
            inv.loreDesc = "红彤彤的浆果，看上去很好吃......";
        } else if (lowerName.contains("魔力浆果")) {
            inv.rarity = "manaBerry";
            inv.functionDesc = "使用后，回复25点魔力。";
            inv.loreDesc = "比较罕见的浆果，散发着浅浅的魔力波动。";
        } else if (lowerName.contains("cookie") || lowerName.contains("饼干")) {
            inv.rarity = "rare";
            inv.functionDesc = "使用后，永久增加最大负重5kg。";
            inv.loreDesc = "散发着魔法光芒的饼干，据说吃了会变强壮......";
        } else if (lowerName.contains("potion") || lowerName.contains("药水")) {
            inv.rarity = "common";
            inv.functionDesc = "使用后，立即回复30点魔力与20点生命。";
            inv.loreDesc = "装在玻璃瓶中的红色液体，散发着淡淡的草药香气。";
        } else if (lowerName.contains("sword") || lowerName.contains("剑")) {
            inv.rarity = "epic";
            inv.functionDesc = "装备后，物理攻击力+15。";
            inv.loreDesc = "一把锋利的宝剑，剑身上刻着古老的符文。";
        } else if (lowerName.contains("shield") || lowerName.contains("盾")) {
            inv.rarity = "epic";
            inv.functionDesc = "装备后，物理防御力+10。";
            inv.loreDesc = "坚固的盾牌，上面有战斗留下的痕迹。";
        } else if (lowerName.contains("berry") || lowerName.contains("浆果")) {
            inv.rarity = "common";
            inv.functionDesc = "使用后，立即回复30点魔力与20点生命。";
            inv.loreDesc = "红彤彤的浆果，看上去很好吃......";
        } else {
            inv.rarity = "common";
            inv.functionDesc = "使用后，恢复少量生命值。";
            inv.loreDesc = "一件普通的物品。";
        }

        return inv;
    }
}
