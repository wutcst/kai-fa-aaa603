package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 拾取命令：pickup <itemName>
 * 拾取当前房间地面上掉落的物品（怪物掉落物），放入背包。
 */
public class PickupCommand implements Command {
    private Game game;
    private String targetName;

    public PickupCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");

        Room current = game.getCurrentRoom();
        if (targetName == null || targetName.isBlank()) {
            return GameResponse.error("要拾取什么？请指定物品名称。");
        }

        // 检查房间的掉落物列表
        var droppedItems = current.getDroppedItems();
        if (droppedItems == null || droppedItems.isEmpty()) {
            return GameResponse.error("这里没有可拾取的物品。");
        }

        // 查找匹配的掉落物
        var it = droppedItems.iterator();
        boolean found = false;
        while (it.hasNext()) {
            var drop = it.next();
            if (drop.getItemName().equalsIgnoreCase(targetName)) {
                it.remove();
                found = true;
                // 将物品加入玩家背包
                player.getBag().addItem(new Item(drop.getItemName()));
                break;
            }
        }

        if (!found) {
            return GameResponse.error("这里没有叫 '" + targetName + "' 的掉落物。");
        }

        return GameResponse.success("你拾取了 " + targetName + "！已放入背包。", current.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            // 支持多词物品名（如"生命浆果"）
            StringBuilder sb = new StringBuilder();
            for (String p : params) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(p);
            }
            this.targetName = sb.toString();
        }
    }
}
