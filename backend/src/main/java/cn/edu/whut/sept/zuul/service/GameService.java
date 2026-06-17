package cn.edu.whut.sept.zuul.service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.command.*;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;
import org.springframework.stereotype.Service;

/**
 * 娓告垙鏈嶅姟灞傦細灏佽娓告垙鏍稿績閫昏緫锛岃В鑰︽帶鍒跺櫒鍜屾父鎴忕被
 */
@Service
public class GameService {
    /** 鐏劙鍙茶幈濮嗚嚜鐖嗗€掕鏃讹紙姣锛夛細3绉?*/
    private static final long EXPLODE_DELAY = 3000L;

    // 娓告垙瀹炰緥锛堝崟渚嬶紝妯℃嫙鍗曠帺瀹讹紱澶氱帺瀹堕渶鏀逛负Map<鐜╁ID, Game>锛?
    private final Game game;

    public GameService() {
        this.game = new Game();
    }

    /**
     * 椹卞姩鎵€鏈夋埧闂翠腑鐖嗙偢鍊掕鏃舵€墿鐨勭粨绠椼€?
     * 濡傛灉鍊掕鏃跺凡鍒帮紝鎵ц鐖嗙偢浼ゅ骞剁Щ闄ゆ€墿銆?
     * 鍚屾椂灏嗙垎鐐镐腑鐨勬€墿淇℃伅娉ㄥ叆 data 渚涘墠绔覆鏌撱€?
     */
    private void tickExplosions(Map<String, Object> data) {
        Player player = game.getPlayer();
        if (player == null) return;

        List<Room> allRooms = game.getAllRooms();
        if (allRooms == null) return;

        Room currentRoom = game.getCurrentRoom();
        long now = System.currentTimeMillis();

        // 鏀堕泦褰撳墠鎴块棿涓垎鐐镐腑鐨勬€墿淇℃伅
        java.util.List<Map<String, Object>> explodingList = new java.util.ArrayList<>();

        for (Room room : allRooms) {
            List<Monster> monsters = room.getMonsters();
            if (monsters == null) continue;

            Iterator<Monster> it = monsters.iterator();
            while (it.hasNext()) {
                Monster m = it.next();
                if (!m.isExploding()) continue;

                long elapsed = now - m.getExplodeStartTime();
                long remaining = Math.max(0, EXPLODE_DELAY - elapsed);

                if (remaining <= 0) {
                    // 鐖嗙偢鍊掕鏃跺埌 鈥?鏍囪宸查€氱煡锛屼笉鍦ㄦ绉婚櫎锛堢敱ExplodeCommand绉婚櫎锛?
                    // 闃叉閲嶅閫氱煡
                    if (!m.isExplodeNotified()) {
                        m.setExplodeNotified(true);

                        Map<String, Object> triggered = new HashMap<>();
                        triggered.put("name", m.getName());
                        triggered.put("specialType", m.getSpecialType());
                        triggered.put("explodeRange", m.getExplodeRange());
                        triggered.put("explodeDamage", (int)(m.getAttack() * 2.0));
                        triggered.put("x", 0);
                        triggered.put("y", 0);

                        @SuppressWarnings("unchecked")
                        java.util.List<Map<String, Object>> triggeredList =
                            (java.util.List<Map<String, Object>>) data.get("explodeTriggeredMonsters");
                        if (triggeredList == null) {
                            triggeredList = new java.util.ArrayList<>();
                            data.put("explodeTriggeredMonsters", triggeredList);
                        }
                        triggeredList.add(triggered);
                    }
                } else {
                    // 浠嶅湪鍊掕鏃朵腑锛屾敹闆嗕俊鎭紙浠呭綋鍓嶆埧闂达級
                    if (room == currentRoom) {
                        Map<String, Object> info = new HashMap<>();
                        info.put("name", m.getName());
                        info.put("exploding", true);
                        info.put("specialType", m.getSpecialType());
                        info.put("explodeRange", m.getExplodeRange());
                        info.put("explodeRemaining", remaining);
                        info.put("x", 0);  // 鍓嶇瑕嗙洊
                        info.put("y", 0);
                        explodingList.add(info);
                    }
                }
            }
        }

        if (!explodingList.isEmpty()) {
            data.put("explodingMonsters", explodingList);
        }
    }

    /**
     * 灏嗙帺瀹剁姸鎬佹敞鍏ュ埌鍝嶅簲鏁版嵁 Map 涓€?
     * 鍚屾椂椹卞姩鐑т激缁撶畻璁℃椂锛坱ickBurn锛夛紝纭繚鏃犺鍓嶇浠ュ楂橀鐜囪疆璇紝
     * 鐑т激姣?绉掗兘鑷冲皯琚粨绠椾竴娆°€?
     */
    private void injectPlayerStatus(Map<String, Object> data) {
        Player player = game.getPlayer();
        if (player != null) {
            // ---- 椹卞姩鐑т激璁℃椂 ----
            if (player.getStatusManager() != null) {
                int burnDmg = player.getStatusManager().tickBurn();
                if (burnDmg > 0) {
                    data.put("burnDamage", burnDmg);
                    data.put("burnLayers", player.getStatusManager().getBurnLayers());
                }
            }

            // ---- 椹卞姩鐖嗙偢璁℃椂 ----
            tickExplosions(data);

            data.put("playerHp", player.getHp());
            data.put("playerMaxHp", player.getMaxHp());
            data.put("playerMp", player.getMp());
            data.put("playerMaxMp", player.getMaxMp());
            if (player.getMoney() != null) {
                data.put("playerMoney", player.getMoney().getAmount());
            }
            // 娉ㄥ叆鑳屽寘鏁版嵁
            java.util.List<cn.edu.whut.sept.zuul.model.InventoryItem> bp = player.getBag().getBackpackItems();
            System.out.println("[Backpack] injectPlayerStatus: inventory size=" + player.getBag().getInventory().size() + ", backpack items=" + bp.size());
            for (cn.edu.whut.sept.zuul.model.InventoryItem it : bp) {
                System.out.println("  - " + it.getName() + " rarity=" + it.getRarity() + " qty=" + it.getQuantity());
            }
            data.put("backpack", bp);
            // 娉ㄥ叆娲昏穬鐘舵€佹晥鏋?
            if (player.getStatusManager() != null) {
                data.put("activeEffects", player.getStatusManager().getActiveEffectsInfo());
            }
            // 娉ㄥ叆淇鍚庡睘鎬э紙渚涘墠绔疕UD鏄剧ず锛?
            data.put("effectiveAttack", player.getEffectiveAttack());
            data.put("effectiveDefense", player.getEffectiveDefense());
            data.put("effectiveMagicAttack", player.getEffectiveMagicAttack());
            data.put("effectiveMagicResist", player.getEffectiveMagicResist());
            data.put("effectiveSpeed", player.getEffectiveSpeed());
            data.put("effectiveDodge", player.getEffectiveDodge());
        }
    }

    /**
     * 鍖呰 GameResponse锛屽湪鍏?data 涓敞鍏ョ帺瀹剁姸鎬?
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
     * 鏍规嵁鍛戒护瀛楃涓插垱寤哄搴斿懡浠ゅ疄渚嬶紙鏇夸唬鍘?CommandFactory锛?
     */
    private Command createCommand(String commandWord) {
        if (commandWord == null) {
            return null;
        }
        return switch (commandWord.toLowerCase()) {
            case "go" -> new GoCommand(game);
            case "attack" -> new AttackCommand(game);
            case "monsterattack" -> new MonsterAttackCommand(game);
            case "take" -> new BagCommand(game);
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
     * 鎵ц鐜╁鍛戒护
     * @param commandStr 鍛戒护瀛楃涓诧紙濡?go east"锛?
     * @return 娓告垙鍝嶅簲
     */
    public GameResponse executeCommand(String commandStr) {
        if (game.isGameOver()) {
            return GameResponse.error("Game is over! Please reset the game.");
        }

        // 瑙ｆ瀽鍛戒护锛堝垎鍓插懡浠よ瘝鍜屽弬鏁帮級
        String[] parts = commandStr.trim().split("\\s+");
        String commandWord = parts[0];
        String[] params = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        // 鍒涘缓骞舵墽琛屽懡浠?
        Command command = createCommand(commandWord);
        if (command == null) {
            return GameResponse.error("I don't know what you mean by '" + commandWord + "'! Type 'help' for available commands.");
        }

        command.setParams(params);
        return wrapWithPlayerStatus(command.execute());
    }

    /**
     * 鑾峰彇褰撳墠娓告垙鐘舵€侊紙鎴块棿淇℃伅绛夛級
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
     * 閲嶇疆娓告垙
     */
    public GameResponse resetGame() {
        game.reset();
        Map<String, Object> data = new HashMap<>(game.getCurrentRoom().getFullInfo());
        injectPlayerStatus(data);
        return GameResponse.success("Game reset successfully", data);
    }

    /**
     * 鑾峰彇鍏ㄥ湴鍥炬暟鎹紙渚涘皬鍦板浘浣跨敤锛?
     */
    public Map<String, Object> getFullMap() {
        return game.getFullMap();
    }
}
