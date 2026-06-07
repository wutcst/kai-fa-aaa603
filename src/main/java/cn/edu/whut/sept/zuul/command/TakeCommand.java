package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 拾取命令：take <itemName>（从当前房间拾取物品到玩家背包）
 */
public class TakeCommand implements Command {
    private Game game;
    private String itemName;

    public TakeCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (itemName == null) {
            return GameResponse.error("Take what? Please specify an item name.");
        }

        Player player = game.getPlayer();
        Room currentRoom = game.getCurrentRoom();
        Item item = currentRoom.getItem(itemName);

        if (item == null) {
            return GameResponse.error("There is no '" + itemName + "' here!");
        }

        if (player.addItem(item)) {
            currentRoom.removeItem(item);
            return GameResponse.success("You take the " + itemName + ".",
                    "Item '" + itemName + "' added to your inventory.\n" + currentRoom.getFullInfo());
        } else {
            return GameResponse.error("You can't carry the '" + itemName + "'! It's too heavy!");
        }
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.itemName = params[0];
        }
    }
}