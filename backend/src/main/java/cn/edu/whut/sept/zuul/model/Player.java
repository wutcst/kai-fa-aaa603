package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 玩家类：管理玩家状态、战斗属性和货币
 */
@Getter
@Setter
public class Player {
    private String name;
    private Room currentRoom;
    private Bag bag;
    private Money money;

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
        this.bag = new Bag();
        this.money = new Money();

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
}