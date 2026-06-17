package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.DroppedItem;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 攻击命令：attack <monsterName>
 * 玩家主动攻击指定的怪物。
 * 注意：怪物反击是独立的，由前端的怪物AI通过 MonsterAttackCommand 触发，
 * 与玩家是否攻击无关。因此本命令仅处理玩家对怪物的伤害，不附带怪物反击。
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

        // 爆炸倒计时中的怪物不可被攻击
        if (m.isExploding()) {
            return GameResponse.error(m.getName() + " 正在自爆倒计时中，无法被攻击。");
        }

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
