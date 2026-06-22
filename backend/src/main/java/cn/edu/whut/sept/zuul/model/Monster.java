package cn.edu.whut.sept.zuul.model;

import lombok.Data;

/**
 * 怪物模型：包含名字、描述、生命值、攻击力、防御、魔抗、移速和分类。
 * 支持特殊怪物类型（如火焰史莱姆、普通史莱姆）的接触攻击机制，
 * 以及骷髅中毒、狼人流血、食人魔吸血等特殊攻击效果。
 */
@Data
public class Monster {
    /** 普通怪物 */
    public static final int TYPE_NORMAL = 0;
    /** 精英怪物 */
    public static final int TYPE_ELITE = 1;
    /** Boss怪物 */
    public static final int TYPE_BOSS = 2;

    /** 特殊怪物类型：火焰史莱姆 */
    public static final String SPECIAL_FLAME_SLIME = "FLAME_SLIME";
    /** 特殊怪物类型：普通史莱姆（不主动攻击，接触造成物理伤害+迟缓） */
    public static final String SPECIAL_SLIME = "SLIME";

    private String name;
    private String description;
    private int hp;
    private int maxHp;
    private int attack;
    /** 物理防御 */
    private int defense;
    /** 魔法抗性 */
    private int magicResist;
    /** 移动速度 */
    private int speed;
    /** 怪物类型：0=普通, 1=精英, 2=Boss */
    private int type;

    // ===== 怪物位置字段（掉落坐标判定用） =====
    /** 怪物当前 X 坐标（像素），前端同步 */
    private int x;
    /** 怪物当前 Y 坐标（像素），前端同步 */
    private int y;

    // ===== 特殊怪物机制字段 =====

    /** 特殊怪物类型标识，null 表示普通怪物 */
    private String specialType;
    /** 上次接触攻击时间戳（毫秒），用于冷却间隔 */
    private long lastContactAttackTime;
    /** 个体攻击冷却时间（毫秒），0 表示使用默认值 */
    private long attackCooldown;
    /** 个体攻击范围（像素），0 表示使用默认值 */
    private int attackRange;
    /** 吸血比例（0.0~1.0），攻击回复 = 造成伤害 × lifeStealRatio */
    private double lifeStealRatio;

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
        this.maxHp = hp;
        this.attack = attack;
        this.defense = defense;
        this.magicResist = magicResist;
        this.speed = speed;
        this.type = type;
        this.specialType = null;
        this.lastContactAttackTime = 0;
        this.attackCooldown = 0;
        this.attackRange = 0;
        this.lifeStealRatio = 0.0;
    }

    // ==================== 普通怪物工厂方法 ====================

    /**
     * 创建哥布林：HP 90-110，攻击 15-20，防御 5，魔抗 0，移速 100。
     */
    public static Monster createGoblin(String suffix) {
        int hp = 90 + (int)(Math.random() * 21);   // 90-110
        int atk = 15 + (int)(Math.random() * 6);    // 15-20
        return new Monster(
            "哥布林#" + suffix,
            "一个矮小狡猾的哥布林",
            hp, atk, 5, 0, 100, TYPE_NORMAL
        );
    }

    /**
     * 创建史莱姆：HP 130-140，攻击 12-13，防御 10，魔抗 25，移速 65。
     * 不主动攻击，触碰到玩家时造成攻击力100%的物理伤害并施加迟缓。
     */
    public static Monster createSlime(String suffix) {
        int hp = 130 + (int)(Math.random() * 11);   // 130-140
        int atk = 12 + (int)(Math.random() * 2);     // 12-13
        Monster m = new Monster(
            "史莱姆#" + suffix,
            "一团黏糊糊的史莱姆，不会主动攻击，但触碰会让人行动迟缓",
            hp, atk, 10, 25, 65, TYPE_NORMAL
        );
        m.specialType = SPECIAL_SLIME;
        return m;
    }

    /**
     * 创建骷髅：HP 60-80，攻击 22-28，防御 0，魔抗 0，移速 125。
     * 攻击到玩家时施加2层中毒。
     */
    public static Monster createSkeleton(String suffix) {
        int hp = 60 + (int)(Math.random() * 21);     // 60-80
        int atk = 22 + (int)(Math.random() * 7);      // 22-28
        return new Monster(
            "骷髅#" + suffix,
            "一具复生的骷髅，攻击带毒",
            hp, atk, 0, 0, 125, TYPE_NORMAL
        );
    }

    /**
     * 创建狼人：HP 80-100，攻击 15-18，防御 0，魔抗 50，移速 100。
     * 攻击间隔0.6s，攻击范围35px，每次命中施加2层流血。
     */
    public static Monster createWerewolf(String suffix) {
        int hp = 80 + (int)(Math.random() * 21);      // 80-100
        int atk = 15 + (int)(Math.random() * 4);       // 15-18
        Monster m = new Monster(
            "狼人#" + suffix,
            "一只凶猛的狼人，攻击迅速且会造成流血",
            hp, atk, 0, 50, 100, TYPE_NORMAL
        );
        m.attackCooldown = 600;   // 0.6s
        m.attackRange = 35;       // 35px
        return m;
    }

    /**
     * 创建食人魔：HP 110-130，攻击 18-22，防御 5，魔抗 25，移速 80。
     * 攻击间隔2s，每次攻击回复造成伤害50%的体力。
     */
    public static Monster createOgre(String suffix) {
        int hp = 110 + (int)(Math.random() * 21);     // 110-130
        int atk = 18 + (int)(Math.random() * 5);       // 18-22
        Monster m = new Monster(
            "食人魔#" + suffix,
            "一个巨大的食人魔，缓慢但能吸血回复",
            hp, atk, 5, 25, 80, TYPE_NORMAL
        );
        m.attackCooldown = 2000;  // 2s
        m.lifeStealRatio = 0.5;   // 50% 吸血
        return m;
    }

    /**
     * 创建火焰史莱姆的静态工厂方法。
     * 血量 65-85，攻击 15，物防 0，法抗 20，移速 60。
     */
    public static Monster createFlameSlime(String nameSuffix) {
        int hp = 65 + (int)(Math.random() * 21); // 65-85
        Monster m = new Monster(
            "火焰史莱姆#" + nameSuffix,
            "一只燃烧着火焰的史莱姆，死后会自爆并造成大范围烧伤",
            hp, 15, 0, 20, 60, TYPE_NORMAL
        );
        m.specialType = SPECIAL_FLAME_SLIME;
        return m;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - dmg);
    }

    /** 恢复生命值 */
    public void heal(int amount) {
        this.hp = Math.min(this.maxHp, this.hp + amount);
    }

}

