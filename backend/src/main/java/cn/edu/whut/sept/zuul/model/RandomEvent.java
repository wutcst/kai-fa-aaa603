package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;

/**
 * 随机事件实体类
 * 在奇遇房间（ENCOUNTER）中生成，玩家靠近按空格键交互。
 *
 * 事件分三档（roll 1-100）：
 * - 优质（1-20）：WOODEN_CHEST(75-150金) / GOLDEN_CHEST(999金，25%触发)
 * - 劣质（21-50）：ELITE_ENEMY（击败后施加烧伤/中毒/流血 5-10层）
 * - 一般（51-100）：FOUNTAIN（50%失10HP得20金 / 50%失25金回10HP）
 *
 * 旧版事件 CHEST/MAIDEN/ANGEL/BLACKSMITH 仍保留兼容。
 */
@Getter
@Setter
public class RandomEvent {
    /** 事件类型 */
    private RandomEventType type;
    /** 是否已被交互过 */
    private boolean used;
    /** 铁匠专用的目标物品名称（由玩家选择） */
    private String blacksmithTargetItem;

    // ===== 新版奇遇事件数据字段 =====

    /** 宝箱事件：金币奖励数（木制75-150 / 金色999） */
    private int rewardGold;
    /** 精英敌人事件：击败后施加的负面状态类型（烧伤/中毒/流血） */
    private String debuffType;
    /** 精英敌人事件：击败后施加的负面状态层数（5-10） */
    private int debuffLayers;
    /** 精英敌人事件：精英怪物的名字（用于在房间中定位） */
    private String eliteEnemyName;
    /** 喷泉事件：是否为失血得钱模式（true=失10HP得20金，false=失25金回10HP） */
    private boolean fountainBloodMode;

    public RandomEvent(RandomEventType type) {
        this.type = type;
        this.used = false;
        this.blacksmithTargetItem = null;
        this.rewardGold = 0;
        this.debuffType = null;
        this.debuffLayers = 0;
        this.eliteEnemyName = null;
        this.fountainBloodMode = false;
    }

    /**
     * 标记为已使用
     */
    public void markUsed() {
        this.used = true;
    }

    /**
     * 转为 Map 供前端使用
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("displayName", type.getDisplayName());
        map.put("color", type.getColor());
        map.put("used", used);
        map.put("blacksmithTargetItem", blacksmithTargetItem);
        // 新版奇遇事件数据
        map.put("rewardGold", rewardGold);
        map.put("debuffType", debuffType);
        map.put("debuffLayers", debuffLayers);
        map.put("eliteEnemyName", eliteEnemyName);
        map.put("fountainBloodMode", fountainBloodMode);
        return map;
    }
}
