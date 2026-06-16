package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Magic;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 月光波命令：长按 H 蓄力2秒后释放，消耗30魔力
 * 向玩家朝向方向发射金色月光波，仅对光波触碰到的敌人造成魔攻300%的魔法伤害
 * 命令格式: wave <monsterName>
 * 前端在光波运动过程中逐帧检测碰撞，对每个触碰到的怪物各发送一次此命令
 *
 * MP 消耗策略：短时间内（2秒窗口）的多次 wave 调用只消耗一次 MP
 */
public class WaveCommand implements Command {
    private Game game;
    private String targetName;

    // 共享时间戳：同一波光波中多次调用只扣一次 MP
    private static long lastWaveCastTime = 0;
    private static final long WAVE_WINDOW_MS = 2000;

    public WaveCommand(Game game) {
        this.game = game;
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
            // 火焰史莱姆：进入自爆倒计时，不立即移除
            if (Monster.SPECIAL_FLAME_SLIME.equals(m.getSpecialType())) {
                m.startExploding();
                sb.append("\n你击败了 ").append(m.getName()).append("！但它即将自爆，快远离！");
            } else {
                current.removeMonster(m);
                sb.append("\n你击败了 ").append(m.getName()).append("！");
            }

            // 根据怪物类型给予货币奖励
            int reward = 0;
            switch (m.getType()) {
                case Monster.TYPE_NORMAL:
                    reward = 15;
                    break;
                case Monster.TYPE_ELITE:
                    reward = 35;
                    break;
                case Monster.TYPE_BOSS:
                    reward = 100;
                    break;
            }
            if (reward > 0 && player.getMoney() != null) {
                player.getMoney().add(reward);
                sb.append("\n获得了 $").append(reward).append(" 货币！");
            }
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
