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
            return GameResponse.error("Go where? Please specify a direction (east, south, west, north)");
        }

        Room currentRoom = game.getCurrentRoom();
        Room nextRoom = currentRoom.getExits().get(direction);
        if (nextRoom == null) {
            return GameResponse.error("There is no door to the " + direction + "!");
        }

        // 触发传输房间逻辑
        game.triggerTeleport(nextRoom);
        // 记录房间历史（供back命令使用）
        game.addRoomHistory(currentRoom);
        game.setCurrentRoom(nextRoom);

        return GameResponse.success("You move to the " + nextRoom.getName(), nextRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.direction = params[0];
        }
    }
}