package cn.edu.whut.sept.zuul.model;

import lombok.Data;

/**
 * 怪物模型：包含名字、描述、生命值、攻击力、防御、魔抗、移速和分类。
 * 支持特殊怪物类型（如火焰史莱姆）的自爆机制。
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

    // ===== 特殊怪物机制字段 =====

    /** 特殊怪物类型标识，null 表示普通怪物 */
    private String specialType;
    /** 是否处于自爆倒计时状态（死亡后暂不消失） */
    private boolean exploding;
    /** 自爆倒计时是否已到期并已通知前端（防重复） */
    private boolean explodeNotified;
    /** 自爆开始时间戳（毫秒），用于后端结算 */
    private long explodeStartTime;
    /** 自爆判定范围半径（像素），前端渲染和后端结算共用 */
    private int explodeRange;
    /** 上次接触攻击时间戳（毫秒），用于1s冷却间隔 */
    private long lastContactAttackTime;

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
        this.exploding = false;
        this.explodeStartTime = 0;
        this.explodeRange = 0;
        this.lastContactAttackTime = 0;
    }

    /**
     * 创建火焰史莱姆的静态工厂方法。
     * 血量 65-85，攻击 15，物防 0，法抗 20，移速 60，自爆范围 80px。
     */
    public static Monster createFlameSlime(String nameSuffix) {
        int hp = 65 + (int)(Math.random() * 21); // 65-85
        Monster m = new Monster(
            "火焰史莱姆#" + nameSuffix,
            "一只燃烧着火焰的史莱姆，死后会自爆并造成大范围烧伤",
            hp, 15, 0, 20, 60, TYPE_NORMAL
        );
        m.specialType = SPECIAL_FLAME_SLIME;
        m.explodeRange = 80;
        return m;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    /** 是否处于活跃状态（存活且未在爆炸倒计时中） */
    public boolean isActive() {
        return this.hp > 0 && !this.exploding;
    }

    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - dmg);
    }

    /**
     * 进入自爆倒计时状态。
     * 调用后怪物停止移动/攻击，3秒后触发爆炸伤害结算。
     * 如果怪物已在自爆状态，不重置计时器（防止重复调用导致倒计时无限延长）。
     */
    public void startExploding() {
        if (this.exploding) {
            return; // 已在自爆中，不重置计时器
        }
        this.exploding = true;
        this.explodeStartTime = System.currentTimeMillis();
    }

}

