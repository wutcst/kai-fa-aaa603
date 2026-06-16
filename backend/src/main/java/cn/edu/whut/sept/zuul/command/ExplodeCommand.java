package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 爆炸伤害命令：explode <monsterName> [_nodmg]
 * 由前端检测到 explodeTriggeredMonsters 后发送。
 * 默认造成伤害；附加参数 _nodmg 表示玩家不在范围内，仅移除怪物不造成伤害。
 * 伤害 = 怪物攻击力 × 200%（法术伤害），施加5层烧伤。
 */
public class ExplodeCommand implements Command {
    private Game game;
    private String targetName;

    public ExplodeCommand(Game game) {
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
            // 怪物可能已被移除（重复调用），静默返回
            return GameResponse.success("", current.getFullInfo());
        }

        if (!m.isExploding()) {
            return GameResponse.error("怪物 '" + targetName + "' 未在自爆状态。");
        }

        // 移除怪物
        current.removeMonster(m);

        // _nodmg 标志：玩家不在爆炸范围内，仅移除不造成伤害
        if (_noDmg) {
            return GameResponse.success(m.getName() + " 的自爆未波及到你。", current.getFullInfo());
        }

        // 执行爆炸伤害
        int explodeDmg = (int)(m.getAttack() * 2.0); // 200%攻击力法术伤害
        player.takeMagicDamage(explodeDmg);
        player.getStatusManager().applyBurn(5);

        StringBuilder sb = new StringBuilder();
        sb.append(m.getName()).append(" 自爆对你造成了 ").append(explodeDmg).append(" 点法术伤害并施加5层烧伤！");
        sb.append("\n你当前生命：").append(player.getHp()).append("。");

        if (!player.isAlive()) {
            game.setGameOver(true);
            sb.append("\n你被 ").append(m.getName()).append(" 的自爆击败，游戏结束。");
            return GameResponse.error(sb.toString());
        }

        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    private boolean _noDmg = false;

    @Override
    public void setParams(String... params) {
        _noDmg = false;
        if (params != null && params.length > 0) {
            this.targetName = params[0];
            // 检查 _nodmg 标记
            for (int i = 1; i < params.length; i++) {
                if ("_nodmg".equals(params[i])) {
                    _noDmg = true;
                    break;
                }
            }
        }
    }
}
