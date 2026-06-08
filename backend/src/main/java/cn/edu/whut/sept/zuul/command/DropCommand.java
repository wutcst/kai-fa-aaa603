package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 丢弃命令：drop <itemName>（从玩家背包丢弃物品到当前房间）
 */
public class DropCommand implements Command {
    private Game game;
    private String itemName;

    public DropCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (itemName == null) {
            return GameResponse.error("要丢弃什么？请指定物品名称。");
        }

        Player player = game.getPlayer();
        Room currentRoom = game.getCurrentRoom();
        Item item = player.getItem(itemName);

        if (item == null) {
            return GameResponse.error("你没有 '" + itemName + "' ！");
        }

        player.removeItem(item);
        currentRoom.addItem(item);
        return GameResponse.success("你丢弃了 " + itemName + "。",
                "物品 '" + itemName + "' 已丢弃到房间。\n" + currentRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.itemName = params[0];
        }
    }
}