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
 */
public class MonsterAttackCommand implements Command {
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

        // 怪物主动攻击玩家 — 直接扣除生命值（无视玩家防御）
        // 原因：怪物攻击绕过防御计算，确保对玩家造成实质威胁（防御已体现在玩家抗怪能力上）
        int dmg = m.getAttack();
        player.setHp(Math.max(0, player.getHp() - dmg));
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName()).append(" 对你造成了 ").append(dmg).append(" 点伤害（防御无效）。\n");
        sb.append("你当前生命：").append(player.getHp()).append("。\n");

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
