package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.model.GameResponse;

/**
 * 帮助命令：help（显示可用命令）
 */
public class HelpCommand implements Command {
    @Override
    public GameResponse execute() {
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append("You are lost. You are alone. You wander around the labyrinth.\n");
        helpMsg.append("Your command words are:\n");
        helpMsg.append("  go back look quit help\n");
        return GameResponse.success("Help information", helpMsg.toString());
    }

    @Override
    public void setParams(String... params) {
        // help命令无参数
    }
}
