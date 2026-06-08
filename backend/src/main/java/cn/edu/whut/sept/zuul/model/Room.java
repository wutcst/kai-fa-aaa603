package cn.edu.whut.sept.zuul.model;

import lombok.Data;
import java.util.*;

/**
 * 房间类：包含出口、物品、传输房间标记
 */
@Data
public class Room {
    private String name;                // 房间名称
    private String description;         // 房间描述
    private Map<String, Room> exits;    // 出口（方向->房间）
    private List<Item> items;           // 房间物品列表
    private boolean isTeleportRoom;     // 是否是传输房间（lab）

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.exits = new HashMap<>();
        this.items = new ArrayList<>();
        this.isTeleportRoom = false;
    }

    // 添加出口
    public void setExit(String direction, Room neighbor) {
        exits.put(direction, neighbor);
    }

    // 获取出口描述（如：Exits: east west）
    public String getExitString() {
        StringBuilder sb = new StringBuilder();
        for (String direction : exits.keySet()) {
            sb.append(direction).append(" ");
        }
        return sb.toString().trim();
    }

    // 添加物品到房间
    public void addItem(Item item) {
        items.add(item);
    }

    // 从房间移除物品
    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    // 根据名称查找房间内的物品
    public Item getItem(String itemName) {
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    // 获取房间完整信息（供look命令使用）
    public Map<String, Object> getFullInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", this.name);
        info.put("description", this.description);
        info.put("exits", this.getExitString());
        info.put("items", this.items);
        info.put("isTeleportRoom", this.isTeleportRoom);
        return info;
    }
}

