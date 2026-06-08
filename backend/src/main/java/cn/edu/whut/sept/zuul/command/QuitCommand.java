package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;

/**
 * 退出命令：quit
 */
public class QuitCommand implements Command {
    private Game game;

    public QuitCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        game.setGameOver(true);
        return GameResponse.success("感谢游玩！再见。", null);
    }

    @Override
    public void setParams(String... params) {
        // quit命令无参数
    }
}