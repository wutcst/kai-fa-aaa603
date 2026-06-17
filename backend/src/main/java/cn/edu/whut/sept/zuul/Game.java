package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.Item;
import cn.edu.whut.sept.zuul.model.Player;
import cn.edu.whut.sept.zuul.model.Room;
import cn.edu.whut.sept.zuul.model.Monster;
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
        // 初始化测试物品：生命浆果 x5 + 魔力浆果 x5
        for (int i = 0; i < 5; i++) {
            player.getBag().addItem(new Item("生命浆果", "回复生命的红色浆果"));
            player.getBag().addItem(new Item("魔力浆果", "回复魔力的蓝色浆果"));
        }
        if (player.getMoney() != null) {
            player.getMoney().reset();
        }
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
