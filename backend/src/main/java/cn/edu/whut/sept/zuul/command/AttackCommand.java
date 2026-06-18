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

        // 玩家攻击（使用状态修正后的攻击力）
        int dmg = Math.max(1, player.getEffectiveAttack());
        m.takeDamage(dmg);
        StringBuilder sb = new StringBuilder();
        sb.append("你对 ").append(m.getName()).append(" 造成了 ").append(dmg).append(" 点伤害。\n");

        if (!m.isAlive()) {
            // 记录怪物位置用于掉落
            int monsterX = 300; // 默认位置，稍后由前端覆盖
            int monsterY = 160;

            current.removeMonster(m);
            sb.append("你击败了 ").append(m.getName()).append("！");

            java.util.Random rnd = new java.util.Random();

            // 普通怪 100%掉落稀有装备（暗影披风、生命戒指、元素项链中的一种）
            if (m.getType() == Monster.TYPE_NORMAL) {
                String[] equipNames = {"暗影披风", "生命戒指", "元素项链"};
                String dropName = equipNames[rnd.nextInt(equipNames.length)];
                DroppedItem drop = new DroppedItem(dropName, monsterX, monsterY);
                current.addDroppedItem(drop);
                sb.append("\n✨ " + m.getName() + "掉落了传说级装备——" + dropName + "！✨");
            }

            // Boss 掉落药水（生命浆果或魔力浆果）
            if (m.getType() == Monster.TYPE_BOSS) {
                String dropName = rnd.nextBoolean() ? "生命浆果" : "魔力浆果";
                DroppedItem drop = new DroppedItem(dropName, monsterX, monsterY);
                current.addDroppedItem(drop);
                sb.append("\n" + m.getName() + "掉落了 " + dropName + "！");
            }

            // 根据怪物类型给予货币奖励（火焰史莱姆也开始爆炸时立即发放）
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
                sb.append("\n获得了 $").append(reward).append(" 货币！(余额: $").append(player.getMoney().getAmount()).append(")");
            }

            return GameResponse.success(sb.toString(), current.getFullInfo());
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
