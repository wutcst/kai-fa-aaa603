package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 玩家类，管理基础属性、位置、背包和货币。
 * 增益/减益状态委托给 {@link Status.StatusManager} 处理。
 */
@Getter
@Setter
public class Player {
    private String name;
    private Room currentRoom;
    private Bag bag;
    private Money money;
    private Status.StatusManager statusManager;

    // -- 基础战斗属性 --
    private int hp;
    private int mp;
    private int maxHp;
    private int maxMp;
    private int attack;
    private int magicAttack;
    private int defense;
    private int magicResist;   // 0-100，超出按100计
    private int speed;
    private int dodge;         // 0-100

    // -- 装备/饰品槽位（已装备的物品名称，null表示空） --
    /** 披风槽位 */
    private String equippedCloak;
    /** 戒指槽位 */
    private String equippedRing;
    /** 项链槽位 */
    private String equippedAmulet;
    /** 武器槽位（铁剑等） */
    private String equippedWeapon;
    /** 护甲槽位（铁盾等） */
    private String equippedArmor;

    // -- 伤害计数器 --
    /** 伤害记录列表，每条记录包含时间戳和最终受到的伤害量 */
    private List<DamageRecord> damageRecords;

    /**
     * 伤害记录：记录单次受击的时间戳与经过防御/魔抗结算后的最终伤害量。
     */
    @Getter
    public static class DamageRecord {
        /** 受击时间戳（毫秒） */
        private final long timestamp;
        /** 经过防御/魔抗结算后的最终伤害量 */
        private final int amount;

        public DamageRecord(long timestamp, int amount) {
            this.timestamp = timestamp;
            this.amount = amount;
        }
    }

    public Player(String name, Room currentRoom) {
        this.name = name;
        this.currentRoom = currentRoom;
        this.bag = new Bag();
        this.money = new Money();
        this.statusManager = new Status.StatusManager(this);
        this.damageRecords = new ArrayList<>();

        hp = 100;
        mp = 100;
        maxHp = 100;
        maxMp = 100;
        attack = 20;
        magicAttack = 15;
        defense = 10;
        magicResist = 0;
        speed = 100;
        dodge = 0;

        equippedCloak = null;
        equippedRing = null;
        equippedAmulet = null;
        equippedWeapon = null;
        equippedArmor = null;
    }

    // -- 饰品系统 --

    /**
     * 获取指定物品对应的装备/饰品槽位。
     * @param itemName 物品名称
     * @return "weapon" / "armor" / "cloak" / "ring" / "amulet" / null（不是装备或饰品）
     */
    public static String getItemSlot(String itemName) {
        if (itemName == null) return null;
        String lower = itemName.toLowerCase();
        if (lower.contains("剑")) return "weapon";
        if (lower.contains("盾")) return "armor";
        if (lower.contains("暗影披风")) return "cloak";
        if (lower.contains("生命戒指")) return "ring";
        if (lower.contains("元素项链")) return "amulet";
        return null;
    }

    /**
     * 判断某件物品是否已佩戴在身上。
     */
    public boolean isEquipped(String itemName) {
        String slot = getItemSlot(itemName);
        if (slot == null) return false;
        String equipped = getEquippedInSlot(slot);
        return itemName != null && itemName.equals(equipped);
    }

    /**
     * 获取指定槽位已装备的物品名称。
     */
    public String getEquippedInSlot(String slot) {
        return switch (slot) {
            case "weapon" -> equippedWeapon;
            case "armor" -> equippedArmor;
            case "cloak" -> equippedCloak;
            case "ring" -> equippedRing;
            case "amulet" -> equippedAmulet;
            default -> null;
        };
    }

    /**
     * 佩戴装备/饰品：应用属性加成，记录到槽位。
     * 如果该槽位已有物品，先卸下旧的。
     * @param itemName 装备/饰品名称
     * @return 被替换下的旧物品名称（null表示无替换）
     */
    public String equipItem(String itemName) {
        String slot = getItemSlot(itemName);
        if (slot == null) return null;

        // 如果槽位已有物品，先卸下
        String oldItem = unequipSlot(slot);

        // 应用新物品的属性加成
        String lower = itemName.toLowerCase();
        if (lower.contains("剑")) {
            this.attack += 15;
        } else if (lower.contains("盾")) {
            this.defense += 10;
        } else if (lower.contains("暗影披风")) {
            this.dodge += 15;
            this.speed += 20;
        } else if (lower.contains("生命戒指")) {
            this.maxHp += 50;
            this.hp = Math.min(this.hp + 50, this.maxHp);
        } else if (lower.contains("元素项链")) {
            this.magicAttack += 15;
            this.magicResist += 20;
        }

        // 记录到槽位
        setEquippedSlot(slot, itemName);
        return oldItem;
    }

    /**
     * 卸下装备/饰品：移除属性加成，清空槽位。
     * @param itemName 装备/饰品名称
     * @return true表示卸下成功
     */
    public boolean unequipItem(String itemName) {
        String slot = getItemSlot(itemName);
        if (slot == null) return false;
        if (!isEquipped(itemName)) return false;

        // 移除属性加成
        String lower = itemName.toLowerCase();
        if (lower.contains("剑")) {
            this.attack = Math.max(0, this.attack - 15);
        } else if (lower.contains("盾")) {
            this.defense = Math.max(0, this.defense - 10);
        } else if (lower.contains("暗影披风")) {
            this.dodge = Math.max(0, this.dodge - 15);
            this.speed = Math.max(0, this.speed - 20);
        } else if (lower.contains("生命戒指")) {
            this.maxHp = Math.max(1, this.maxHp - 50);
            this.hp = Math.min(this.hp, this.maxHp);
        } else if (lower.contains("元素项链")) {
            this.magicAttack = Math.max(0, this.magicAttack - 15);
            this.magicResist = Math.max(0, this.magicResist - 20);
        }

        // 清空槽位
        setEquippedSlot(slot, null);
        return true;
    }

    /**
     * 清空指定槽位（内部方法，不处理属性加减）。
     * @param slot 槽位名称
     * @return 原槽位中的物品名称（null表示空）
     */
    private String unequipSlot(String slot) {
        String old = getEquippedInSlot(slot);
        if (old != null) {
            // 先移除属性
            String lower = old.toLowerCase();
            if (lower.contains("剑")) {
                this.attack = Math.max(0, this.attack - 15);
            } else if (lower.contains("盾")) {
                this.defense = Math.max(0, this.defense - 10);
            } else if (lower.contains("暗影披风")) {
                this.dodge = Math.max(0, this.dodge - 15);
                this.speed = Math.max(0, this.speed - 20);
            } else if (lower.contains("生命戒指")) {
                this.maxHp = Math.max(1, this.maxHp - 50);
                this.hp = Math.min(this.hp, this.maxHp);
            } else if (lower.contains("元素项链")) {
                this.magicAttack = Math.max(0, this.magicAttack - 15);
                this.magicResist = Math.max(0, this.magicResist - 20);
            }
        }
        setEquippedSlot(slot, null);
        return old;
    }

    private void setEquippedSlot(String slot, String itemName) {
        switch (slot) {
            case "weapon" -> this.equippedWeapon = itemName;
            case "armor" -> this.equippedArmor = itemName;
            case "cloak" -> this.equippedCloak = itemName;
            case "ring" -> this.equippedRing = itemName;
            case "amulet" -> this.equippedAmulet = itemName;
        }
    }

    // -- 伤害计数器方法 --

    /**
     * 记录一次受到的伤害。
     * 应在防御/魔抗结算后调用，传入最终实际伤害量。
     * 闪避成功时不应调用此方法。
     * @param amount 经过防御/魔抗结算后的最终伤害量（可为0）
     */
    public void recordDamage(int amount) {
        damageRecords.add(new DamageRecord(System.currentTimeMillis(), amount));
    }

    /**
     * 查询最近 windowMs 毫秒内累计受到的总伤害量。
     * @param windowMs 时间窗口（毫秒）
     * @return 时间窗口内的累计伤害量
     */
    public int getRecentDamage(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        int total = 0;
        for (DamageRecord dr : damageRecords) {
            if (dr.timestamp >= cutoff) {
                total += dr.amount;
            }
        }
        return total;
    }

    /**
     * 查询最近 windowMs 毫秒内受到伤害的次数。
     * 不包含闪避成功的攻击，但包含结算后伤害为0的攻击。
     * @param windowMs 时间窗口（毫秒）
     * @return 时间窗口内的受击次数
     */
    public int getRecentHitCount(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        int count = 0;
        for (DamageRecord dr : damageRecords) {
            if (dr.timestamp >= cutoff) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清理超过 windowMs 毫秒的旧记录，避免内存持续增长。
     * @param windowMs 保留的时间窗口（毫秒）
     */
    public void cleanupOldRecords(long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        damageRecords.removeIf(dr -> dr.timestamp < cutoff);
    }

    // -- 受击 --

    /** 物理伤害 = max(0, 原始伤害 - 有效防御)，优先闪避判定 */
    public void takeDamage(int dmg) {
        int effDodge = statusManager.getModifiedDodge();
        if (effDodge > 0 && Math.random() * 100 < effDodge) {
            return; // 闪避成功，不计入伤害计数器
        }
        int effDef = statusManager.getModifiedDefense();
        int actual = Math.max(0, dmg - effDef);
        hp = Math.max(0, hp - actual);
        recordDamage(actual); // 记录最终伤害（含为0的情况）
    }

    /** 魔法伤害 = 原始伤害 × (1 - 有效魔抗/100)，向下取整，优先闪避判定 */
    public void takeMagicDamage(int dmg) {
        int effDodge = statusManager.getModifiedDodge();
        if (effDodge > 0 && Math.random() * 100 < effDodge) {
            return; // 闪避成功，不计入伤害计数器
        }
        int resist = Math.min(100, statusManager.getModifiedMagicResist());
        int actual = (int) Math.floor(dmg * (1.0 - resist / 100.0));
        hp = Math.max(0, hp - actual);
        recordDamage(actual); // 记录最终伤害（含为0的情况）
    }

    // -- 状态修正后的有效属性 --

    public int getEffectiveAttack()      { return statusManager.getModifiedAttack(); }
    public int getEffectiveMagicAttack() { return statusManager.getModifiedMagicAttack(); }
    public int getEffectiveDefense()     { return statusManager.getModifiedDefense(); }
    public int getEffectiveMagicResist() { return statusManager.getModifiedMagicResist(); }
    public int getEffectiveSpeed()       { return statusManager.getModifiedSpeed(); }
    public int getEffectiveDodge()       { return statusManager.getModifiedDodge(); }

    // -- 饰品被动效果tick --

    /** 生命戒指的回血计时器（毫秒时间戳），由 GameService 驱动 */
    private long lastRingRegenTime = 0;
    /** 生命戒指回血间隔：2秒恢复1点生命 */
    public static final long RING_REGEN_INTERVAL = 2000L;

    /**
     * 驱动生命戒指的被动回血。
     * 每2秒恢复1点生命值，仅在佩戴生命戒指时生效。
     * @return 本次实际恢复的生命值
     */
    public int tickRingRegen() {
        if (equippedRing == null || !equippedRing.contains("生命戒指")) {
            lastRingRegenTime = System.currentTimeMillis();
            return 0;
        }
        long now = System.currentTimeMillis();
        if (now - lastRingRegenTime < RING_REGEN_INTERVAL) {
            return 0;
        }
        lastRingRegenTime = now;
        int before = hp;
        restoreHp(1);
        return hp - before;
    }

    // -- 资源管理 --

    public boolean consumeMp(int amount) {
        if (mp >= amount) {
            mp -= amount;
            return true;
        }
        return false;
    }

    public void restoreMp(int amount) {
        mp = Math.min(mp + amount, maxMp);
    }

    public void restoreHp(int amount) {
        hp = Math.min(hp + amount, maxHp);
    }

    /**
     * 应用博学祭坛增益效果。
     * @param boon 增益类型
     * @return 增益描述文本
     */
    public String applyWisdomBoon(WisdomBoon boon) {
        if (boon == null) return "";
        boon.apply(this);
        return "【" + boon.getDisplayName() + "】" + boon.getDescription() + "！";
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
