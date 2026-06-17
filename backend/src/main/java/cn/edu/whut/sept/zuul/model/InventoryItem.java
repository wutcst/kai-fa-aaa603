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
    private String rarity;       // 稀有度（common/rare/epic/legendary/lifeBerry/manaBerry）
    private String functionDesc; // 功能描述
    private String loreDesc;     // 背景描述
    private int quantity;        // 拥有数量
    private boolean equipped;    // 是否已佩戴（仅饰品使用）

    /**
     * 根据原始Item和数量创建InventoryItem，自动推断稀有度和描述（未佩戴）
     */
    public static InventoryItem fromItem(Item item, int quantity, int itemId) {
        return fromItem(item, quantity, itemId, false);
    }

    /**
     * 根据原始Item和数量创建InventoryItem，自动推断稀有度和描述
     * @param equipped 是否已佩戴
     */
    public static InventoryItem fromItem(Item item, int quantity, int itemId, boolean equipped) {
        InventoryItem inv = new InventoryItem();
        inv.itemId = itemId;
        inv.name = item.getName();
        inv.quantity = quantity;
        inv.equipped = equipped;

        // 根据物品名称推断稀有度和描述
        String name = item.getName();

        if (name.contains("生命浆果")) {
            inv.rarity = "lifeBerry";
            inv.functionDesc = "使用后，回复25点血量。";
            inv.loreDesc = "红彤彤的浆果，看上去很好吃......";
        } else if (name.contains("魔力浆果")) {
            inv.rarity = "manaBerry";
            inv.functionDesc = "使用后，回复25点魔力。";
            inv.loreDesc = "比较罕见的浆果，散发着浅浅的魔力波动。";
        } else if (name.contains("cookie") || name.contains("饼干")) {
            inv.rarity = "rare";
            inv.functionDesc = "使用后，提升最大生命值10点。";
            inv.loreDesc = "散发着魔法光芒的饼干，据说吃了会变强壮......";
        } else if (name.contains("potion") || name.contains("药水")) {
            inv.rarity = "common";
            inv.functionDesc = "使用后，立即回复30点魔力与20点生命。";
            inv.loreDesc = "装在玻璃瓶中的红色液体，散发着淡淡的草药香气。";
        } else if (name.contains("sword") || name.contains("剑")) {
            inv.rarity = "epic";
            inv.functionDesc = "装备后，物理攻击力+15。";
            inv.loreDesc = "一把锋利的宝剑，剑身上刻着古老的符文。";
        } else if (name.contains("shield") || name.contains("盾")) {
            inv.rarity = "epic";
            inv.functionDesc = "装备后，物理防御力+10。";
            inv.loreDesc = "坚固的盾牌，上面有战斗留下的痕迹。";
        } else if (name.contains("berry") || name.contains("浆果")) {
            inv.rarity = "common";
            inv.functionDesc = "使用后，立即回复30点魔力与20点生命。";
            inv.loreDesc = "红彤彤的浆果，看上去很好吃......";
        } else if (name.contains("暗影披风")) {
            inv.rarity = "legendary";
            inv.functionDesc = "佩戴后，永久提升闪避率+15%，移动速度+20。同类饰品只能佩戴一个。";
            inv.loreDesc = "由暗影魔龙的鳞片编织而成，披上它仿佛融入了黑暗之中，敌人的攻击难以命中...";
        } else if (name.contains("生命戒指")) {
            inv.rarity = "epic";
            inv.functionDesc = "佩戴后，永久提升最大生命值+50点，每2秒自动恢复1点生命。同类饰品只能佩戴一个。";
            inv.loreDesc = "精灵族工匠以生命之树的枝干打造，散发着柔和的翠绿色光芒...";
        } else if (name.contains("元素项链")) {
            inv.rarity = "epic";
            inv.functionDesc = "佩戴后，永久提升魔法攻击力+15，魔法抗性+20%。同类饰品只能佩戴一个。";
            inv.loreDesc = "蕴含着四种元素之力的古老吊坠，佩戴者能感受到火焰、冰霜、雷电与风暴在脉动...";
        } else {
            inv.rarity = "common";
            inv.functionDesc = "使用后，恢复少量生命值。";
            inv.loreDesc = "一件普通的物品。";
        }

        return inv;
    }
}
