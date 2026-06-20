package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 祭坛模型类：篝火房间中的可交互对象
 * 每个篝火房间有三种祭坛：回复、锻炼、博学
 * 一个篝火房间只能选择一个祭坛进行交互一次
 */
@Getter
@Setter
public class Altar {
    /** 祭坛类型 */
    private AltarType type;
    /** 是否已被激活（交互过） */
    private boolean activated;
    /** 该房间是否已有祭坛被选择过 */
    private boolean roomUsed;
    /** 博学祭坛待选增益列表（仅博学祭坛使用，交互时随机生成3个） */
    private List<WisdomBoon> pendingBoons;

    public Altar(AltarType type) {
        this.type = type;
        this.activated = false;
        this.roomUsed = false;
        this.pendingBoons = new ArrayList<>();
    }

    /**
     * 激活祭坛，返回是否成功
     */
    public boolean activate() {
        if (!activated && !roomUsed) {
            this.activated = true;
            this.roomUsed = true;
            return true;
        }
        return false;
    }

    /**
     * 标记同房间所有祭坛为已使用
     */
    public void markRoomUsed() {
        this.roomUsed = true;
    }

    /**
     * 将祭坛转为 Map 供前端使用
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("displayName", type.getDisplayName());
        map.put("color", type.getColor());
        map.put("activated", activated);
        map.put("roomUsed", roomUsed);
        // 博学祭坛：返回待选增益列表
        if (pendingBoons != null && !pendingBoons.isEmpty()) {
            List<Map<String, String>> boonList = new ArrayList<>();
            for (WisdomBoon boon : pendingBoons) {
                boonList.add(boon.toMap());
            }
            map.put("pendingBoons", boonList);
        }
        return map;
    }
}
