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
            return GameResponse.error("Eat what? You can only eat 'cookie'!");
        }

        Player player = game.getPlayer();
        Item cookie = player.getItem("cookie");

        if (cookie == null) {
            return GameResponse.error("You don't have a cookie to eat! Find one first.");
        }

        // 吃掉魔法饼干：增加负重上限5kg，移除饼干
        player.removeItem(cookie);
        player.increaseMaxWeight(5.0);

        return GameResponse.success("You eat the magic cookie!",
                "Yummy! The magic cookie gives you more strength!\n"
                        + "Your max carrying weight increased by 5.0 kg.\n"
                        + "Current: " + player.getTotalWeight() + "/" + player.getMaxWeight() + " kg");
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.target = params[0];
        }
    }
}