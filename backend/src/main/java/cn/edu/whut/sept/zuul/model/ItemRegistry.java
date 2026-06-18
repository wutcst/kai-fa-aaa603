package cn.edu.whut.sept.zuul.model;

import java.util.*;

/**
 * 物品注册表：存储游戏中所有可获取/可购买的物品原型。
 * 提供按名称查找物品、获取价格、随机选取等功能。
 * 商店系统从此注册表中选取商品，而非生成伪随机"商品X"。
 */
public class ItemRegistry {

    /** 物品目录 Map<物品名称, Item原型> */
    private static final Map<String, Item> CATALOG = new LinkedHashMap<>();

    /** 所有物品名称列表（保持注册顺序） */
    private static final List<String> ITEM_NAMES = new ArrayList<>();

    static {
        // ===== 消耗品 =====
        register("生命浆果",
                "红彤彤的浆果，使用后可回复25点生命值。",
                25);
        register("魔力浆果",
                "散发着魔力波动的浆果，使用后可回复25点魔力值。",
                25);
        register("饼干",
                "散发着魔法光芒的饼干，使用后提升最大生命值10点。",
                40);
        register("药水",
                "装在玻璃瓶中的红色液体，使用后立即回复30点魔力与20点生命。",
                35);

        // ===== 装备 =====
        register("铁剑",
                "一把锋利的铁剑，装备后物理攻击力+15。",
                80);
        register("铁盾",
                "坚固的铁盾，装备后物理防御力+10。",
                70);

        // ===== 饰品（高价值） =====
        register("暗影披风",
                "由暗影魔龙的鳞片编织而成，佩戴后闪避率+15%，移动速度+20。",
                200);
        register("生命戒指",
                "精灵族工匠以生命之树的枝干打造，佩戴后最大生命值+50，每2秒恢复1点生命。",
                150);
        register("元素项链",
                "蕴含着四种元素之力的古老吊坠，佩戴后魔法攻击力+15，魔法抗性+20%。",
                150);
    }

    private static void register(String name, String description, int price) {
        Item item = new Item(name, description, price);
        CATALOG.put(name, item);
        ITEM_NAMES.add(name);
    }

    /**
     * 根据物品名称获取原型 Item（每次返回一个新实例，避免引用修改）
     */
    public static Item getItem(String name) {
        Item prototype = CATALOG.get(name);
        if (prototype == null) return null;
        return new Item(prototype.getName(), prototype.getDescription(), prototype.getPrice());
    }

    /**
     * 获取所有注册物品的名称列表
     */
    public static List<String> getAllNames() {
        return new ArrayList<>(ITEM_NAMES);
    }

    /**
     * 获取指定物品的价格
     */
    public static int getPrice(String name) {
        Item item = CATALOG.get(name);
        return item != null ? item.getPrice() : 0;
    }

    /**
     * 检查物品是否存在于注册表中
     */
    public static boolean exists(String name) {
        return CATALOG.containsKey(name);
    }

    /**
     * 从注册表中随机选取指定数量的不同物品名称
     * @param count 要选取的数量
     * @param rnd   随机数生成器
     * @return 选中的物品名称列表（不超过注册表总大小）
     */
    public static List<String> pickRandom(int count, Random rnd) {
        List<String> shuffled = new ArrayList<>(ITEM_NAMES);
        Collections.shuffle(shuffled, rnd);
        int actualCount = Math.min(count, shuffled.size());
        return shuffled.subList(0, actualCount);
    }

    /**
     * 获取注册表中物品的总数
     */
    public static int size() {
        return ITEM_NAMES.size();
    }
}
