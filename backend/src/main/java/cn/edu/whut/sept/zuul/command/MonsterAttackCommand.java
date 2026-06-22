package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 怪物主动攻击命令：monsterattack <monsterName>
 * 由前端怪物AI调用，怪物主动对玩家造成伤害（不附带玩家反击效果）。
 * 与 AttackCommand（玩家攻击，怪物反击）区分开。
 *
 * 特殊怪物机制：
 * - 火焰史莱姆：接触攻击造成法术伤害 + 施加2层烧伤，1s冷却间隔
 * - 史莱姆：不主动攻击，触碰到玩家时造成攻击力100%物理伤害 + 施加迟缓
 * - 骷髅：攻击到玩家时施加2层中毒
 * - 狼人：攻击间隔0.6s，攻击范围35px，每次命中施加2层流血
 * - 食人魔：攻击间隔2s，每次攻击回复造成伤害50%的体力
 */
public class MonsterAttackCommand implements Command {
    /** 火焰史莱姆接触攻击冷却（毫秒） */
    private static final long FLAME_SLIME_ATTACK_COOLDOWN = 1000L;
    /** 史莱姆接触攻击冷却（毫秒） */
    private static final long SLIME_ATTACK_COOLDOWN = 1000L;

    private Game game;
    private String targetName;

    public MonsterAttackCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");
        Room current = game.getCurrentRoom();
        if (targetName == null || targetName.isBlank()) {
            return GameResponse.error("怪物名称不能为空");
        }

        Monster m = current.getMonster(targetName);
        if (m == null) {
            return GameResponse.error("这里没有叫 '" + targetName + "' 的怪物。");
        }

        if (!m.isAlive()) {
            return GameResponse.error("怪物 '" + targetName + "' 已经死亡。");
        }

        // ---- 玩家无敌帧：突刺期间怪物攻击无效 ----
        if (player.isInvincible()) {
            return GameResponse.success("", current.getFullInfo());
        }

        StringBuilder sb = new StringBuilder();
        String specialType = m.getSpecialType();

        // ---- 火焰史莱姆特殊攻击 ----
        if (Monster.SPECIAL_FLAME_SLIME.equals(specialType)) {
            // 接触攻击冷却检查
            long now = System.currentTimeMillis();
            long cooldown = m.getAttackCooldown() > 0 ? m.getAttackCooldown() : FLAME_SLIME_ATTACK_COOLDOWN;
            if (m.getLastContactAttackTime() > 0
                    && now - m.getLastContactAttackTime() < cooldown) {
                return GameResponse.error("火焰史莱姆攻击冷却中。");
            }
            m.setLastContactAttackTime(now);

            // 法术伤害 = 攻击力 × 100%
            int dmg = m.getAttack();
            player.takeMagicDamage(dmg);
            sb.append(m.getName()).append(" 的火焰接触对你造成了 ").append(dmg).append(" 点法术伤害。\n");

            // 施加2层烧伤
            player.getStatusManager().applyBurn(2);
            sb.append("你被施加了 2 层烧伤！（当前层数：").append(player.getStatusManager().getBurnLayers()).append("）\n");

        // ---- 普通史莱姆特殊攻击（不主动攻击，仅触碰时触发） ----
        } else if (Monster.SPECIAL_SLIME.equals(specialType)) {
            // 接触攻击冷却检查
            long now = System.currentTimeMillis();
            long cooldown = m.getAttackCooldown() > 0 ? m.getAttackCooldown() : SLIME_ATTACK_COOLDOWN;
            if (m.getLastContactAttackTime() > 0
                    && now - m.getLastContactAttackTime() < cooldown) {
                return GameResponse.error("史莱姆攻击冷却中。");
            }
            m.setLastContactAttackTime(now);

            // 物理伤害 = 攻击力 × 100%
            int dmg = m.getAttack();
            player.takeDamage(dmg);
            sb.append(m.getName()).append(" 的触碰对你造成了 ").append(dmg).append(" 点物理伤害。\n");

            // 施加迟缓
            player.getStatusManager().applySlow(1);
            sb.append("你被施加了迟缓！（移速减半，持续10秒）\n");

        // ---- 骷髅：施加2层中毒 ----
        } else if (isSkeleton(m)) {
            int dmg = m.getAttack();
            player.takeDamage(dmg);
            sb.append(m.getName()).append(" 对你造成了 ").append(dmg).append(" 点伤害。\n");

            player.getStatusManager().applyPoison(2);
            sb.append("你被施加了 2 层中毒！（当前层数：").append(player.getStatusManager().getPoisonLayers()).append("）\n");

        // ---- 狼人：施加2层流血 ----
        } else if (isWerewolf(m)) {
            int dmg = m.getAttack();
            player.takeDamage(dmg);
            sb.append(m.getName()).append(" 对你造成了 ").append(dmg).append(" 点伤害。\n");

            player.getStatusManager().applyBleed(2);
            sb.append("你被施加了 2 层流血！（当前层数：").append(player.getStatusManager().getBleedLayers()).append("）\n");

        // ---- 食人魔：吸血50% ----
        } else if (m.getLifeStealRatio() > 0) {
            int dmg = m.getAttack();
            player.takeDamage(dmg);
            sb.append(m.getName()).append(" 对你造成了 ").append(dmg).append(" 点伤害。\n");

            int healAmount = (int) Math.round(dmg * m.getLifeStealRatio());
            if (healAmount > 0) {
                m.heal(healAmount);
                sb.append(m.getName()).append(" 回复了 ").append(healAmount).append(" 点生命！（HP: ")
                  .append(m.getHp()).append("/").append(m.getMaxHp()).append("）\n");
            }

        } else {
            // 普通怪物攻击 — 物理伤害
            int dmg = m.getAttack();
            player.takeDamage(dmg);
            sb.append(m.getName()).append(" 对你造成了 ").append(dmg).append(" 点伤害。\n");
        }

        sb.append("你当前生命：").append(player.getHp()).append("。\n");

        if (!player.isAlive()) {
            game.setGameOver(true);
            sb.append("你被 ").append(m.getName()).append(" 击败，游戏结束。");
            return GameResponse.error(sb.toString());
        }

        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    /** 判断怪物名称是否包含"骷髅" */
    private boolean isSkeleton(Monster m) {
        if (m.getName() == null) return false;
        return m.getName().contains("骷髅");
    }

    /** 判断怪物名称是否包含"狼人" */
    private boolean isWerewolf(Monster m) {
        if (m.getName() == null) return false;
        return m.getName().contains("狼人");
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.targetName = params[0];
        }
    }
}
