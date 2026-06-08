package cn.edu.whut.sept.zuul.command;
import cn.edu.whut.sept.zuul.command.Command;
import cn.edu.whut.sept.zuul.model.GameResponse;

/**
 * 帮助命令：help（显示可用命令）
 */
public class HelpCommand implements Command {
    @Override
    public GameResponse execute() {
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append("你迷路了。你孤身一人，在迷宫中徘徊。\n");
        helpMsg.append("可用命令：\n");
        helpMsg.append("  go back look quit help\n");
        helpMsg.append("  take drop items eat\n");
        return GameResponse.success("帮助信息", helpMsg.toString());
    }

    @Override
    public void setParams(String... params) {
        // help锻炼护鏃犲弬鏁?
    }
}
