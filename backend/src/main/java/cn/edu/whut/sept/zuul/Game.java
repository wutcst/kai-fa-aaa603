package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.Item;
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
    private List<Room> roomHistory;
    private boolean gameOver;

    public Game() {
        roomHistory = new ArrayList<>();
        gameOver = false;
        createRooms();
        player = new Player("冒险者", currentRoom);
    }

    private void createRooms() {
        Room outside = new Room("教学楼外", "大学主楼外");
        Room lobby = new Room("大厅", "建筑大厅");
        Room lab = new Room("实验室", "一个有着奇怪设备的实验室");
        Room office = new Room("办公室", "教授的办公室，满是书架");
        Room library = new Room("图书馆", "安静的图书馆，藏书丰富");

        outside.setExit("east", lobby);
        lobby.setExit("west", outside);
        lobby.setExit("north", lab);
        lab.setExit("south", lobby);
        lab.setExit("east", office);
        office.setExit("west", lab);
        lobby.setExit("south", library);
        library.setExit("north", lobby);

        lab.setTeleportRoom(true);

        outside.addItem(new Item("rock", "一块很重的石头", 2.5));
        lab.addItem(new Item("beaker", "一个玻璃烧杯", 0.5));
        office.addItem(new Item("book", "一本编程书", 1.0));
        library.addItem(new Item("pen", "一支金属笔", 0.2));
        library.addItem(new Item("cookie", "一块闪亮的魔法饼干", 0.1));

        currentRoom = outside;
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
            if (allRooms.add(r)) {
                queue.addAll(r.getExits().values());
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

    public void addRoomHistory(Room room) {
        roomHistory.add(room);
    }

    public Room getPreviousRoom() {
        if (roomHistory.isEmpty()) {
            return null;
        }
        return roomHistory.get(roomHistory.size() - 1);
    }

    public void removeLastRoomHistory() {
        if (!roomHistory.isEmpty()) {
            roomHistory.remove(roomHistory.size() - 1);
        }
    }

    public void reset() {
        roomHistory.clear();
        gameOver = false;
        createRooms();
        player = new Player("冒险者", currentRoom);
    }
}