package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
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
        sb.append("Room items:\n");
        if (currentRoom.getItems().isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Item item : currentRoom.getItems()) {
                sb.append("  - ").append(item.getName())
                        .append(" (").append(item.getDescription())
                        .append(", weight: ").append(item.getWeight())
                        .append(")\n");
            }
        }

        // 显示玩家背包
        sb.append("\nInventory (").append(player.getTotalWeight())
                .append("/").append(player.getMaxWeight()).append(" kg):\n");
        if (player.getInventory().isEmpty()) {
            sb.append("  (empty)\n");
        } else {
            for (Item item : player.getInventory()) {
                sb.append("  - ").append(item.getName())
                        .append(" (").append(item.getDescription())
                        .append(", weight: ").append(item.getWeight())
                        .append(")\n");
            }
        }

        return GameResponse.success("Items and inventory info", sb.toString());
    }

    @Override
    public void setParams(String... params) {
        // items命令无参数
    }
}