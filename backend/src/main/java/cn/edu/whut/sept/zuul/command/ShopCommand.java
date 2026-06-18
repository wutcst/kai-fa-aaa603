package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;

/**
 * 商店命令：shop buy <itemName> — 购买指定商品
 *          shop sell <itemName> — 出售背包中的物品
 * 仅在商店房间中可用，NPC 已经存在。
 */
public class ShopCommand implements Command {
    private Game game;
    private String subCommand; // buy / sell
    private String itemName;

    public ShopCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");

        Room current = game.getCurrentRoom();
        if (current.getRoomType() != RoomType.SHOP) {
            return GameResponse.error("当前房间不是商店，没有商人。");
        }

        // 确保商店已初始化
        current.initShopItems();

        if (subCommand == null || subCommand.isBlank()) {
            return GameResponse.error("用法：shop buy <商品名> / shop sell <物品名>");
        }

        if ("buy".equalsIgnoreCase(subCommand)) {
            return executeBuy(player, current);
        } else if ("sell".equalsIgnoreCase(subCommand)) {
            return executeSell(player);
        }

        return GameResponse.error("未知的商店子命令：" + subCommand + "。可用子命令：buy, sell");
    }

    /**
     * 购买商品
     */
    private GameResponse executeBuy(Player player, Room current) {
        if (itemName == null || itemName.isBlank()) {
            return GameResponse.error("请指定要购买的商品名称，如：shop buy 生命浆果");
        }

        ShopItem shopItem = current.getShopItem(itemName);
        if (shopItem == null) {
            return GameResponse.error("商店中没有名为 " + itemName + " 的商品。");
        }
        if (shopItem.isSold()) {
            return GameResponse.error(itemName + " 已经售罄。");
        }

        int price = shopItem.getPrice();
        Money money = player.getMoney();
        if (money == null) {
            return GameResponse.error("货币系统异常。");
        }
        if (!money.hasEnough(price)) {
            return GameResponse.error("你的货币不足！需要 $" + price + "，当前拥有 $" + money.getAmount() + "。");
        }

        // 扣除货币
        if (!money.spend(price)) {
            return GameResponse.error("购买失败：扣除货币时出错。");
        }

        // 标记售出
        shopItem.setSold(true);

        // 从注册表获取真正的物品实例，放入背包
        Item item = ItemRegistry.getItem(itemName);
        if (item == null) {
            item = new Item(itemName, "从商店购买的" + itemName, price);
        }
        player.getBag().addItem(item);

        return GameResponse.success("你成功购买了 " + itemName + "，花费 $" + price + "。剩余 $" + money.getAmount() + "。",
                current.getFullInfo());
    }

    /**
     * 出售背包中的物品
     */
    private GameResponse executeSell(Player player) {
        if (itemName == null || itemName.isBlank()) {
            return GameResponse.error("请指定要出售的物品名称，如：shop sell 生命浆果");
        }

        Bag bag = player.getBag();
        Item item = bag.getItem(itemName);
        if (item == null) {
            return GameResponse.error("背包中没有名为 " + itemName + " 的物品。");
        }

        // 如果物品已装备，先卸下
        String slot = Player.getItemSlot(itemName);
        if (slot != null && player.isEquipped(itemName)) {
            player.unequipItem(itemName);
        }

        // 从背包移除物品
        bag.removeItem(item);

        // 计算售价（买价的一半，向下取整，最低$1）
        int buyPrice = ItemRegistry.getPrice(itemName);
        int sellPrice = Math.max(1, buyPrice / 2);

        // 加钱
        Money money = player.getMoney();
        if (money != null) {
            money.add(sellPrice);
        }

        return GameResponse.success("你出售了 " + itemName + "，获得 $" + sellPrice + "。当前拥有 $" + (money != null ? money.getAmount() : 0) + "。",
                game.getCurrentRoom().getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null) {
            if (params.length >= 1) {
                this.subCommand = params[0];
            }
            if (params.length >= 2) {
                this.itemName = params[1];
            }
        }
    }
}
