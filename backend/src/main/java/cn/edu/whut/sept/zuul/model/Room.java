package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 房间类：包含出口、物品、怪物、传输房间标记、房间类型、祭坛
 */
@Getter
@Setter
public class Room {
    private String name;
    private String description;
    private Map<String, Room> exits;
    private List<Item> items;
    private List<Monster> monsters;
    private boolean isTeleportRoom;
    private RoomType roomType;
    // 祭坛系统（仅篝火房间使用）
    private List<Altar> altars;
    // 该房间是否有祭坛已被选择（持久标记，再次进入不刷新）
    private boolean altarUsed;

    public Room(String name, String description) {
        this.name = name;
        this.description = description;
        this.exits = new HashMap<>();
        this.items = new ArrayList<>();
        this.monsters = new ArrayList<>();
        this.isTeleportRoom = false;
        this.roomType = RoomType.NORMAL_MONSTER; // 默认普通怪物房
        this.altars = new ArrayList<>();
        this.altarUsed = false;
    }

    public void setExit(String direction, Room neighbor) {
        exits.put(direction, neighbor);
    }

    public String getExitString() {
        StringBuilder sb = new StringBuilder();
        for (String direction : exits.keySet()) {
            sb.append(direction).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * 获取出口数量（用于判断是否为尽头房间等）
     */
    public int getExitCount() {
        return exits.size();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    public Item getItem(String itemName) {
        for (Item item : items) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    public void addMonster(Monster m) {
        if (m != null) monsters.add(m);
    }

    public boolean removeMonster(Monster m) {
        return monsters.remove(m);
    }

    public Monster getMonster(String name) {
        for (Monster m : monsters) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public Map<String, Object> getFullInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", this.name);
        info.put("description", this.description);
        info.put("exits", this.getExitString());
        info.put("items", this.items);
        info.put("monsters", this.monsters);
        info.put("isTeleportRoom", this.isTeleportRoom);
        info.put("roomType", this.roomType != null ? this.roomType.name() : RoomType.NORMAL_MONSTER.name());
        // 祭坛数据
        info.put("altarUsed", this.altarUsed);
        List<Map<String, Object>> altarList = new ArrayList<>();
        if (this.altars != null) {
            for (Altar altar : this.altars) {
                altarList.add(altar.toMap());
            }
        }
        info.put("altars", altarList);
        return info;
    }

    /**
     * 初始化祭坛（篝火房间调用）
     * 创建三个祭坛：HEAL（左）、TRAIN（中）、WISDOM（右）
     */
    public void initAltars() {
        if (this.altars == null || this.altars.isEmpty()) {
            this.altars = new ArrayList<>();
            this.altars.add(new Altar(AltarType.HEAL));
            this.altars.add(new Altar(AltarType.TRAIN));
            this.altars.add(new Altar(AltarType.WISDOM));
        }
        // 同步 roomUsed 状态到所有祭坛
        for (Altar a : this.altars) {
            if (this.altarUsed) {
                a.markRoomUsed();
            }
        }
    }

    /**
     * 获取指定类型的祭坛
     */
    public Altar getAltar(AltarType type) {
        if (this.altars == null) return null;
        for (Altar a : this.altars) {
            if (a.getType() == type) return a;
        }
        return null;
    }

    /**
     * 激活祭坛（返回激活的祭坛，null表示失败）
     */
    public Altar activateAltar(AltarType type) {
        Altar altar = getAltar(type);
        if (altar != null && altar.activate()) {
            this.altarUsed = true;
            // 标记所有其他祭坛为 roomUsed
            for (Altar a : this.altars) {
                a.markRoomUsed();
            }
            return altar;
        }
        return null;
    }

    // 安全的 hashCode / equals（只使用 name）
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room room = (Room) o;
        return Objects.equals(name, room.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    // 安全的 toString（不包含 exits 等循环引用字段）
    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isTeleportRoom=" + isTeleportRoom +
                ", roomType=" + (roomType != null ? roomType.getDisplayName() : "NORMAL_MONSTER") +
                '}';
    }
}
