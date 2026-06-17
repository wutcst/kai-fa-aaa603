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

    public boolean isAlive() {
        return hp > 0;
    }
}
