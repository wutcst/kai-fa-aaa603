package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Magic;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 月光波命令：长按 H 蓄力1秒后释放，消耗20魔力
 * 向玩家朝向方向发射金色月光波，仅对光波触碰到的敌人造成魔攻300%的魔法伤害
 * 命令格式: wave <monsterName>
 * 前端在光波运动过程中逐帧检测碰撞，对每个触碰到的怪物各发送一次此命令
 *
 * MP 消耗策略：短时间内（2秒窗口）的多次 wave 调用只消耗一次 MP
 */
public class WaveCommand implements Command {
    private Game game;
    private String targetName;
    private int targetX;
    private int targetY;

    // 共享时间戳：同一波光波中多次调用只扣一次 MP
    private static long lastWaveCastTime = 0;
    private static final long WAVE_WINDOW_MS = 2000;

    public WaveCommand(Game game) {
        this.game = game;
        this.targetX = 0;
        this.targetY = 0;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");

        long now = System.currentTimeMillis();
        boolean isNewWave = (now - lastWaveCastTime > WAVE_WINDOW_MS);

        // 仅在新光波时检查并立即消耗魔力（避免 wave _init 空调用跳过扣魔）
        if (isNewWave) {
            if (!Magic.canCast(player, Magic.Skill.MOONLIGHT_WAVE)) {
                return GameResponse.error("魔力不足" + Magic.Skill.MOONLIGHT_WAVE.getMpCost() + "，无法释放月光波！");
            }
            player.consumeMp(Magic.Skill.MOONLIGHT_WAVE.getMpCost());
            lastWaveCastTime = now;
        }

        Room current = game.getCurrentRoom();

        if (targetName == null || targetName.isBlank()) {
            return GameResponse.success("月光波划过空气。", current.getFullInfo());
        }

        Monster m = current.getMonster(targetName);
        if (m == null) {
            return GameResponse.success("月光波划过空气。", current.getFullInfo());
        }

        // 使用 Magic 类计算并施加魔法伤害
        int magicDmg = Magic.calcMagicDamage(player, Magic.Skill.MOONLIGHT_WAVE);
        Magic.dealMagicDamage(m, magicDmg);

        StringBuilder sb = new StringBuilder();
        sb.append("月光波命中了 ").append(m.getName()).append("，造成 ").append(magicDmg).append(" 点法术伤害。");

        if (!m.isAlive()) {
            // 同步怪物坐标后再处理掉落
            m.setX(targetX);
            m.setY(targetY);
            current.removeMonster(m);
            sb.append("\n你击败了 ").append(m.getName()).append("！");

            // 统一处理掉落与货币奖励（从怪物自身坐标读取掉落位置）
            sb.append(game.processMonsterDrop(m));
        }

        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.targetName = params[0];
            // 解析可选坐标参数：wave <monsterName> <x> <y>
            if (params.length >= 2) {
                try { this.targetX = Integer.parseInt(params[1]); } catch (NumberFormatException e) { /* ignore */ }
            }
            if (params.length >= 3) {
                try { this.targetY = Integer.parseInt(params[2]); } catch (NumberFormatException e) { /* ignore */ }
            }
        }
    }
}
