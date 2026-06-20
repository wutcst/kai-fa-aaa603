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
 * 特殊怪物：火焰史莱姆 — 接触攻击造成法术伤害 + 施加2层烧伤，1s冷却间隔。
 */
public class MonsterAttackCommand implements Command {
    /** 火焰史莱姆接触攻击冷却（毫秒） */
    private static final long FLAME_SLIME_ATTACK_COOLDOWN = 1000L;

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

        StringBuilder sb = new StringBuilder();

        // 判断是否为火焰史莱姆的特殊攻击
        if (Monster.SPECIAL_FLAME_SLIME.equals(m.getSpecialType())) {
            // 接触攻击冷却检查
            long now = System.currentTimeMillis();
            if (m.getLastContactAttackTime() > 0
                    && now - m.getLastContactAttackTime() < FLAME_SLIME_ATTACK_COOLDOWN) {
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

            sb.append("你当前生命：").append(player.getHp()).append("。\n");
        } else {
            // 普通怪物攻击 — 物理伤害
            int dmg = m.getAttack();
            player.takeDamage(dmg);
            sb.append(m.getName()).append(" 对你造成了 ").append(dmg).append(" 点伤害。\n");
            sb.append("你当前生命：").append(player.getHp()).append("。\n");
        }

        if (!player.isAlive()) {
            game.setGameOver(true);
            sb.append("你被 ").append(m.getName()).append(" 击败，游戏结束。");
            return GameResponse.error(sb.toString());
        }

        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.targetName = params[0];
        }
    }
}
