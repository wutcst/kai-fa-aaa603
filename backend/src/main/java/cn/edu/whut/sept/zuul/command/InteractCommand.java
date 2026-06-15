package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;

/**
 * 交互命令：interact <altarType>
 * 用于在篝火房间中与祭坛交互
 * 参数: heal / train / wisdom
 */
public class InteractCommand implements Command {
    private Game game;
    private String altarTypeStr;

    public InteractCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");

        Room current = game.getCurrentRoom();
        if (current.getRoomType() != RoomType.CAMPFIRE) {
            return GameResponse.error("当前房间没有祭坛，你只有在篝火房间才能与祭坛交互。");
        }

        if (altarTypeStr == null || altarTypeStr.isBlank()) {
            return GameResponse.error("请指定要交互的祭坛类型：heal（回复）、train（锻炼）、wisdom（博学）");
        }

        AltarType altarType;
        try {
            altarType = AltarType.valueOf(altarTypeStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return GameResponse.error("未知的祭坛类型：" + altarTypeStr + "。可选：heal、train、wisdom");
        }

        if (current.isAltarUsed()) {
            return GameResponse.error("这个房间的祭坛已经被使用过了，无法再次交互。");
        }

        // 初始化祭坛（如果尚未初始化）
        if (current.getAltars() == null || current.getAltars().isEmpty()) {
            current.initAltars();
        }

        Altar altar = current.activateAltar(altarType);
        if (altar == null) {
            return GameResponse.error("该祭坛无法被激活（可能已被使用）。");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你激活了").append(altar.getType().getDisplayName()).append("！\n");

        switch (altarType) {
            case HEAL:
                // 回复最大HP和MP的50%
                int healHp = (int) Math.ceil(player.getMaxHp() * 0.5);
                int healMp = (int) Math.ceil(player.getMaxMp() * 0.5);
                player.restoreHp(healHp);
                player.restoreMp(healMp);
                sb.append("祭坛散发出淡绿色的光芒，你感到体力恢复！\n");
                sb.append("恢复了 ").append(healHp).append(" 点生命值和 ").append(healMp).append(" 点魔力值。");
                break;
            case TRAIN:
                // 攻击、魔攻、防御 +25%（向上取整），魔抗 +10
                int atkBonus = (int) Math.ceil(player.getAttack() * 0.25);
                int matkBonus = (int) Math.ceil(player.getMagicAttack() * 0.25);
                int defBonus = (int) Math.ceil(player.getDefense() * 0.25);
                player.setAttack(player.getAttack() + atkBonus);
                player.setMagicAttack(player.getMagicAttack() + matkBonus);
                player.setDefense(player.getDefense() + defBonus);
                player.setMagicResist(player.getMagicResist() + 10);
                sb.append("祭坛散发出橙金色的光芒，你的身体变得更加强韧！\n");
                sb.append("攻击 +").append(atkBonus).append("，魔攻 +").append(matkBonus)
                        .append("，防御 +").append(defBonus).append("，魔抗 +10。");
                break;
            case WISDOM:
                // 博学祭坛：返回选项列表（前端展示三个空选项）
                sb.append("祭坛散发出蓝色的光芒，请从以下增益中选择一项...");
                break;
        }

        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.altarTypeStr = params[0];
        }
    }
}
