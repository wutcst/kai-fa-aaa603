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
        String result = game.attackMonster(targetName, 300, 160);
        Room current = game.getCurrentRoom();
        if (current == null) {
            return GameResponse.error("当前不在任何房间");
        }
        // attackMonster 返回纯文本；若以"要攻击谁"/"这里没有"/"正在自爆"开头则为错误
        if (result.startsWith("要攻击谁") || result.startsWith("这里没有") || result.contains("无法被攻击")) {
            return GameResponse.error(result);
        }
        return GameResponse.success(result, current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.targetName = params[0];
        }
    }
}
