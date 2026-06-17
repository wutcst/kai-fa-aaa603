package cn.edu.whut.sept.zuul.service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.command.*;
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

    public GameService() {
        this.game = new Game();
    }

    /**
     * 将玩家状态注入到响应数据 Map 中。
     * 同时驱动烧伤结算计时（tickBurn），确保无论前端以多高频率轮询，
     * 烧伤每3秒都至少被结算一次。
     */
    private void injectPlayerStatus(Map<String, Object> data) {
        Player player = game.getPlayer();
        if (player != null) {
            // ---- 驱动烧伤计时 ----
            if (player.getStatusManager() != null) {
                int burnDmg = player.getStatusManager().tickBurn();
                if (burnDmg > 0) {
                    data.put("burnDamage", burnDmg);
                    data.put("burnLayers", player.getStatusManager().getBurnLayers());
                }
                // ---- 驱动中毒计时 ----
                int poisonDmg = player.getStatusManager().tickPoison();
                if (poisonDmg > 0) {
                    data.put("poisonDamage", poisonDmg);
                    data.put("poisonLayers", player.getStatusManager().getPoisonLayers());
                }
            }

            // ---- 检查玩家是否因烧伤死亡 ----
            if (!player.isAlive() && !game.isGameOver()) {
                game.setGameOver(true);
                data.put("gameOver", true);
            }

            // ---- 驱动生命戒指被动回血 ----
            int regen = player.tickRingRegen();
            if (regen > 0) {
                data.put("ringRegen", regen);
            }

            data.put("playerHp", player.getHp());
            data.put("playerMaxHp", player.getMaxHp());
            data.put("playerMp", player.getMp());
            data.put("playerMaxMp", player.getMaxMp());
            if (player.getMoney() != null) {
                data.put("playerMoney", player.getMoney().getAmount());
            }
            // 注入背包数据
            java.util.List<cn.edu.whut.sept.zuul.model.InventoryItem> bp = player.getBag().getBackpackItems();
            System.out.println("[Backpack] injectPlayerStatus: inventory size=" + player.getBag().getInventory().size() + ", backpack items=" + bp.size());
            for (cn.edu.whut.sept.zuul.model.InventoryItem it : bp) {
                System.out.println("  - " + it.getName() + " rarity=" + it.getRarity() + " qty=" + it.getQuantity());
            }
            data.put("backpack", bp);
            // 注入活跃状态效果
            if (player.getStatusManager() != null) {
                data.put("activeEffects", player.getStatusManager().getActiveEffectsInfo());
            }
            // 注入修正后属性（供前端HUD显示）
            data.put("effectiveAttack", player.getEffectiveAttack());
            data.put("effectiveDefense", player.getEffectiveDefense());
            data.put("effectiveMagicAttack", player.getEffectiveMagicAttack());
            data.put("effectiveMagicResist", player.getEffectiveMagicResist());
            data.put("effectiveSpeed", player.getEffectiveSpeed());
            data.put("effectiveDodge", player.getEffectiveDodge());
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
     * 根据命令字符串创建对应命令实例（替代原 CommandFactory）
     */
    private Command createCommand(String commandWord) {
        if (commandWord == null) {
            return null;
        }
        return switch (commandWord.toLowerCase()) {
            case "go" -> new GoCommand(game);
            case "attack" -> new AttackCommand(game);
            case "monsterattack" -> new MonsterAttackCommand(game);
            case "pickup", "take" -> new PickupCommand(game);
            case "drop" -> new DropCommand(game);
            case "items" -> new ItemsCommand(game);
            case "interact" -> new InteractCommand(game);
            case "shop" -> new ShopCommand(game);
            case "wave" -> new WaveCommand(game);
            case "explode" -> new ExplodeCommand(game);
            case "bag" -> new BagCommand(game);
            default -> null;
        };
    }

    /**
     * 执行玩家命令
     * @param commandStr 命令字符串（如"go east"）
     * @return 游戏响应
     */
    public GameResponse executeCommand(String commandStr) {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }

        // 解析命令（分割命令词和参数）
        String[] parts = commandStr.trim().split("\\s+");
        String commandWord = parts[0];

        // ---- 测试命令：test burn - 施加一层烧伤 ----
        if ("test".equalsIgnoreCase(commandWord) && parts.length >= 2 && "burn".equalsIgnoreCase(parts[1])) {
            Player player = game.getPlayer();
            if (player != null && player.getStatusManager() != null) {
                player.getStatusManager().applyBurn(1);
                Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
                injectPlayerStatus(data);
                return GameResponse.success("测试：施加 1 层烧伤", data);
            }
            return GameResponse.error("施加烧伤失败");
        }
        // ---- 测试命令：test poison - 施加一层中毒 ----
        if ("test".equalsIgnoreCase(commandWord) && parts.length >= 2 && "poison".equalsIgnoreCase(parts[1])) {
            Player player = game.getPlayer();
            if (player != null && player.getStatusManager() != null) {
                player.getStatusManager().applyPoison(1);
                Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
                injectPlayerStatus(data);
                return GameResponse.success("测试：施加 1 层中毒", data);
            }
            return GameResponse.error("施加中毒失败");
        }
        String[] params = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        // 创建并执行命令
        Command command = createCommand(commandWord);
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
