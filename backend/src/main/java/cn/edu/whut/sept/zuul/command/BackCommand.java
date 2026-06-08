package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Room;

/**
 * 回退命令：back
 */
public class BackCommand implements Command {
    private Game game;

    public BackCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Room previousRoom = game.getPreviousRoom();
        if (previousRoom == null) {
            return GameResponse.error("你无法再回退了！");
        }

        // 交换当前房间和历史房间
        Room currentRoom = game.getCurrentRoom();
        game.setCurrentRoom(previousRoom);
        game.removeLastRoomHistory(); // 移除最后一条历史（避免重复回退）
        game.addRoomHistory(currentRoom);

        return GameResponse.success("你回到了 " + previousRoom.getName(), previousRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        // back命令无参数
    }
}