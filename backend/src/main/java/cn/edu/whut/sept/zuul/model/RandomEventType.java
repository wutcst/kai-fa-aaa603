package cn.edu.whut.sept.zuul.model;

/**
 * 随机事件类型枚举
 * 用于奇遇房间（ENCOUNTER）中的可交互事件
 */
public enum RandomEventType {
    /** 宝箱 — 增加200枚钱币 */
    CHEST("宝箱", 0xDAA520),
    /** 圣女 — 血回满、蓝回满 */
    MAIDEN("圣女", 0xFFB6C1),
    /** 天使 — 整体属性上浮至150%，持续2分钟，身上浮现金光 */
    ANGEL("天使", 0xFFD700),
    /** 铁匠 — 某件饰品或装备功能上浮150% */
    BLACKSMITH("铁匠", 0x888888);

    private final String displayName;
    private final int color;

    RandomEventType(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
