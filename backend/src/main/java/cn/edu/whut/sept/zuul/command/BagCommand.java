package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;

import java.util.*;

/**
 * 背包命令：bag use <itemName> / bag discard <itemName>
 * 用于背包UI中的使用/丢弃操作
 */
public class BagCommand implements Command {
    private Game game;
    private String subCommand; // "use" or "discard"
    private String itemName;

    public BagCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (subCommand == null || itemName == null) {
            return buildInventoryResponse("请指定操作：bag use <物品名> 或 bag discard <物品名>");
        }

        Player player = game.getPlayer();
        Bag bag = player.getBag();
        Item item = bag.getItem(itemName);

        if (item == null) {
            return buildInventoryResponse("背包中没有名为 '" + itemName + "' 的物品！");
        }

        switch (subCommand.toLowerCase()) {
            case "use":
                return executeUse(player, bag, item);
            case "discard":
                return executeDiscard(bag, itemName);
            default:
                return buildInventoryResponse("未知的背包操作：" + subCommand);
        }
    }

    private GameResponse executeUse(Player player, Bag bag, Item item) {
        String itemName = item.getName();
        String lowerName = itemName.toLowerCase();

        // 使用物品效果
        StringBuilder effectMsg = new StringBuilder();

        if (lowerName.contains("生命浆果")) {
            player.restoreHp(25);
            effectMsg.append("你恢复了 25 点生命！");
        } else if (lowerName.contains("魔力浆果")) {
            player.restoreMp(25);
            effectMsg.append("你恢复了 25 点魔力！");
        } else if (lowerName.contains("cookie") || lowerName.contains("饼干")) {
            player.setMaxHp(player.getMaxHp() + 10);
            player.restoreHp(10);
            effectMsg.append("你的最大生命值提升了 10 点！");
        } else if (lowerName.contains("berry") || lowerName.contains("浆果") || lowerName.contains("potion") || lowerName.contains("药水")) {
            player.restoreHp(20);
            player.restoreMp(30);
            effectMsg.append("你恢复了 20 点生命和 30 点魔力！");
        } else if (lowerName.contains("sword") || lowerName.contains("剑")) {
            player.setAttack(player.getAttack() + 15);
            effectMsg.append("你的物理攻击力 +15！");
        } else if (lowerName.contains("shield") || lowerName.contains("盾")) {
            player.setDefense(player.getDefense() + 10);
            effectMsg.append("你的物理防御力 +10！");
        } else {
            player.restoreHp(10);
            effectMsg.append("你恢复了 10 点生命！");
        }

        // 从背包中移除一个该物品
        bag.useItem(itemName);

        return buildInventoryResponse("使用了 " + itemName + "。" + effectMsg.toString());
    }

    private GameResponse executeDiscard(Bag bag, String itemName) {
        int count = bag.discardAllItem(itemName);
        if (count > 0) {
            return buildInventoryResponse("丢弃了 " + count + " 个 " + itemName + "。");
        }
        return buildInventoryResponse("背包中没有名为 '" + itemName + "' 的物品！");
    }

    /**
     * 构建包含背包数据的响应
     */
    private GameResponse buildInventoryResponse(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);

        // 注入背包数据
        if (game.getPlayer() != null) {
            List<InventoryItem> backpack = game.getPlayer().getBag().getBackpackItems();
            data.put("backpack", backpack);
        }

        // 注入当前房间信息
        if (game.getCurrentRoom() != null) {
            data.putAll(game.getCurrentRoom().getFullInfo());
        }

        return GameResponse.success(message, data);
    }

    @Override
    public void setParams(String... params) {
        if (params.length >= 2) {
            this.subCommand = params[0];
            // 物品名可能包含空格，将所有剩余参数拼接
            this.itemName = String.join(" ", Arrays.copyOfRange(params, 1, params.length));
        } else if (params.length == 1) {
            this.subCommand = params[0];
        }
    }
}
