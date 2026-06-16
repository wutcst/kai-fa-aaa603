package cn.edu.whut.sept.zuul.model;

import lombok.Data;

/**
 * 怪物模型：包含名字、描述、生命值、攻击力、防御、魔抗、移速和分类
 */
@Data
public class Monster {
    /** 普通怪物 */
    public static final int TYPE_NORMAL = 0;
    /** 精英怪物 */
    public static final int TYPE_ELITE = 1;
    /** Boss怪物 */
    public static final int TYPE_BOSS = 2;

    private String name;
    private String description;
    private int hp;
    private int attack;
    /** 物理防御 */
    private int defense;
    /** 魔法抗性 */
    private int magicResist;
    /** 移动速度 */
    private int speed;
    /** 怪物类型：0=普通, 1=精英, 2=Boss */
    private int type;

    public Monster(String name, String description, int hp, int attack) {
        this(name, description, hp, attack, 0, 0, 100, TYPE_NORMAL);
    }

    public Monster(String name, String description, int hp, int attack, int type) {
        this(name, description, hp, attack, 0, 0, 100, type);
    }

    public Monster(String name, String description, int hp, int attack,
                   int defense, int magicResist, int speed, int type) {
        this.name = name;
        this.description = description;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.magicResist = magicResist;
        this.speed = speed;
        this.type = type;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - dmg);
    }

}

