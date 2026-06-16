package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.Bag;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 物品查看命令：items（显示当前房间物品、玩家背包及负重信息）
 */
public class ItemsCommand implements Command {
    private Game game;

    public ItemsCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        Room currentRoom = game.getCurrentRoom();

        StringBuilder sb = new StringBuilder();

        // 显示当前房间物品
        sb.append("房间物品：\n");
        if (currentRoom.getItems().isEmpty()) {
            sb.append("  （无）\n");
        } else {
            for (Item item : currentRoom.getItems()) {
                sb.append("  - ").append(item.getName())
                        .append(" (").append(item.getDescription())
                        .append(")\n");
            }
        }

        // 显示玩家背包
        Bag bag = player.getBag();
        sb.append("\n背包：\n");
        if (bag.isEmpty()) {
            sb.append("  （空）\n");
        } else {
            for (Item item : bag.getInventory()) {
                sb.append("  - ").append(item.getName())
                        .append(" (").append(item.getDescription())
                        .append(")\n");
            }
        }

        return GameResponse.success("物品和背包信息", sb.toString());
    }

    @Override
    public void setParams(String... params) {
        // items命令无参数
    }
}