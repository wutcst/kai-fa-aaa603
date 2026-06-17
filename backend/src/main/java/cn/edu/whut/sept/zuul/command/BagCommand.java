package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;

import java.util.*;

/**
 * 背包/拾取命令：bag use <itemName> / bag unequip <itemName> / bag discard <itemName> / take <itemName>
 * 使用说明：
 * - 药水/消耗品：bag use = 使用消耗
 * - 饰品（披风/戒指/项链）：bag use = 佩戴（永久，同类只能佩戴一个）
 * - 已佩戴的饰品：bag unequip = 卸下
 * - bag discard = 丢弃
 * - take = 拾取房间物品
 */
public class BagCommand implements Command {
    private Game game;
    private String subCommand; // "use", "unequip", "discard", or "take"
    private String itemName;

    public BagCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (subCommand == null || itemName == null) {
            return buildInventoryResponse("请指定操作：bag use <物品名> / bag unequip <物品名> / bag discard <物品名> / take <物品名>");
        }

        Player player = game.getPlayer();

        // take 子命令：从当前房间拾取物品
        if ("take".equalsIgnoreCase(subCommand)) {
            return executeTake(player);
        }

        // use / unequip / discard 子命令：操作背包中的物品
        Bag bag = player.getBag();
        Item item = bag.getItem(itemName);

        if (item == null) {
            return buildInventoryResponse("背包中没有名为 '" + itemName + "' 的物品！");
        }

        switch (subCommand.toLowerCase()) {
            case "use":
                return executeUse(player, bag, item);
            case "unequip":
                return executeUnequip(player, bag, item);
            case "discard":
                return executeDiscard(bag, itemName);
            default:
                return buildInventoryResponse("未知的背包操作：" + subCommand);
        }
    }

    /**
     * 从当前房间拾取物品到背包
     */
    private GameResponse executeTake(Player player) {
        Room currentRoom = game.getCurrentRoom();
        Item item = currentRoom.getItem(itemName);

        if (item == null) {
            return GameResponse.error("这里没有名为 '" + itemName + "' 的物品！");
        }

        player.getBag().addItem(item);
        currentRoom.removeItem(item);
        return GameResponse.success("你拾取了 " + itemName + "。",
                currentRoom.getFullInfo());
    }

    /**
     * 使用/佩戴物品
     */
    private GameResponse executeUse(Player player, Bag bag, Item item) {
        String itemName = item.getName();
        String lowerName = itemName.toLowerCase();

        StringBuilder effectMsg = new StringBuilder();

        // ===== 饰品佩戴逻辑 =====
        String slot = Player.getItemSlot(itemName);
        if (slot != null) {
            // 检查是否已佩戴
            if (player.isEquipped(itemName)) {
                return buildInventoryResponse(itemName + " 已佩戴，如需卸下请使用「卸下」操作。");
            }

            // 检查同类饰品是否已佩戴
            String currentEquipped = player.getEquippedInSlot(slot);
            if (currentEquipped != null) {
                return buildInventoryResponse("同类饰品只能佩戴一个！当前已佩戴：" + currentEquipped + "，请先卸下。");
            }

            // 佩戴饰品（不消耗物品）
            String oldItem = player.equipItem(itemName);
            if (oldItem != null) {
                effectMsg.append("已卸下 ").append(oldItem).append("。");
            }
            effectMsg.append("你佩戴了 ").append(itemName).append("！属性已生效！");

            return buildInventoryResponse(effectMsg.toString());
        }

        // ===== 消耗品使用逻辑 =====
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

        // 从背包中移除一个该消耗品
        bag.useItem(itemName);

        return buildInventoryResponse("使用了 " + itemName + "。" + effectMsg.toString());
    }

    /**
     * 卸下饰品
     */
    private GameResponse executeUnequip(Player player, Bag bag, Item item) {
        String itemName = item.getName();

        if (Player.getItemSlot(itemName) == null) {
            return buildInventoryResponse(itemName + " 不是饰品，无法卸下。");
        }

        if (!player.isEquipped(itemName)) {
            return buildInventoryResponse(itemName + " 当前未佩戴。");
        }

        player.unequipItem(itemName);
        return buildInventoryResponse("已卸下 " + itemName + "，属性加成已移除。物品仍在背包中。");
    }

    /**
     * 丢弃物品（饰品丢弃前会先卸下）
     */
    private GameResponse executeDiscard(Bag bag, String itemName) {
        // 如果是已佩戴的饰品，先卸下
        if (bag.getOwner() != null) {
            String slot = Player.getItemSlot(itemName);
            if (slot != null && bag.getOwner().isEquipped(itemName)) {
                bag.getOwner().unequipItem(itemName);
            }
        }

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
            // "bag use <itemName>" 或 "bag take <itemName>"
            this.subCommand = params[0];
            // 物品名可能包含空格，将所有剩余参数拼接
            this.itemName = String.join(" ", Arrays.copyOfRange(params, 1, params.length));
        } else if (params.length == 1) {
            // "take <itemName>"
            this.subCommand = "take";
            this.itemName = params[0];
        }
    }
}
