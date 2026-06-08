package cn.edu.whut.sept.zuul.service;
import java.util.Arrays;
import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.command.Command;
import cn.edu.whut.sept.zuul.command.CommandFactory;
import cn.edu.whut.sept.zuul.model.GameResponse;
import org.springframework.stereotype.Service;

/**
 * 游戏服务层：封装游戏核心逻辑，解耦控制器和游戏类
 */
@Service
public class GameService {
    // 游戏实例（单例，模拟单玩家；多玩家需改为Map<玩家ID, Game>）
    private final Game game;
    private final CommandFactory commandFactory;

    public GameService() {
        this.game = new Game();
        this.commandFactory = new CommandFactory(game);
    }

    /**
     * 执行玩家命令
     * @param commandStr 命令字符串（如"go east"、"back"）
     * @return 游戏响应
     */
    public GameResponse executeCommand(String commandStr) {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }

        // 解析命令（分割命令词和参数）
        String[] parts = commandStr.trim().split("\\s+");
        String commandWord = parts[0];
        String[] params = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        // 创建并执行命令
        Command command = commandFactory.createCommand(commandWord);
        if (command == null) {
            return GameResponse.error("I don't know what you mean by '" + commandWord + "'! Type 'help' for available commands.");
        }

        command.setParams(params);
        return command.execute();
    }

    /**
     * 获取当前游戏状态（房间信息等）
     */
    public GameResponse getGameStatus() {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }
        return GameResponse.success("Current game status", game.getCurrentRoom().getFullInfo());
    }

    /**
     * 重置游戏
     */
    public GameResponse resetGame() {
        game.reset();
        return GameResponse.success("Game reset successfully", game.getCurrentRoom().getFullInfo());
    }
}