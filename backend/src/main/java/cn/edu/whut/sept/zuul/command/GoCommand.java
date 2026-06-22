package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 移动命令：go + 方向
 */
public class GoCommand implements Command {
    private Game game;
    private String direction;

    public GoCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (direction == null) {
            return GameResponse.error("要去哪儿？请指定方向（east, south, west, north）");
        }

        // ---- 检查玩家是否存活（兜底防御，主入口 executeCommand 已做相同检查） ----
        Player player = game.getPlayer();
        if (player != null && !player.isAlive()) {
            game.setGameOver(true);
            return GameResponse.error("你因持续伤害而死亡，游戏结束。");
        }

        Room currentRoom = game.getCurrentRoom();
        
        // 检查当前房间是否为怪物房间且还有存活的怪物 — 阻止离开
        if (currentRoom.isMonsterRoom() && currentRoom.hasAliveMonsters()) {
            return GameResponse.error("还有怪物在附近！击败所有怪物后才能离开。");
        }

        Room nextRoom = currentRoom.getExits().get(direction);
        if (nextRoom == null) {
            return GameResponse.error("该方向没有门：" + direction + "！");
        }

        // 触发传输房间逻辑
        game.triggerTeleport(nextRoom);
        game.setCurrentRoom(nextRoom);

        // 房间怪物生成（幂等，由Room自身负责）
        try {
            nextRoom.spawnMonsters();
        } catch (Exception e) {
            // 不要因为刷怪导致移动失败
        }

        return GameResponse.success("你移动到了 " + nextRoom.getName(), nextRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.direction = params[0];
        }
    }
}