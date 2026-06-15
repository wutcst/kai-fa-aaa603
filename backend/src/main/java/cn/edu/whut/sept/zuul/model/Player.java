package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家类：管理玩家状态、背包和负重
 */
@Getter
@Setter
public class Player {
    private String name;
    private Room currentRoom;
    private List<Item> inventory;
    private double maxWeight;

    // ---- 战斗属性 ----
    private int hp;           // 当前生命值
    private int mp;           // 当前魔力值
    private int maxHp;        // 最大生命值
    private int maxMp;        // 最大魔力值
    private int attack;       // 物理攻击力
    private int magicAttack;  // 魔法攻击力
    private int defense;      // 物理防御力
    private int magicResist;  // 魔法抗性（百分比，0-100，超过100按100计算）
    private int speed;        // 移动速度
    private int dodge;        // 闪避率（百分比，0-100）

    public Player(String name, Room currentRoom) {
        this.name = name;
        this.currentRoom = currentRoom;
        this.inventory = new ArrayList<>();
        this.maxWeight = 10.0;

        // 初始属性
        this.hp = 100;
        this.mp = 100;
        this.maxHp = 100;
        this.maxMp = 100;
        this.attack = 20;
        this.magicAttack = 15;
        this.defense = 10;
        this.magicResist = 0;
        this.speed = 100;
        this.dodge = 0;
    }

    /**
     * 计算当前背包总重量
     */
    public double getTotalWeight() {
        double total = 0;
        for (Item item : inventory) {
            total += item.getWeight();
        }
        return total;
    }

    /**
     * 判断能否携带此物品（不超过最大负重）
     */
    public boolean canCarry(Item item) {
        return getTotalWeight() + item.getWeight() <= maxWeight;
    }

    /**
     * 添加物品到背包
     */
    public boolean addItem(Item item) {
        if (canCarry(item)) {
            inventory.add(item);
            return true;
        }
        return false;
    }

    /**
     * 从背包移除物品
     */
    public boolean removeItem(Item item) {
        return inventory.remove(item);
    }

    /**
     * 受到物理伤害：最终伤害 = 原始伤害 - 物防（最低为0）
     * 闪避判定优先：有 dodge% 概率完全免受此次伤害
     */
    public void takeDamage(int dmg) {
        // 闪避判定
        if (dodge > 0 && Math.random() * 100 < dodge) {
            return;
        }
        int actualDmg = Math.max(0, dmg - defense);
        this.hp = Math.max(0, this.hp - actualDmg);
    }

    /**
     * 受到魔法伤害：最终伤害 = 原始伤害 * (1 - 魔抗/100)
     * 魔抗超过100按100计算（即完全免疫）
     * 闪避判定优先：有 dodge% 概率完全免受此次伤害
     */
    public void takeMagicDamage(int dmg) {
        // 闪避判定
        if (dodge > 0 && Math.random() * 100 < dodge) {
            return;
        }
        int resist = Math.min(100, magicResist);
        int actualDmg = (int) Math.round(dmg * (1.0 - resist / 100.0));
        this.hp = Math.max(0, this.hp - actualDmg);
    }

    /**
     * 消耗魔力，返回是否成功
     */
    public boolean consumeMp(int amount) {
        if (this.mp >= amount) {
            this.mp -= amount;
            return true;
        }
        return false;
    }

    /**
     * 恢复魔力
     */
    public void restoreMp(int amount) {
        this.mp = Math.min(this.mp + amount, this.maxMp);
    }

    /**
     * 恢复生命值
     */
    public void restoreHp(int amount) {
        this.hp = Math.min(this.hp + amount, this.maxHp);
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    /**
     * 根据名称在背包中查找物品
     */
    public Item getItem(String itemName) {
        for (Item item : inventory) {
            if (item.getName().equalsIgnoreCase(itemName)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 增加最大负重
     */
    public void increaseMaxWeight(double amount) {
        this.maxWeight += amount;
    }
}