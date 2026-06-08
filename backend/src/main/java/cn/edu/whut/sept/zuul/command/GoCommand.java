package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
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

        Room currentRoom = game.getCurrentRoom();
        Room nextRoom = currentRoom.getExits().get(direction);
        if (nextRoom == null) {
            return GameResponse.error("该方向没有门：" + direction + "！");
        }

        // 触发传输房间逻辑
        game.triggerTeleport(nextRoom);
        // 记录房间历史（供back命令使用）
        game.addRoomHistory(currentRoom);
        game.setCurrentRoom(nextRoom);

        return GameResponse.success("你移动到了 " + nextRoom.getName(), nextRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.direction = params[0];
        }
    }
}