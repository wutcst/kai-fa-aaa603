package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import java.util.HashMap;

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

    public Altar(AltarType type) {
        this.type = type;
        this.activated = false;
        this.roomUsed = false;
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
        return map;
    }
}
