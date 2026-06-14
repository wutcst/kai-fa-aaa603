package cn.edu.whut.sept.zuul.model;

import lombok.Data;

/**
 * 简单的怪物模型：包含名字、描述、生命值和攻击力
 */
@Data
public class Monster {
    private String name;
    private String description;
    private int hp;
    private int attack;

    public Monster(String name, String description, int hp, int attack) {
        this.name = name;
        this.description = description;
        this.hp = hp;
        this.attack = attack;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - dmg);
    }
}

