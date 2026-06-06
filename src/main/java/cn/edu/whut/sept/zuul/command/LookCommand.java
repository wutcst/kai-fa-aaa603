package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;

/**
 * 查看命令：look（显示当前房间完整信息）
 */
public class LookCommand implements Command {
    private Game game;

    public LookCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        return GameResponse.success("You look around the room", game.getCurrentRoom().getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        // look命令无参数
    }
}