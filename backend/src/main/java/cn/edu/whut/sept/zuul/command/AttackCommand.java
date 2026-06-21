package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 攻击命令（旧版命令行兼容）：attack <monsterName>
 * 委托给 {@link Game#attackMonster(String, int, int)} 完成伤害结算与掉落。
 * 前端新版攻击通过 POST /api/attack 走 GameService.performAttack()，
 * 但 AttackCommand 保留用于命令行调试和向后兼容。
 *
 * 死亡检查统一使用 {@link Player#checkAndMarkDeath(StringBuilder)}，
 * 不绑定任何特定负面状态——任一状态（流血/烧伤/中毒）使HP降至0即判死。
 */
public class AttackCommand implements Command {
    private final Game game;
    private String targetName;

    public AttackCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) {
            return GameResponse.error("玩家不存在");
        }

        Room current = game.getCurrentRoom();
        if (current == null) {
            return GameResponse.error("当前不在任何房间");
        }

        StringBuilder sb = new StringBuilder();

        // ---- 流血：每次主动攻击触发一次 ----
        if (player.getStatusManager() != null) {
            int bleedDmg = player.getStatusManager().tickBleedOnAttack();
            if (bleedDmg > 0) {
                sb.append("流血状态触发，受到 ").append(bleedDmg).append(" 点真实伤害！\n");
            }
        }

        // ---- 通用死亡检查（不绑定特定状态） ----
        if (player.checkAndMarkDeath(sb)) {
            game.setGameOver(true);  // 保险：确保 gameOver 一定被设置
            return GameResponse.success(sb.toString().trim(), current.getFullInfo());
        }

        // ---- 执行攻击 ----
        String result = game.attackMonster(targetName, 300, 160);

        // 先校验目标有效性（避免错误消息中混入流血文本）
        if (result.startsWith("要攻击谁") || result.startsWith("这里没有") || result.contains("无法被攻击")) {
            return GameResponse.error(result);
        }

        sb.append(result);
        return GameResponse.success(sb.toString().trim(), current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.targetName = params[0];
        }
    }
}
