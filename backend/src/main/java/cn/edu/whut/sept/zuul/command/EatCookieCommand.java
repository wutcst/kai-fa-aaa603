package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Player;

/**
 * 吃饼干命令：eat cookie（吃掉魔法饼干，增加最大负重5kg）
 */
public class EatCookieCommand implements Command {
    private Game game;
    private String target;

    public EatCookieCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (target == null || !target.equalsIgnoreCase("cookie")) {
            return GameResponse.error("吃什么？你只能吃 'cookie'！");
        }

        Player player = game.getPlayer();
        Item cookie = player.getItem("cookie");

        if (cookie == null) {
            return GameResponse.error("你没有可吃的饼干！先找到一个。");
        }

        // 吃掉魔法饼干：增加负重上限5kg，移除饼干
        player.removeItem(cookie);
        player.increaseMaxWeight(5.0);

        return GameResponse.success("你吃掉了魔法饼干！",
                "美味！魔法饼干使你更强壮！\n"
                        + "你的最大负重增加了 5.0 kg。\n"
                        + "当前负重：" + player.getTotalWeight() + "/" + player.getMaxWeight() + " kg");
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.target = params[0];
        }
    }
}