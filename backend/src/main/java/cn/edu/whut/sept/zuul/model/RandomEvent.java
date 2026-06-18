package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;

/**
 * 随机事件实体类
 * 在奇遇房间（ENCOUNTER）中生成，玩家靠近按空格键交互
 * 类型包括：宝箱（钱币+200）、圣女（满血满蓝）、天使（属性150%持续2分钟）、铁匠（装备/饰品增强）
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

    public RandomEvent(RandomEventType type) {
        this.type = type;
        this.used = false;
        this.blacksmithTargetItem = null;
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
        return map;
    }
}
