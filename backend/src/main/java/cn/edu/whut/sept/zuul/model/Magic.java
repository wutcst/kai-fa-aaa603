package cn.edu.whut.sept.zuul.model;

/**
 * 玩家技能类（Magic）：集中管理所有玩家技能相关的逻辑。
 * 包括技能定义、魔力消耗检查、魔法伤害计算、对怪物施加魔法伤害等。
 * 原先分散在 Monster 类中的魔法相关函数统一迁移至此。
 */
public class Magic {

    /**
     * 技能枚举：定义所有玩家可用的技能及其属性
     */
    public enum Skill {
        /** 月光波：消耗30魔力，造成魔攻300%的魔法伤害 */
        MOONLIGHT_WAVE("月光波", 30, 3.0);

        private final String displayName;
        private final int mpCost;
        private final double damageMultiplier;

        Skill(String displayName, int mpCost, double damageMultiplier) {
            this.displayName = displayName;
            this.mpCost = mpCost;
            this.damageMultiplier = damageMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMpCost() {
            return mpCost;
        }

        public double getDamageMultiplier() {
            return damageMultiplier;
        }
    }

    /**
     * 检查玩家是否有足够魔力释放指定技能
     *
     * @param player 玩家
     * @param skill  要释放的技能
     * @return true 表示魔力足够
     */
    public static boolean canCast(Player player, Skill skill) {
        return player != null && player.getMp() >= skill.getMpCost();
    }

    /**
     * 计算技能造成的魔法伤害（基于玩家魔攻 × 技能倍率）
     *
     * @param player 玩家
     * @param skill  使用的技能
     * @return 魔法伤害值
     */
    public static int calcMagicDamage(Player player, Skill skill) {
        return (int) (player.getMagicAttack() * skill.getDamageMultiplier());
    }

    /**
     * 对怪物施加魔法伤害（怪物无魔抗，直接等同于物理扣血）
     *
     * @param monster 目标怪物
     * @param damage  魔法伤害值
     */
    public static void dealMagicDamage(Monster monster, int damage) {
        if (monster != null && damage > 0) {
            monster.takeDamage(damage);
        }
    }

    /**
     * 释放技能（一站式方法）：消耗魔力 → 计算伤害 → 施加伤害
     *
     * @param player  玩家
     * @param monster 目标怪物
     * @param skill   要释放的技能
     * @return 造成的实际伤害值；-1 表示魔力不足
     */
    public static int cast(Player player, Monster monster, Skill skill) {
        if (!canCast(player, skill)) {
            return -1;
        }
        player.consumeMp(skill.getMpCost());
        int damage = calcMagicDamage(player, skill);
        dealMagicDamage(monster, damage);
        return damage;
    }
}
