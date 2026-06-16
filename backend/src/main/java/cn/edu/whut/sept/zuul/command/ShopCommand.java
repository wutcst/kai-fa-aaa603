package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;

/**
 * 商店命令：shop buy <itemName> — 购买指定商品
 * 仅在商店房间中可用，NPC 已经存在。
 */
public class ShopCommand implements Command {
    private Game game;
    private String subCommand; // buy
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
            return GameResponse.error("用法：shop buy <商品名>");
        }

        if ("buy".equalsIgnoreCase(subCommand)) {
            if (itemName == null || itemName.isBlank()) {
                return GameResponse.error("请指定要购买的商品名称，如：shop buy 商品12");
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

            // 创建物品并放入背包（无视重量）
            Item item = new Item(itemName, shopItem.getPrice() / 10.0); // 重量设为价格的十分之一
            item.setDescription("从商店购买的" + itemName);
            player.getBag().addItem(item);

            return GameResponse.success("你成功购买了 " + itemName + "，花费 $" + price + "。剩余 $" + money.getAmount() + "。",
                    current.getFullInfo());
        }

        return GameResponse.error("未知的商店子命令：" + subCommand + "。可用子命令：buy");
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
