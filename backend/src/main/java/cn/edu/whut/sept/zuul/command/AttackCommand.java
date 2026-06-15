package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 攻击命令：attack <monsterName>
 */
public class AttackCommand implements Command {
    private Game game;
    private String targetName;

    public AttackCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");
        Room current = game.getCurrentRoom();
        if (targetName == null || targetName.isBlank()) {
            return GameResponse.error("要攻击谁？请指定怪物名称，例如: attack goblin");
        }

        Monster m = current.getMonster(targetName);
        if (m == null) {
            return GameResponse.error("这里没有叫 '" + targetName + "' 的怪物。");
        }

        // 玩家先手
        int dmg = Math.max(1, player.getAttack());
        m.takeDamage(dmg);
        StringBuilder sb = new StringBuilder();
        sb.append("你对 ").append(m.getName()).append(" 造成了 ").append(dmg).append(" 点伤害。\n");

        if (!m.isAlive()) {
            current.removeMonster(m);
            sb.append("你击败了 ").append(m.getName()).append("！");
            return GameResponse.success(sb.toString(), current.getFullInfo());
        }

        // 怪物反击 — 使用怪物的实际攻击力
        int retaliate = m.getAttack();
        player.takeDamage(retaliate);
        sb.append(m.getName()).append(" 反击造成 ").append(retaliate).append(" 点伤害。\n");
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

