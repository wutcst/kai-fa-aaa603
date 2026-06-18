package cn.edu.whut.sept.zuul.service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.command.*;
import cn.edu.whut.sept.zuul.model.AttackRequest;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;
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
                // ---- 注入流血层数信息（供前端展示） ----
                if (player.getStatusManager().hasBleed()) {
                    data.put("bleedLayers", player.getStatusManager().getBleedLayers());
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
            data.put("backpack", player.getBag().getBackpackItems());
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

        // ---- 测试命令：test bleed - 施加一层流血 ----
        if ("test".equalsIgnoreCase(commandWord) && parts.length >= 2 && "bleed".equalsIgnoreCase(parts[1])) {
            Player player = game.getPlayer();
            if (player != null && player.getStatusManager() != null) {
                player.getStatusManager().applyBleed(1);
                Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
                injectPlayerStatus(data);
                return GameResponse.success("测试：施加 1 层流血", data);
            }
            return GameResponse.error("施加流血失败");
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
     * 执行一次玩家攻击（扇形扫击或直线突刺），由后端做空间命中判定。
     * 前端只需传入攻击类型、玩家坐标/朝向、怪物坐标快照，后端完成全部游戏逻辑。
     *
     * @param req 攻击请求
     * @return 含更新后房间数据、命中怪物列表等
     */
    public GameResponse performAttack(AttackRequest req) {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }

        if (game.getPlayer() == null) {
            return GameResponse.error("玩家不存在");
        }

        Room current = game.getCurrentRoom();
        if (current == null) {
            return GameResponse.error("当前不在任何房间");
        }

        String attackType = req.getAttackType();
        if (attackType == null || (!attackType.equals("sweep") && !attackType.equals("pierce") && !attackType.equals("charged"))) {
            return GameResponse.error("无效的攻击类型: " + attackType);
        }

        double px = req.getPlayerX();
        double py = req.getPlayerY();
        double angle = req.getFacingAngle();

        StringBuilder sb = new StringBuilder();
        int hitCount = 0;

        // ---- 流血：每次主动攻击触发一次（在怪物命中判定之前，支持空挥） ----
        Player player = game.getPlayer();
        if (player != null && player.getStatusManager() != null) {
            int bleedDmg = player.getStatusManager().tickBleedOnAttack();
            if (bleedDmg > 0) {
                sb.append("流血状态触发，受到 ").append(bleedDmg).append(" 点真实伤害！\n");
            }
        }

        List<AttackRequest.MonsterPosition> monsters = req.getMonsters();
        if (monsters == null || monsters.isEmpty()) {
            Map<String, Object> data = new HashMap<>(current.getFullInfo());
            data.put("hitCount", 0);
            injectPlayerStatus(data);
            if (sb.length() > 0) {
                return GameResponse.success(sb.toString().trim(), data);
            }
            return GameResponse.error("攻击请求中没有怪物数据");
        }

        for (AttackRequest.MonsterPosition mp : monsters) {
            if (mp == null || mp.getName() == null) continue;
            if (!isHit(attackType, px, py, angle, mp)) continue;

            // 防御：确认怪物仍存活且可被攻击后再调用攻击方法
            Monster m = current.getMonster(mp.getName());
            if (m == null || !m.isAlive() || m.isExploding()) continue;

            int dropX = (int) Math.round(mp.getX());
            int dropY = (int) Math.round(mp.getY());
            String result;
            if ("charged".equals(attackType)) {
                result = game.chargedAttackMonster(mp.getName(), dropX, dropY);
            } else {
                result = game.attackMonster(mp.getName(), dropX, dropY);
            }
            sb.append(result).append("\n");
            hitCount++;
        }

        if (hitCount == 0) {
            sb.append("攻击落空了，没有命中任何怪物。");
        }

        Map<String, Object> data = new HashMap<>(current.getFullInfo());
        data.put("hitCount", hitCount);
        injectPlayerStatus(data);
        return GameResponse.success(sb.toString().trim(), data);
    }

    /**
     * 空间命中判定：根据攻击类型检查怪兽是否在判定范围内。
     */
    private boolean isHit(String attackType, double px, double py, double angle,
                          AttackRequest.MonsterPosition mp) {
        if ("sweep".equals(attackType)) {
            double dx = mp.getX() - px;
            double dy = mp.getY() - py;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 120) return false;
            double angleToMon = Math.atan2(dy, dx);
            double diff = angleToMon - angle;
            while (diff > Math.PI) diff -= 2 * Math.PI;
            while (diff < -Math.PI) diff += 2 * Math.PI;
            return Math.abs(diff) <= Math.toRadians(67.5);
        } else if ("pierce".equals(attackType)) {
            double dirX = Math.cos(angle);
            double dirY = Math.sin(angle);
            double mdx = mp.getX() - px;
            double mdy = mp.getY() - py;
            double along = mdx * dirX + mdy * dirY;
            if (along < 0 || along > 120) return false;
            double perp = Math.abs(mdx * (-dirY) + mdy * dirX);
            return perp <= 21.0; // 12/2 + 15
        } else if ("charged".equals(attackType)) {
            // 蓄力攻击：360° 周身 150px 范围
            double dx = mp.getX() - px;
            double dy = mp.getY() - py;
            double dist = Math.sqrt(dx * dx + dy * dy);
            return dist <= 150;
        }
        return false;
    }

    /**
     * 获取全地图数据（供小地图使用）
     */
    public Map<String, Object> getFullMap() {
        return game.getFullMap();
    }
}
