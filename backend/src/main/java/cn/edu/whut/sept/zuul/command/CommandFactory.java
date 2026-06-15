package cn.edu.whut.sept.zuul.command;
import cn.edu.whut.sept.zuul.Game;
import lombok.AllArgsConstructor;

/**
 * CommandFactory：根据命令字符串创建对应命令实例
 */
@AllArgsConstructor
public class CommandFactory {
    private Game game;

    public Command createCommand(String commandWord) {
        if (commandWord == null) {
            return null;
        }
        return switch (commandWord.toLowerCase()) {
            case "go" -> new GoCommand(game);
            case "back" -> new BackCommand(game);
            case "look" -> new LookCommand(game);
            case "quit" -> new QuitCommand(game);
            case "help" -> new HelpCommand();
            case "attack" -> new AttackCommand(game);
            case "take" -> new TakeCommand(game);
            case "drop" -> new DropCommand(game);
            case "items" -> new ItemsCommand(game);
            case "eat" -> new EatCookieCommand(game);
            case "interact" -> new InteractCommand(game);
            case "shop" -> new ShopCommand(game);
            default -> null;
        };
    }
}
