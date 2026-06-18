package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.DroppedItem;
import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Monster;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;
import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 游戏核心类：初始化房间、处理游戏状态、传输房间逻辑
 */
@Getter
@Setter
public class Game {
    private Room currentRoom;
    private Player player;
    private boolean gameOver;
    private List<Room> allRooms;

    public Game() {
        gameOver = false;
        createRooms();
        player = new Player("冒险者", currentRoom);
        player.getBag().setOwner(player);
        // 初始化测试物品：生命浆果 x5 + 魔力浆果 x5
        for (int i = 0; i < 5; i++) {
            player.getBag().addItem(new Item("生命浆果", "回复生命的红色浆果"));
            player.getBag().addItem(new Item("魔力浆果", "回复魔力的蓝色浆果"));
        }
    }

    private void createRooms() {
        GenerateRoom.GeneratedMap gm = GenerateRoom.generate(10, 15);
        this.allRooms = gm.rooms;

        List<Room> rooms = gm.rooms;
        if (!rooms.isEmpty()) {
            Random rnd = new Random();
            // mark one random room as teleport room
            Room tele = rooms.get(rnd.nextInt(rooms.size()));
            tele.setTeleportRoom(true);
        }
        currentRoom = gm.startRoom;
        // debug: print generated map (room name -> exits) to stdout to help debug navigation issues
        try {
            System.out.println("[GenerateRoom] Generated map:");
            for (Room r : rooms) {
                System.out.println("  " + r.getName() + " -> " + r.getExitString());
            }
            System.out.println("[GenerateRoom] Start room: " + currentRoom.getName() + " exits=" + currentRoom.getExitString());
        } catch (Exception e) {
            // ignore logging errors
        }
    }

    public Room getCurrentRoom() {
        if (player != null) {
            return player.getCurrentRoom();
        }
        return currentRoom;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
        if (player != null) {
            player.setCurrentRoom(room);
        }
        // 进入篝火房间时初始化祭坛
        try {
            if (room != null && room.getRoomType() == cn.edu.whut.sept.zuul.model.RoomType.CAMPFIRE) {
                room.initAltars();
            }
        } catch (Exception e) {
            // 忽略祭坛初始化异常
        }
    }

    public void triggerTeleport(Room room) {
        if (!room.isTeleportRoom()) {
            return;
        }
        Set<Room> allRooms = new HashSet<>();
        Queue<Room> queue = new LinkedList<>();
        queue.add(currentRoom);
        while (!queue.isEmpty()) {
            Room r = queue.poll();
            if (r == null) continue; // defensive
            if (allRooms.add(r)) {
                // add only non-null neighbor rooms
                try {
                    for (Room nr : r.getExits().values()) {
                        if (nr != null) queue.add(nr);
                    }
                } catch (Exception e) {
                    // ignore malformed exits
                }
            }
        }
        allRooms.remove(room);
        List<Room> roomList = new ArrayList<>(allRooms);
        if (!roomList.isEmpty()) {
            Random random = new Random();
            this.currentRoom = roomList.get(random.nextInt(roomList.size()));
            if (player != null) {
                player.setCurrentRoom(this.currentRoom);
            }
        }
    }

    public void reset() {
        gameOver = false;
        createRooms();
        player = new Player("冒险者", currentRoom);
        player.getBag().setOwner(player);
        // 初始化测试物品：生命浆果 x5 + 魔力浆果 x5
        for (int i = 0; i < 5; i++) {
            player.getBag().addItem(new Item("生命浆果", "回复生命的红色浆果"));
            player.getBag().addItem(new Item("魔力浆果", "回复魔力的蓝色浆果"));
        }
        if (player.getMoney() != null) {
            player.getMoney().reset();
        }
    }

    /** 普通怪物奖励 */
    private static final int REWARD_NORMAL = 15;
    /** 精英怪物奖励 */
    private static final int REWARD_ELITE = 35;
    /** Boss 奖励 */
    private static final int REWARD_BOSS = 100;

    private static final Random DROP_RND = new Random();

    /**
     * 处理怪物死亡后的掉落和货币奖励。
     * @param m     被击败的怪物
     * @param dropX 掉落物 X 坐标
     * @param dropY 掉落物 Y 坐标
     * @return 描述掉落和奖励的文本
     */
    public String processMonsterDrop(Monster m, int dropX, int dropY) {
        StringBuilder sb = new StringBuilder();

        // ---- Boss 掉落浆果 ----
        if (m.getType() == Monster.TYPE_BOSS) {
            String dropName = DROP_RND.nextBoolean() ? "生命浆果" : "魔力浆果";
            DroppedItem drop = new DroppedItem(dropName, dropX, dropY);
            Room room = getCurrentRoom();
            if (room != null) {
                room.addDroppedItem(drop);
            }
            sb.append("\n").append(m.getName()).append("掉落了 ").append(dropName).append("！");
        }

        // ---- 普通怪物 30% 概率掉落药水 ----
        if (m.getType() == Monster.TYPE_NORMAL && DROP_RND.nextInt(100) < 30) {
            DroppedItem drop = new DroppedItem("药水", dropX, dropY);
            Room room = getCurrentRoom();
            if (room != null) {
                room.addDroppedItem(drop);
            }
            sb.append("\n").append(m.getName()).append("掉落了药水！");
        }

        // ---- 精英怪物 50% 概率掉落装备或饰品 ----
        if (m.getType() == Monster.TYPE_ELITE && DROP_RND.nextInt(100) < 50) {
            String[] eliteDrops = {"铁剑", "铁盾", "暗影披风", "生命戒指", "元素项链"};
            String dropName = eliteDrops[DROP_RND.nextInt(eliteDrops.length)];
            DroppedItem drop = new DroppedItem(dropName, dropX, dropY);
            Room room = getCurrentRoom();
            if (room != null) {
                room.addDroppedItem(drop);
            }
            sb.append("\n").append(m.getName()).append("掉落了 ").append(dropName).append("！");
        }

        // ---- 货币奖励 ----
        int reward = switch (m.getType()) {
            case Monster.TYPE_NORMAL -> REWARD_NORMAL;
            case Monster.TYPE_ELITE  -> REWARD_ELITE;
            case Monster.TYPE_BOSS   -> REWARD_BOSS;
            default -> 0;
        };

        if (reward > 0 && player != null && player.getMoney() != null) {
            player.getMoney().add(reward);
            sb.append("\n获得了 $").append(reward).append(" 货币！(余额: $")
              .append(player.getMoney().getAmount()).append(")");
        }

        return sb.toString();
    }

    /**
     * 对指定怪物造成一次玩家物理伤害。如果怪物死亡，自动移除并处理掉落。
     * 此方法由 AttackCommand（旧版命令行）和 GameService.performAttack（新版 API）共享。
     *
     * @param targetName 怪物名称
     * @param dropX 掉落物 X 坐标（怪物死亡时使用）
     * @param dropY 掉落物 Y 坐标（怪物死亡时使用）
     * @return 攻击结果描述文本，若怪物不存在或不可攻击则返回错误信息
     */
    public String attackMonster(String targetName, int dropX, int dropY) {
        Room current = getCurrentRoom();
        Player p = getPlayer();
        if (p == null) return "玩家不存在";
        if (current == null) return "当前不在任何房间";
        if (targetName == null || targetName.isBlank()) return "要攻击谁？请指定怪物名称";

        Monster m = current.getMonster(targetName);
        if (m == null) return "这里没有叫 '" + targetName + "' 的怪物。";
        if (m.isExploding()) return m.getName() + " 正在自爆倒计时中，无法被攻击。";

        int dmg = Math.max(1, p.getEffectiveAttack());
        m.takeDamage(dmg);
        StringBuilder sb = new StringBuilder();
        sb.append("你对 ").append(m.getName()).append(" 造成了 ").append(dmg).append(" 点伤害。");

        if (!m.isAlive()) {
            current.removeMonster(m);
            sb.append("\n你击败了 ").append(m.getName()).append("！");
            sb.append(processMonsterDrop(m, dropX, dropY));
        }

        return sb.toString();
    }

    /**
     * 蓄力攻击：对指定怪物造成两次玩家当前物攻150%的物理伤害。
     * 若怪物在两次伤害之间死亡，第一次击倒即停止并处理掉落。
     *
     * @param targetName 怪物名称
     * @param dropX 掉落物 X 坐标
     * @param dropY 掉落物 Y 坐标
     * @return 攻击结果描述文本
     */
    public String chargedAttackMonster(String targetName, int dropX, int dropY) {
        Room current = getCurrentRoom();
        Player p = getPlayer();
        if (p == null) return "玩家不存在";
        if (current == null) return "当前不在任何房间";
        if (targetName == null || targetName.isBlank()) return "要攻击谁？请指定怪物名称";

        Monster m = current.getMonster(targetName);
        if (m == null) return "这里没有叫 '" + targetName + "' 的怪物。";
        if (m.isExploding()) return m.getName() + " 正在自爆倒计时中，无法被攻击。";

        // 150% 物攻，两次伤害
        int baseDmg = Math.max(1, p.getEffectiveAttack());
        int chargedDmg = Math.max(1, (int) Math.round(baseDmg * 1.5));
        StringBuilder sb = new StringBuilder();

        for (int hit = 1; hit <= 2; hit++) {
            m.takeDamage(chargedDmg);
            if (hit == 1) {
                sb.append("蓄力攻击第1段对 ").append(m.getName()).append(" 造成了 ").append(chargedDmg).append(" 点伤害。");
            } else {
                sb.append("\n蓄力攻击第2段对 ").append(m.getName()).append(" 造成了 ").append(chargedDmg).append(" 点伤害。");
            }
            if (!m.isAlive()) {
                current.removeMonster(m);
                sb.append("\n你击败了 ").append(m.getName()).append("！");
                sb.append(processMonsterDrop(m, dropX, dropY));
                break;
            }
        }

        return sb.toString();
    }

    /**
     * 返回全地图数据，供前端小地图使用
     * 格式：{ rooms: [{name, exits:{方向:邻居名}, roomType}], startRoomName: "..." }
     */
    public Map<String, Object> getFullMap() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (Room room : allRooms) {
            Map<String, Object> r = new HashMap<>();
            r.put("name", room.getName());
            // 将出口 Map<String, Room> 转为 Map<String, String>，避免循环引用
            Map<String, String> exits = new HashMap<>();
            for (Map.Entry<String, Room> e : room.getExits().entrySet()) {
                exits.put(e.getKey(), e.getValue().getName());
            }
            r.put("exits", exits);
            r.put("roomType", room.getRoomType() != null ? room.getRoomType().name() : "NORMAL_MONSTER");
            roomList.add(r);
        }
        result.put("rooms", roomList);
        result.put("startRoomName", currentRoom.getName());
        return result;
    }

}
