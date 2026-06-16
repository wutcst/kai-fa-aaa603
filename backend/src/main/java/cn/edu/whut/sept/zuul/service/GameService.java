package cn.edu.whut.sept.zuul.service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.command.Command;
import cn.edu.whut.sept.zuul.command.CommandFactory;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Player;
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
     * 将玩家状态注入到响应数据 Map 中
     */
    private void injectPlayerStatus(Map<String, Object> data) {
        Player player = game.getPlayer();
        if (player != null) {
            data.put("playerHp", player.getHp());
            data.put("playerMaxHp", player.getMaxHp());
            data.put("playerMp", player.getMp());
            data.put("playerMaxMp", player.getMaxMp());
            if (player.getMoney() != null) {
                data.put("playerMoney", player.getMoney().getAmount());
            }
            // 注入背包数据
            java.util.List<cn.edu.whut.sept.zuul.model.InventoryItem> bp = player.getBackpackItems();
            System.out.println("[Backpack] injectPlayerStatus: inventory size=" + player.getInventory().size() + ", backpack items=" + bp.size());
            for (cn.edu.whut.sept.zuul.model.InventoryItem it : bp) {
                System.out.println("  - " + it.getName() + " rarity=" + it.getRarity() + " qty=" + it.getQuantity());
            }
            data.put("backpack", bp);
        }
    }

    /**
     * 包装 GameResponse，在其 data 中注入玩家状态
     */
    private GameResponse wrapWithPlayerStatus(GameResponse response) {
        if (response != null && response.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            injectPlayerStatus(data);
        }
        return response;
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
        return wrapWithPlayerStatus(command.execute());
    }

    /**
     * 获取当前游戏状态（房间信息等）
     */
    public GameResponse getGameStatus() {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }
        Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
        injectPlayerStatus(data);
        return GameResponse.success("Current game status", data);
    }

    /**
     * 重置游戏
     */
    public GameResponse resetGame() {
        game.reset();
        Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
        injectPlayerStatus(data);
        return GameResponse.success("Game reset successfully", data);
    }

    /**
     * 获取全地图数据（供小地图使用）
     */
    public Map<String, Object> getFullMap() {
        return game.getFullMap();
    }
}