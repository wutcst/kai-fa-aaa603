package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.Item;
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
    private Room currentRoom;        // 当前房间
    private List<Room> roomHistory;  // 房间历史（供back命令）
    private boolean gameOver;        // 游戏是否结束

    public Game() {
        roomHistory = new ArrayList<>();
        gameOver = false;
        createRooms(); // 初始化房间
    }

    /**
     * 初始化房间地图（包含物品、传输房间标记）
     */
    private void createRooms() {
        // 创建房间
        Room outside = new Room("Outside Building", "Outside the main building of the university");
        Room lobby = new Room("Lobby", "The lobby of the building");
        Room lab = new Room("Lab", "A science lab with strange equipment");
        Room office = new Room("Office", "A professor's office with bookshelves");
        Room library = new Room("Library", "A quiet library with many books");

        // 设置出口
        outside.setExit("east", lobby);
        lobby.setExit("west", outside);
        lobby.setExit("north", lab);
        lab.setExit("south", lobby);
        lab.setExit("east", office);
        office.setExit("west", lab);
        lobby.setExit("south", library);
        library.setExit("north", lobby);

        // 标记传输房间（lab）
        lab.setTeleportRoom(true);

        // 给房间添加物品
        outside.addItem(new Item("rock", "A heavy rock", 2.5));
        lab.addItem(new Item("beaker", "A glass beaker", 0.5));
        office.addItem(new Item("book", "A programming book", 1.0));
        library.addItem(new Item("pen", "A metal pen", 0.2));

        // 初始房间
        currentRoom = outside;
    }

    /**
     * 触发传输房间逻辑：进入lab时随机传送到其他房间
     */
    public void triggerTeleport(Room room) {
        if (!room.isTeleportRoom()) {
            return;
        }

        // 获取所有房间（排除当前lab）
        Set<Room> allRooms = new HashSet<>();
        Queue<Room> queue = new LinkedList<>();
        queue.add(currentRoom);
        while (!queue.isEmpty()) {
            Room r = queue.poll();
            if (allRooms.add(r)) {
                queue.addAll(r.getExits().values());
            }
        }
        allRooms.remove(room); // 排除当前lab

        // 随机选择一个房间传送
        List<Room> roomList = new ArrayList<>(allRooms);
        if (!roomList.isEmpty()) {
            Random random = new Random();
            this.currentRoom = roomList.get(random.nextInt(roomList.size()));
        }
    }

    /**
     * 添加房间到历史记录
     */
    public void addRoomHistory(Room room) {
        roomHistory.add(room);
    }

    /**
     * 获取上一个房间（back命令）
     */
    public Room getPreviousRoom() {
        if (roomHistory.isEmpty()) {
            return null;
        }
        return roomHistory.get(roomHistory.size() - 1);
    }

    /**
     * 移除最后一条房间历史
     */
    public void removeLastRoomHistory() {
        if (!roomHistory.isEmpty()) {
            roomHistory.remove(roomHistory.size() - 1);
        }
    }

    /**
     * 重置游戏
     */
    public void reset() {
        roomHistory.clear();
        gameOver = false;
        createRooms();
    }
}