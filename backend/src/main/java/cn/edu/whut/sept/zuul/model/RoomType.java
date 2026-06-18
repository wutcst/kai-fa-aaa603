package cn.edu.whut.sept.zuul.model;

/**
 * 房间类型枚举
 */
public enum RoomType {
    /** 开始大厅 — 玩家初始生成房间，用于基础操作教学 */
    START_HALL("开始大厅"),
    /** 商店房间 */
    SHOP("商店"),
    /** 奇遇房间 */
    ENCOUNTER("奇遇"),
    /** 篝火（休息）房间 */
    CAMPFIRE("篝火休息"),
    /** Boss（领袖）房间 — 仅有一个出口的尽头房间 */
    BOSS("Boss"),
    /** 精英怪物房间 */
    ELITE_MONSTER("精英怪物"),
    /** 普通怪物房间 */
    NORMAL_MONSTER("普通怪物");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
