package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 房间类：包含出口、物品、怪物、传输房间标记、房间类型、祭坛、商店
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
    // 商店系统（仅商店房间使用）
    private List<ShopItem> shopItems;
    // 商店是否已初始化
    private boolean shopInitialized;

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
        this.shopItems = new ArrayList<>();
        this.shopInitialized = false;
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
        // 如果是商店房间，确保商品已初始化
        if (this.roomType == RoomType.SHOP) {
            initShopItems();
        }
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
        // 商店数据（仅商店房间有意义）
        info.put("shopInitialized", this.shopInitialized);
        info.put("isShop", this.roomType == RoomType.SHOP);
        List<Map<String, Object>> shopItemList = new ArrayList<>();
        if (this.shopItems != null) {
            for (ShopItem si : this.shopItems) {
                shopItemList.add(si.toMap());
            }
        }
        info.put("shopItems", shopItemList);
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

    /**
     * 初始化商店商品（仅商店房间调用，幂等操作）
     * 从 50 个随机数中选取 6 个作为商品，价格在 10-200 随机
     */
    public void initShopItems() {
        if (this.shopInitialized) return;
        if (this.shopItems == null) this.shopItems = new ArrayList<>();
        Random rnd = new Random();
        // 先用房间 name 的 hashCode 作为种子保证同一房间每次生成相同结果
        long seed = this.name.hashCode();
        rnd.setSeed(seed);
        for (int i = 0; i < 6; i++) {
            int itemNum = rnd.nextInt(50) + 1;
            int price = rnd.nextInt(191) + 10; // 10-200
            ShopItem si = new ShopItem("商品" + itemNum, price, false);
            this.shopItems.add(si);
        }
        this.shopInitialized = true;
    }

    /**
     * 根据房间类型生成怪物（幂等：仅当房间无怪物时生成）
     */
    public void spawnMonsters() {
        if (monsters != null && !monsters.isEmpty()) return;
        if (roomType == null) return;
        Random rnd = new Random();
        switch (roomType) {
            case BOSS -> spawnBossMonster(rnd);
            case ELITE_MONSTER -> spawnEliteMonsters(rnd);
            case NORMAL_MONSTER -> spawnNormalMonsters(rnd);
            default -> { /* 非怪物房间不生成 */ }
        }
    }

    private void spawnBossMonster(Random rnd) {
        String[] bossNames = new String[]{"巨龙", "恶魔领主", "巫妖王", "巨型石魔像", "暗影之王"};
        String base = bossNames[rnd.nextInt(bossNames.length)];
        String mname = base + "#" + (rnd.nextInt(9000) + 1000);
        String desc = "恐怖的" + base + "——最终挑战";
        int hp = 600 + rnd.nextInt(201);   // 600-800
        int attack = 30 + rnd.nextInt(6); // 30-35
        addMonster(new Monster(mname, desc, hp, attack, Monster.TYPE_BOSS));
    }

    private void spawnEliteMonsters(Random rnd) {
        String[] eliteNames = new String[]{"暗影骑士", "地狱犬", "石像鬼", "暗黑法师", "巨型蜘蛛"};
        int count = 1 + rnd.nextInt(2); // 1-2 个怪物
        for (int i = 0; i < count; i++) {
            String base = eliteNames[rnd.nextInt(eliteNames.length)];
            String mname = base + "#" + (rnd.nextInt(9000) + 1000);
            String desc = "强大的" + base;
            int hp = 200 + rnd.nextInt(101);    // 200-300
            int attack = 20 + rnd.nextInt(6);   // 20-25
            addMonster(new Monster(mname, desc, hp, attack, Monster.TYPE_ELITE));
        }
    }

    private void spawnNormalMonsters(Random rnd) {
        String[] monsterNames = new String[]{"哥布林", "史莱姆", "骷髅", "狼人", "食人魔"};
        int count = 1 + rnd.nextInt(2); // 1-2 个怪物
        // 约 25% 概率将其中一个普通怪物替换为火焰史莱姆
        boolean flameSlimeSpawned = false;
        for (int i = 0; i < count; i++) {
            // 如果还没生成火焰史莱姆且为最后一个怪物或随机命中，生成火焰史莱姆
            if (!flameSlimeSpawned && rnd.nextInt(4) == 0) {
                String suffix = String.valueOf(rnd.nextInt(9000) + 1000);
                addMonster(Monster.createFlameSlime(suffix));
                flameSlimeSpawned = true;
            } else {
                String base = monsterNames[rnd.nextInt(monsterNames.length)];
                String mname = base + "#" + (rnd.nextInt(9000) + 1000);
                String desc = "一个可怕的" + base;
                int hp = 80 + rnd.nextInt(21);   // 80-100
                int attack = 15 + rnd.nextInt(4); // 15-18
                addMonster(new Monster(mname, desc, hp, attack, Monster.TYPE_NORMAL));
            }
        }
    }

    /**
     * 获取指定名称的商店商品
     */
    public ShopItem getShopItem(String name) {
        if (this.shopItems == null) return null;
        for (ShopItem si : this.shopItems) {
            if (si.getName().equals(name)) return si;
        }
        return null;
    }

    /**
     * 获取未售出的商店商品列表
     */
    public List<ShopItem> getAvailableShopItems() {
        List<ShopItem> available = new ArrayList<>();
        if (this.shopItems == null) return available;
        for (ShopItem si : this.shopItems) {
            if (!si.isSold()) available.add(si);
        }
        return available;
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
