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
            return GameResponse.error("要拿什么？请指定物品名称。");
        }

        Player player = game.getPlayer();
        Room currentRoom = game.getCurrentRoom();
        Item item = currentRoom.getItem(itemName);

        if (item == null) {
            return GameResponse.error("这里没有名为 '" + itemName + "' 的物品！");
        }

        player.getBag().addItem(item);
        currentRoom.removeItem(item);
        // 返回当前房间完整信息作为 data，前端依赖 data 来渲染房间
        return GameResponse.success("你拾取了 " + itemName + "。",
                currentRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.itemName = params[0];
        }
    }
}