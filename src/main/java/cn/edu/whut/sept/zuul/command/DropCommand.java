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
            return GameResponse.error("Drop what? Please specify an item name.");
        }

        Player player = game.getPlayer();
        Room currentRoom = game.getCurrentRoom();
        Item item = player.getItem(itemName);

        if (item == null) {
            return GameResponse.error("You don't have a '" + itemName + "'!");
        }

        player.removeItem(item);
        currentRoom.addItem(item);
        return GameResponse.success("You drop the " + itemName + ".",
                "Item '" + itemName + "' dropped in the room.\n" + currentRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.itemName = params[0];
        }
    }
}