package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物品类：游戏中实际存在的物品。
 * 创建时自动根据名称推断稀有度(rarity)、功能描述(functionDesc)、背景描述(loreDesc)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private String name;           // 物品名称
    private String description;    // 物品描述
    private int price;             // 物品价格（0 表示不可购买/无价格）
    private String rarity;         // 稀有度（common/rare/epic/legendary/lifeBerry/manaBerry）
    private String functionDesc;   // 功能描述
    private String loreDesc;       // 背景描述

    /**
     * 简化构造器（仅名称，其余自动推断或使用默认值）
     */
    public Item(String name) {
        this.name = name;
        this.description = "普通的" + name;
        this.price = 0;
        inferMetadata(name);
    }

    /**
     * 构造器（名称 + 描述，价格默认为0）
     */
    public Item(String name, String description) {
        this.name = name;
        this.description = description;
        this.price = 0;
        inferMetadata(name);
    }

    /**
     * 全参构造器（名称 + 描述 + 价格，其他自动推断）
     */
    public Item(String name, String description, int price) {
        this.name = name;
        this.description = description;
        this.price = price;
        inferMetadata(name);
    }

    /**
     * 根据物品名称推断稀有度、功能描述和背景描述
     */
    private void inferMetadata(String name) {
        if (name == null) {
            this.rarity = "common";
            this.functionDesc = "一件普通的物品。";
            this.loreDesc = "一件普通的物品。";
            return;
        }

        if (name.contains("生命浆果")) {
            this.rarity = "lifeBerry";
            this.functionDesc = "使用后，回复25点血量。";
            this.loreDesc = "红彤彤的浆果，看上去很好吃......";
        } else if (name.contains("魔力浆果")) {
            this.rarity = "manaBerry";
            this.functionDesc = "使用后，回复25点魔力。";
            this.loreDesc = "比较罕见的浆果，散发着浅浅的魔力波动。";
        } else if (name.contains("cookie") || name.contains("饼干")) {
            this.rarity = "rare";
            this.functionDesc = "使用后，提升最大生命值10点。";
            this.loreDesc = "散发着魔法光芒的饼干，据说吃了会变强壮......";
        } else if (name.contains("potion") || name.contains("药水")) {
            this.rarity = "common";
            this.functionDesc = "使用后，立即回复30点魔力与20点生命。";
            this.loreDesc = "装在玻璃瓶中的红色液体，散发着淡淡的草药香气。";
        } else if (name.contains("sword") || name.contains("剑")) {
            this.rarity = "epic";
            this.functionDesc = "装备后，物理攻击力+15。";
            this.loreDesc = "一把锋利的宝剑，剑身上刻着古老的符文。";
        } else if (name.contains("shield") || name.contains("盾")) {
            this.rarity = "epic";
            this.functionDesc = "装备后，物理防御力+10。";
            this.loreDesc = "坚固的盾牌，上面有战斗留下的痕迹。";
        } else if (name.contains("berry") || name.contains("浆果")) {
            this.rarity = "common";
            this.functionDesc = "使用后，立即回复30点魔力与20点生命。";
            this.loreDesc = "红彤彤的浆果，看上去很好吃......";
        } else if (name.contains("暗影披风")) {
            this.rarity = "legendary";
            this.functionDesc = "佩戴后，永久提升闪避率+15%，移动速度+20。同类饰品只能佩戴一个。";
            this.loreDesc = "由暗影魔龙的鳞片编织而成，披上它仿佛融入了黑暗之中，敌人的攻击难以命中...";
        } else if (name.contains("生命戒指")) {
            this.rarity = "epic";
            this.functionDesc = "佩戴后，永久提升最大生命值+50点，每2秒自动恢复1点生命。同类饰品只能佩戴一个。";
            this.loreDesc = "精灵族工匠以生命之树的枝干打造，散发着柔和的翠绿色光芒...";
        } else if (name.contains("元素项链")) {
            this.rarity = "epic";
            this.functionDesc = "佩戴后，永久提升魔法攻击力+15，魔法抗性+20%。同类饰品只能佩戴一个。";
            this.loreDesc = "蕴含着四种元素之力的古老吊坠，佩戴者能感受到火焰、冰霜、雷电与风暴在脉动...";
        } else {
            this.rarity = "common";
            this.functionDesc = "使用后，恢复少量生命值。";
            this.loreDesc = "一件普通的物品。";
        }

        // 如果价格仍为0，尝试从注册表获取价格
        if (this.price == 0 && name != null) {
            int regPrice = ItemRegistry.getPrice(name);
            if (regPrice > 0) {
                this.price = regPrice;
            }
        }
    }
}
