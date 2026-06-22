package cn.edu.whut.sept.zuul.service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.command.*;
import cn.edu.whut.sept.zuul.model.AttackRequest;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.GameSaveEntity;
import cn.edu.whut.sept.zuul.model.Magic;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 游戏服务层：封装游戏核心逻辑，解耦控制器和游戏类
 */
@Service
public class GameService {
    // 游戏实例（单例，模拟单玩家；多玩家需改为Map<玩家ID, Game>）
    private Game game;

    @Autowired
    private SaveService saveService;

    public GameService() {
        this.game = new Game();
    }

    /**
     * 将玩家状态注入到响应数据 Map 中。
     * 同时驱动所有负面状态计时（烧伤/中毒），确保无论前端以多高频率轮询，
     * 各状态都能按固定间隔结算。
     */
    private void injectPlayerStatus(Map<String, Object> data) {
        Player player = game.getPlayer();
        if (player != null) {
            // ---- 驱动烧伤/中毒/流血等状态计时 ----
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
                // ---- 驱动迟缓过期检查 ----
                player.getStatusManager().tickSlow();
                // ---- 驱动束缚过期检查 ----
                player.getStatusManager().tickBind();
                // ---- 注入迟缓/束缚状态信息 ----
                if (player.getStatusManager().hasSlow()) {
                    data.put("hasSlow", true);
                }
                if (player.getStatusManager().hasBind()) {
                    data.put("hasBind", true);
                }
            }

            // ---- 风隐形态：免疫负面状态；若玩家处于风隐且被施加了debuff，立即清除 ----
            // 风隐形态下，所有负面状态tick仍会运行但层数已被clear，不会造成伤害
            if (player.isWindCloakActive() && player.getStatusManager() != null) {
                // 风隐形态下清除所有debuff（持续免疫）
                player.getStatusManager().clear();
            }

            // ---- 驱动风隐形态MP消耗 ----
            int windCloakMp = player.tickWindCloakMp();
            if (windCloakMp < 0) {
                data.put("windCloakAutoDeactivate", true);
            }

            // ---- 通用死亡检查：任一负面状态使 HP 降至 0 即判死 ----
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
            // 注入风隐形态状态
            data.put("windCloakActive", player.isWindCloakActive());
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

        // ---- 检查玩家是否因持续伤害（流血/烧伤/中毒）已经死亡 ----
        // 若 gameOver 尚未被设置（例如攻击响应尚未到达前端，前端就发来了 go 命令），
        // 此处的存活检查可防止已死亡玩家继续移动或执行其他命令。
        Player player = game.getPlayer();
        if (player != null && !player.isAlive()) {
            game.setGameOver(true);
            return GameResponse.error("你因持续伤害而死亡，游戏结束。");
        }

        // 解析命令（分割命令词和参数）
        String[] parts = commandStr.trim().split("\\s+");
        String commandWord = parts[0];

        // ---- 测试命令：test poison - 施加一层中毒 ----
        if ("test".equalsIgnoreCase(commandWord) && parts.length >= 2 && "poison".equalsIgnoreCase(parts[1])) {
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
            if (player != null && player.getStatusManager() != null) {
                player.getStatusManager().applyBleed(1);
                Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
                injectPlayerStatus(data);
                return GameResponse.success("测试：施加 1 层流血", data);
            }
            return GameResponse.error("施加流血失败");
        }

        // ---- 测试命令：test slow - 施加一次迟缓 ----
        if ("test".equalsIgnoreCase(commandWord) && parts.length >= 2 && "slow".equalsIgnoreCase(parts[1])) {
            if (player != null && player.getStatusManager() != null) {
                player.getStatusManager().applySlow(1);
                Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
                injectPlayerStatus(data);
                return GameResponse.success("测试：施加迟缓", data);
            }
            return GameResponse.error("施加迟缓失败");
        }

        // ---- 测试命令：test bind - 施加一次束缚 ----
        if ("test".equalsIgnoreCase(commandWord) && parts.length >= 2 && "bind".equalsIgnoreCase(parts[1])) {
            if (player != null && player.getStatusManager() != null) {
                player.getStatusManager().applyBind(1);
                Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
                injectPlayerStatus(data);
                return GameResponse.success("测试：施加束缚", data);
            }
            return GameResponse.error("施加束缚失败");
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

        // ---- 流血：每次主动攻击触发一次 ----
        Player player = game.getPlayer();
        if (player != null && player.getStatusManager() != null) {
            int bleedDmg = player.getStatusManager().tickBleedOnAttack();
            if (bleedDmg > 0) {
                sb.append("流血状态触发，受到 ").append(bleedDmg).append(" 点真实伤害！\n");
            }
        }

        // ---- 通用死亡检查（不绑定特定状态） ----
        if (player != null && player.checkAndMarkDeath(sb)) {
            game.setGameOver(true);
            Map<String, Object> data = new HashMap<>(current.getFullInfo());
            data.put("hitCount", 0);
            data.put("gameOver", true);
            injectPlayerStatus(data);
            return GameResponse.success(sb.toString().trim(), data);
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

        // ---- 对于突刺攻击，提取起点/终点用于线段命中判定，并授予无敌帧 ----
        double pierceStartX = px;
        double pierceStartY = py;
        double pierceEndX = px;
        double pierceEndY = py;
        if ("pierce".equals(attackType)) {
            if (req.getStartX() != null) pierceStartX = req.getStartX();
            if (req.getStartY() != null) pierceStartY = req.getStartY();
            if (req.getEndX() == null || req.getEndY() == null) {
                // 兜底：用朝向推算终点（兼容旧前端）
                pierceEndX = pierceStartX + Math.cos(angle) * 120;
                pierceEndY = pierceStartY + Math.sin(angle) * 120;
            } else {
                pierceEndX = req.getEndX();
                pierceEndY = req.getEndY();
            }
            // 突刺无敌帧：基础 180ms，风隐形态下延长至 1.5 倍 = 270ms
            int invincibilityDuration = player.isWindCloakActive() ? 270 : 180;
            player.setInvincibleUntil(System.currentTimeMillis() + invincibilityDuration);
        }

        for (AttackRequest.MonsterPosition mp : monsters) {
            if (mp == null || mp.getName() == null) continue;
            if (!isHit(attackType, px, py, angle, mp, pierceStartX, pierceStartY, pierceEndX, pierceEndY)) continue;

            // 防御：确认怪物仍存活且可被攻击后再调用攻击方法
            Monster m = current.getMonster(mp.getName());
            if (m == null || !m.isAlive()) continue;

            // 同步前端传来的怪物坐标到 Monster 对象，供掉落位置判定使用
            m.setX((int) Math.round(mp.getX()));
            m.setY((int) Math.round(mp.getY()));

            String result;
            if ("charged".equals(attackType)) {
                result = game.chargedAttackMonster(mp.getName());
            } else {
                result = game.attackMonster(mp.getName());
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
     *
     * @param pierceStartX 突刺起点 X（仅 pierce 类型使用）
     * @param pierceStartY 突刺起点 Y（仅 pierce 类型使用）
     * @param pierceEndX   突刺终点 X（仅 pierce 类型使用）
     * @param pierceEndY   突刺终点 Y（仅 pierce 类型使用）
     */
    private boolean isHit(String attackType, double px, double py, double angle,
                          AttackRequest.MonsterPosition mp,
                          double pierceStartX, double pierceStartY,
                          double pierceEndX, double pierceEndY) {
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
            // 突刺命中判定：怪物点到"起点→终点"线段的最短距离 ≤ 21px
            // 若投影在线段上，垂距即最短距离；否则取到两端点的最小距离
            double sx = pierceStartX, sy = pierceStartY;
            double ex = pierceEndX, ey = pierceEndY;
            double segDx = ex - sx;
            double segDy = ey - sy;
            double segLenSq = segDx * segDx + segDy * segDy;

            double mx = mp.getX();
            double my = mp.getY();

            if (segLenSq < 0.01) {
                // 起点终点重合，退化为点判定
                double dist = Math.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy));
                return dist <= 21.0;
            }

            // 投影参数 t = ((mx-sx)·(ex-sx) + (my-sy)·(ey-sy)) / segLenSq
            double t = ((mx - sx) * segDx + (my - sy) * segDy) / segLenSq;
            // 夹到 [0, 1]：若投影在线段外，取最近端点
            double clampT = Math.max(0, Math.min(1, t));
            double closestX = sx + clampT * segDx;
            double closestY = sy + clampT * segDy;
            double perpDist = Math.sqrt((mx - closestX) * (mx - closestX) + (my - closestY) * (my - closestY));

            return perpDist <= 21.0;
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

    // ======================== 风隐技能接口 ========================

    /**
     * 激活风隐形态：清除负面状态，开启移速加成。
     * @return 含玩家状态的响应
     */
    public GameResponse activateWindCloak() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");
        if (game.isGameOver()) return GameResponse.error("Game is over!");
        if (player.isWindCloakActive()) {
            return GameResponse.error("风隐形态已激活");
        }
        player.activateWindCloak();
        Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
        injectPlayerStatus(data);
        return GameResponse.success("风隐形态开启！移速提高100%，免疫负面状态。", data);
    }

    /**
     * 解除风隐形态。
     * @return 含玩家状态的响应
     */
    public GameResponse deactivateWindCloak() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");
        if (!player.isWindCloakActive()) {
            return GameResponse.error("风隐形态未激活");
        }
        player.deactivateWindCloak();
        Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
        injectPlayerStatus(data);
        return GameResponse.success("风隐形态已解除。", data);
    }

    // ======================== 寒冰风暴技能接口 ========================

    /** 寒冰风暴迟缓持续时间（毫秒）：10秒 */
    private static final long ICE_STORM_SLOW_DURATION = 10000L;

    /**
     * 释放寒冰风暴：消耗25MP，对房间内全体敌人造成3次100%法伤，并施加迟缓。
     * @param monsterPositions 怪物名称和坐标列表（供掉落坐标同步）
     * @return 含更新后房间数据、命中怪物数量和伤害信息的响应
     */
    public GameResponse castIceStorm(List<Map<String, Object>> monsterPositions) {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }

        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");

        Room current = game.getCurrentRoom();
        if (current == null) return GameResponse.error("当前不在任何房间");

        // 检查并消耗MP
        if (!Magic.canCast(player, Magic.Skill.ICE_STORM)) {
            return GameResponse.error("魔力不足" + Magic.Skill.ICE_STORM.getMpCost() + "，无法释放寒冰风暴！");
        }
        player.consumeMp(Magic.Skill.ICE_STORM.getMpCost());

        int magicDmg = Magic.calcMagicDamage(player, Magic.Skill.ICE_STORM);
        StringBuilder sb = new StringBuilder();
        sb.append("寒冰风暴释放！\n");

        // 同步怪物坐标
        if (monsterPositions != null) {
            for (Map<String, Object> mp : monsterPositions) {
                String name = (String) mp.get("name");
                if (name == null) continue;
                Monster m = current.getMonster(name);
                if (m == null) continue;
                try { m.setX(((Number) mp.get("x")).intValue()); } catch (Exception e) {}
                try { m.setY(((Number) mp.get("y")).intValue()); } catch (Exception e) {}
            }
        }

        int hitCount = 0;
        // 对房间内所有存活怪物造成3次法伤（使用快照避免在迭代中修改列表）
        List<Monster> snapshot = new java.util.ArrayList<>(current.getMonsters());
        for (Monster m : snapshot) {
            if (m == null || !m.isAlive()) continue;
            hitCount++;
            int totalDmg = 0;
            for (int hit = 0; hit < 3; hit++) {
                Magic.dealMagicDamage(m, magicDmg);
                totalDmg += magicDmg;
            }
            // 施加迟缓状态（10秒内移速为0）
            m.applySlow(ICE_STORM_SLOW_DURATION);

            sb.append(m.getName()).append(" 受到3次冰霜冲击，共 ").append(totalDmg).append(" 点法术伤害，并被迟缓！");
            if (!m.isAlive()) {
                current.removeMonster(m);
                sb.append("\n你击败了 ").append(m.getName()).append("！");
                sb.append(game.processMonsterDrop(m));
            }
            sb.append("\n");
        }

        if (hitCount == 0) {
            sb.append("房间内没有怪物。");
        }

        Map<String, Object> data = new HashMap<>(current.getFullInfo());
        data.put("hitCount", hitCount);
        data.put("iceStormDamage", magicDmg);
        data.put("iceStormTotalHits", hitCount * 3);
        injectPlayerStatus(data);
        return GameResponse.success(sb.toString().trim(), data);
    }

    // ======================== 存档接口 ========================

    /**
     * 保存当前游戏到数据库。
     * @param saveId 存档ID（null表示新建）
     * @return 保存的存档信息
     */
    public GameSaveEntity saveGame(Long saveId) {
        return saveService.saveGame(game, saveId);
    }

    /**
     * 从数据库加载存档，替换当前游戏实例。
     * @param saveId 存档ID
     * @return 加载后当前房间的状态数据
     */
    public Map<String, Object> loadGame(Long saveId) {
        this.game = saveService.loadGame(saveId);
        Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
        injectPlayerStatus(data);
        return data;
    }

    /**
     * 获取所有存档列表。
     */
    public List<Map<String, Object>> listSaves() {
        return saveService.listSaves().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("playerName", e.getPlayerName());
            m.put("hp", e.getHp());
            m.put("maxHp", e.getMaxHp());
            m.put("money", e.getMoney());
            m.put("currentRoom", e.getCurrentRoom());
            m.put("updatedAt", e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 删除存档。
     */
    public void deleteSave(Long saveId) {
        saveService.deleteSave(saveId);
    }
}
