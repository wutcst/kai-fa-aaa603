package cn.edu.whut.sept.zuul.model;

/**
 * 随机事件类型枚举
 * 用于奇遇房间（ENCOUNTER）中的可交互事件
 *
 * 事件分为三档：
 * - 优质事件（roll 1-20）：木制宝箱 / 金色宝箱
 * - 劣质事件（roll 21-50）：精英敌人
 * - 一般事件（roll 51-100）：喷泉
 */
public enum RandomEventType {
    // ===== 旧版事件（保留兼容） =====
    /** 宝箱 — 增加200枚钱币 */
    CHEST("宝箱", 0xDAA520),
    /** 圣女 — 血回满、蓝回满 */
    MAIDEN("圣女", 0xFFB6C1),
    /** 天使 — 整体属性上浮至150%，持续30秒，身上浮现金光 */
    ANGEL("天使", 0xFFD700),
    /** 铁匠 — 某件饰品或装备功能上浮150% */
    BLACKSMITH("铁匠", 0x888888),

    // ===== 新版奇遇事件 =====
    /** 木制宝箱 — 交互获得75-150金币（优质事件） */
    WOODEN_CHEST("木制宝箱", 0xCD853F),
    /** 金色宝箱 — 交互获得999金币（优质事件，25%概率替换木箱） */
    GOLDEN_CHEST("金色宝箱", 0xFFD700),
    /** 精英敌人 — 击败后施加负面状态（劣质事件） */
    ELITE_ENEMY("精英敌人", 0xDC143C),
    /** 喷泉 — 50%失血得钱/50%失钱回血（一般事件） */
    FOUNTAIN("喷泉", 0x00BFFF);

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
