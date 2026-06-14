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
        // generate a random connected map of rooms on startup
        GenerateRoom.GeneratedMap gm = GenerateRoom.generate(10, 15);
        // pick a random teleport room among generated rooms (optional)
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