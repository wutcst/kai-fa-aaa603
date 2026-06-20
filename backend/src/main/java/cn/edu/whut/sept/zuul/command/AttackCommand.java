package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 攻击命令（旧版命令行兼容）：attack <monsterName>
 * 委托给 {@link Game#attackMonster(String, int, int)} 完成伤害结算与掉落。
 * 前端新版攻击通过 POST /api/attack 走 GameService.performAttack()，
 * 但 AttackCommand 保留用于命令行调试和向后兼容。
 */
public class AttackCommand implements Command {
    private final Game game;
    private String targetName;

    public AttackCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        // ---- 流血：每次主动攻击触发一次（命令行兼容） ----
        StringBuilder fullResult = new StringBuilder();
        if (game.getPlayer() != null && game.getPlayer().getStatusManager() != null) {
            int bleedDmg = game.getPlayer().getStatusManager().tickBleedOnAttack();
            if (bleedDmg > 0) {
                fullResult.append("流血状态触发，受到 ").append(bleedDmg).append(" 点真实伤害！\n");
            }
        }

        // ---- 如果玩家因流血死亡，立即返回死亡结果 ----
        if (game.getPlayer() != null && !game.getPlayer().isAlive()) {
            game.setGameOver(true);
            fullResult.append("你因流血过多而死亡！");
            Room current = game.getCurrentRoom();
            if (current != null) {
                return GameResponse.success(fullResult.toString().trim(), current.getFullInfo());
            }
            return GameResponse.error(fullResult.toString().trim());
        }

        String result = game.attackMonster(targetName, 300, 160);
        fullResult.append(result);

        Room current = game.getCurrentRoom();
        if (current == null) {
            return GameResponse.error("当前不在任何房间");
        }
        // attackMonster 返回纯文本；若以"要攻击谁"/"这里没有"/"正在自爆"开头则为错误
        if (result.startsWith("要攻击谁") || result.startsWith("这里没有") || result.contains("无法被攻击")) {
            return GameResponse.error(fullResult.toString().trim());
        }
        return GameResponse.success(fullResult.toString().trim(), current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.targetName = params[0];
        }
    }
}
