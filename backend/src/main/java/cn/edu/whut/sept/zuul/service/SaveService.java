package cn.edu.whut.sept.zuul.service;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;
import cn.edu.whut.sept.zuul.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 存档服务：处理游戏状态的持久化（保存/读取）。
 * 四表联动：game_save + bag_item + room_state + shop_state
 */
@Service
public class SaveService {

    @Autowired
    private GameSaveRepository gameSaveRepo;

    @Autowired
    private BagItemRepository bagItemRepo;

    @Autowired
    private RoomStateRepository roomStateRepo;

    @Autowired
    private ShopStateRepository shopStateRepo;

    @Autowired
    private MonsterStateRepository monsterStateRepo;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ======================== 保存 ========================

    @Transactional
    public GameSaveEntity saveGame(Game game, Long saveId) {
        Player player = game.getPlayer();
        if (player == null) throw new IllegalStateException("玩家不存在，无法保存");

        // 如果该槽位已有存档，先彻底删除（确保覆盖而非主键冲突静默失败）
        if (saveId != null && gameSaveRepo.existsById(saveId)) {
            bagItemRepo.deleteAll(bagItemRepo.findBySaveId(saveId));
            roomStateRepo.deleteAll(roomStateRepo.findBySaveId(saveId));
            shopStateRepo.deleteAll(shopStateRepo.findBySaveId(saveId));
            monsterStateRepo.deleteAll(monsterStateRepo.findBySaveId(saveId));
            gameSaveRepo.deleteById(saveId);
            gameSaveRepo.flush();
        }

        // 1. 保存玩家主档
        GameSaveEntity entity = new GameSaveEntity();
        if (saveId != null) {
            entity.setId(saveId);
        }

        entity.setPlayerName(player.getName());
        entity.setHp(player.getHp());
        entity.setMaxHp(player.getMaxHp());
        entity.setMp(player.getMp());
        entity.setMaxMp(player.getMaxMp());
        entity.setAttack(player.getAttack());
        entity.setMagicAttack(player.getMagicAttack());
        entity.setDefense(player.getDefense());
        entity.setMagicResist(player.getMagicResist());
        entity.setSpeed(player.getSpeed());
        entity.setDodge(player.getDodge());
        entity.setMoney(player.getMoney() != null ? player.getMoney().getAmount() : 0);

        entity.setCurrentRoom(player.getCurrentRoom() != null ? player.getCurrentRoom().getName() : null);
        entity.setMapSeed(game.getMapSeed());

        entity.setEquippedCloak(player.getEquippedCloak());
        entity.setEquippedRing(player.getEquippedRing());
        entity.setEquippedAmulet(player.getEquippedAmulet());
        entity.setEquippedWeapon(player.getEquippedWeapon());
        entity.setEquippedArmor(player.getEquippedArmor());

        entity.setStatusJson(serializeStatus(player));

        GameSaveEntity saved = gameSaveRepo.save(entity);
        gameSaveRepo.flush();
        Long actualSaveId = saved.getId();

        // 2. 背包物品：删旧插新
        bagItemRepo.deleteAll(bagItemRepo.findBySaveId(actualSaveId));
        for (Item item : player.getBag().getInventory()) {
            bagItemRepo.save(new BagItemEntity(actualSaveId, item.getName()));
        }

        // 3. 房间状态：删旧插新
        roomStateRepo.deleteAll(roomStateRepo.findBySaveId(actualSaveId));
        for (Room room : game.getAllRooms()) {
            roomStateRepo.save(new RoomStateEntity(
                actualSaveId,
                room.getName(),
                room.isMonstersCleared(),
                room.isAltarUsed(),
                room.getRandomEvent() != null && room.getRandomEvent().isUsed()
            ));
        }

        // 4. 怪物血量：删旧插新
        monsterStateRepo.deleteAll(monsterStateRepo.findBySaveId(actualSaveId));
        for (Room room : game.getAllRooms()) {
            if (room.getMonsters() != null) {
                for (Monster m : room.getMonsters()) {
                    if (m.isAlive()) {
                        monsterStateRepo.save(new MonsterStateEntity(
                            actualSaveId, room.getName(), m.getName(),
                            m.getHp(), m.getMaxHp(), m.getType()
                        ));
                    }
                }
            }
        }

        // 5. 商店售出状态：删旧插新
        shopStateRepo.deleteAll(shopStateRepo.findBySaveId(actualSaveId));
        for (Room room : game.getAllRooms()) {
            if (room.getRoomType() == RoomType.SHOP && room.getShopItems() != null) {
                for (ShopItem si : room.getShopItems()) {
                    if (si.isSold()) {
                        shopStateRepo.save(new ShopStateEntity(actualSaveId, room.getName(), si.getName()));
                    }
                }
            }
        }

        return saved;
    }

    // ======================== 读取 ========================

    @Transactional
    public Game loadGame(Long saveId) {
        GameSaveEntity entity = gameSaveRepo.findById(saveId)
            .orElseThrow(() -> new IllegalArgumentException("存档不存在: " + saveId));

        // 1. 用种子重建地图
        Game game = new Game(entity.getMapSeed());

        // 2. 重建玩家属性
        Player player = game.getPlayer();
        player.setHp(entity.getHp());
        player.setMaxHp(entity.getMaxHp());
        player.setMp(entity.getMp());
        player.setMaxMp(entity.getMaxMp());
        player.setAttack(entity.getAttack());
        player.setMagicAttack(entity.getMagicAttack());
        player.setDefense(entity.getDefense());
        player.setMagicResist(entity.getMagicResist());
        player.setSpeed(entity.getSpeed());
        player.setDodge(entity.getDodge());
        if (player.getMoney() != null) {
            player.getMoney().setAmount(entity.getMoney());
        }

        // 3. 恢复装备槽位
        player.setEquippedCloak(entity.getEquippedCloak());
        player.setEquippedRing(entity.getEquippedRing());
        player.setEquippedAmulet(entity.getEquippedAmulet());
        player.setEquippedWeapon(entity.getEquippedWeapon());
        player.setEquippedArmor(entity.getEquippedArmor());

        // 4. 重建背包
        Bag bag = player.getBag();
        bag.getInventory().clear();
        List<BagItemEntity> bagItems = bagItemRepo.findBySaveId(saveId);
        for (BagItemEntity bi : bagItems) {
            bag.addItem(new Item(bi.getItemName()));
        }

        // 5. 恢复状态效果
        deserializeStatus(player, entity.getStatusJson());

        // 7. 构建房间名→房间映射
        Map<String, Room> roomMap = new HashMap<>();
        for (Room r : game.getAllRooms()) {
            roomMap.put(r.getName(), r);
        }

        // 8. 先应用房间状态（monstersCleared/altarUsed/randomEventUsed）
        //    必须在 initRandomEvent / spawnMonsters 之前，避免在已清除的房间里重新生成怪物
        List<RoomStateEntity> roomStates = roomStateRepo.findBySaveId(saveId);
        for (RoomStateEntity rs : roomStates) {
            Room room = roomMap.get(rs.getRoomName());
            if (room != null) {
                room.setMonstersCleared(rs.isMonstersCleared());
                room.setAltarUsed(rs.isAltarUsed());
            }
        }

        // 9. 初始化奇遇房间的随机事件（依赖步骤8的monstersCleared状态）
        for (Room room : game.getAllRooms()) {
            if (room.getRoomType() == RoomType.ENCOUNTER) {
                room.initRandomEvent();
            }
        }

        // 10. 应用 randomEventUsed 标记（必须在initRandomEvent之后）
        for (RoomStateEntity rs : roomStates) {
            Room room = roomMap.get(rs.getRoomName());
            if (room != null && rs.isRandomEventUsed()) {
                if (room.getRandomEvent() != null) {
                    room.getRandomEvent().setUsed(true);
                }
            }
        }

        // 11. 恢复怪物血量
        List<MonsterStateEntity> monsterStates = monsterStateRepo.findBySaveId(saveId);
        for (MonsterStateEntity ms : monsterStates) {
            Room room = roomMap.get(ms.getRoomName());
            if (room != null) {
                // 确保怪物已生成
                room.spawnMonsters();
                if (room.getMonsters() != null) {
                    for (Monster m : room.getMonsters()) {
                        if (m.getName().equals(ms.getMonsterName())) {
                            m.setHp(ms.getHp());
                            m.setMaxHp(ms.getMaxHp());
                            break;
                        }
                    }
                }
            }
        }

        // 12. 应用商店售出状态
        List<ShopStateEntity> shopStates = shopStateRepo.findBySaveId(saveId);
        for (ShopStateEntity ss : shopStates) {
            Room room = roomMap.get(ss.getRoomName());
            if (room != null && room.getShopItems() != null) {
                for (ShopItem si : room.getShopItems()) {
                    if (si.getName().equals(ss.getItemName())) {
                        si.setSold(true);
                    }
                }
            }
        }

        // 10. 定位到存档时的房间
        String savedRoomName = entity.getCurrentRoom();
        if (savedRoomName != null) {
            Room savedRoom = roomMap.get(savedRoomName);
            if (savedRoom != null) {
                game.setCurrentRoom(savedRoom);
            }
        }

        return game;
    }

    // ======================== 辅助方法 ========================

    private String serializeStatus(Player player) {
        Status.StatusManager sm = player.getStatusManager();
        if (sm == null) return null;

        Map<String, Object> statusMap = new HashMap<>();

        if (sm.getBurnEffect() != null && !sm.getBurnEffect().isExpired()) {
            Map<String, Object> burn = new HashMap<>();
            burn.put("layers", sm.getBurnEffect().getLayers());
            burn.put("lastTickTime", sm.getBurnEffect().getLastTickTime());
            statusMap.put("burn", burn);
        }

        if (sm.getPoisonEffect() != null && !sm.getPoisonEffect().isExpired()) {
            Map<String, Object> poison = new HashMap<>();
            poison.put("layers", sm.getPoisonEffect().getLayers());
            poison.put("lastTickTime", sm.getPoisonEffect().getLastTickTime());
            statusMap.put("poison", poison);
        }

        if (sm.getBleedEffect() != null && !sm.getBleedEffect().isExpired()) {
            Map<String, Object> bleed = new HashMap<>();
            bleed.put("layers", sm.getBleedEffect().getLayers());
            bleed.put("lastTickTime", sm.getBleedEffect().getLastTickTime());
            statusMap.put("bleed", bleed);
        }

        if (sm.getSlowEffect() != null && !sm.getSlowEffect().isExpired()) {
            Map<String, Object> slow = new HashMap<>();
            slow.put("layers", 1);
            slow.put("lastTickTime", sm.getSlowEffect().getLastTickTime());
            statusMap.put("slow", slow);
        }

        if (sm.getBindEffect() != null && !sm.getBindEffect().isExpired()) {
            Map<String, Object> bind = new HashMap<>();
            bind.put("layers", 1);
            bind.put("lastTickTime", sm.getBindEffect().getLastTickTime());
            statusMap.put("bind", bind);
        }

        if (statusMap.isEmpty()) return null;

        try {
            return MAPPER.writeValueAsString(statusMap);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void deserializeStatus(Player player, String statusJson) {
        if (statusJson == null || statusJson.isEmpty()) return;

        Status.StatusManager sm = player.getStatusManager();
        if (sm == null) return;

        try {
            Map<String, Object> statusMap = MAPPER.readValue(statusJson, Map.class);

            if (statusMap.containsKey("burn")) {
                Map<String, Object> burn = (Map<String, Object>) statusMap.get("burn");
                int layers = ((Number) burn.get("layers")).intValue();
                long lastTickTime = ((Number) burn.get("lastTickTime")).longValue();
                Status.StatusEffect effect = new Status.StatusEffect(Status.StatusType.BURN, layers);
                effect.setLastTickTime(lastTickTime);
                sm.setBurnEffect(effect);
            }

            if (statusMap.containsKey("poison")) {
                Map<String, Object> poison = (Map<String, Object>) statusMap.get("poison");
                int layers = ((Number) poison.get("layers")).intValue();
                long lastTickTime = ((Number) poison.get("lastTickTime")).longValue();
                Status.StatusEffect effect = new Status.StatusEffect(Status.StatusType.POISON, layers);
                effect.setLastTickTime(lastTickTime);
                sm.setPoisonEffect(effect);
            }

            if (statusMap.containsKey("bleed")) {
                Map<String, Object> bleed = (Map<String, Object>) statusMap.get("bleed");
                int layers = ((Number) bleed.get("layers")).intValue();
                long lastTickTime = ((Number) bleed.get("lastTickTime")).longValue();
                Status.StatusEffect effect = new Status.StatusEffect(Status.StatusType.BLEED, layers);
                effect.setLastTickTime(lastTickTime);
                sm.setBleedEffect(effect);
            }

            if (statusMap.containsKey("slow")) {
                Map<String, Object> slow = (Map<String, Object>) statusMap.get("slow");
                long lastTickTime = ((Number) slow.get("lastTickTime")).longValue();
                Status.StatusEffect effect = new Status.StatusEffect(Status.StatusType.SLOW, 1);
                effect.setLastTickTime(lastTickTime);
                sm.setSlowEffect(effect);
            }

            if (statusMap.containsKey("bind")) {
                Map<String, Object> bind = (Map<String, Object>) statusMap.get("bind");
                long lastTickTime = ((Number) bind.get("lastTickTime")).longValue();
                Status.StatusEffect effect = new Status.StatusEffect(Status.StatusType.BIND, 1);
                effect.setLastTickTime(lastTickTime);
                sm.setBindEffect(effect);
            }

        } catch (Exception e) {
            System.err.println("[SaveService] 状态效果恢复失败: " + e.getMessage());
        }
    }

    public List<GameSaveEntity> listSaves() {
        return gameSaveRepo.findAll();
    }

    @Transactional
    public void deleteSave(Long saveId) {
        bagItemRepo.deleteAll(bagItemRepo.findBySaveId(saveId));
        roomStateRepo.deleteAll(roomStateRepo.findBySaveId(saveId));
        shopStateRepo.deleteAll(shopStateRepo.findBySaveId(saveId));
        monsterStateRepo.deleteAll(monsterStateRepo.findBySaveId(saveId));
        gameSaveRepo.deleteById(saveId);
    }
}
