package cn.edu.whut.sept.zuul.model;

/**
 * 祭坛类型枚举
 */
public enum AltarType {
    /** 回复祭坛 — 回复最大HP和MP的50% */
    HEAL("回复祭坛", 0x44cc44),
    /** 锻炼祭坛 — 攻击、魔攻、防御 +25%，魔抗 +10 */
    TRAIN("锻炼祭坛", 0xff8800),
    /** 博学祭坛 — 三选一增益（暂空） */
    WISDOM("博学祭坛", 0x4488ff);

    private final String displayName;
    private final int color;

    AltarType(String displayName, int color) {
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
