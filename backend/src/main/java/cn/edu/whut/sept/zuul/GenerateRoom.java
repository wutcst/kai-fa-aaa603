package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.Room;
import java.util.*;

public class GenerateRoom {

    public static class GeneratedMap {
        public final List<Room> rooms;
        public final Room startRoom;

        public GeneratedMap(List<Room> rooms, Room startRoom) {
            this.rooms = rooms;
            this.startRoom = startRoom;
        }
    }

    private static final Map<String, int[]> DIR_VECS = Map.of(
            "north", new int[]{0, -1},
            "south", new int[]{0, 1},
            "east",  new int[]{1, 0},
            "west",  new int[]{-1, 0}
    );

    public static GeneratedMap generate(int minRooms, int maxRooms) {
        if (minRooms < 1) minRooms = 1;
        if (maxRooms < minRooms) maxRooms = minRooms;
        Random rnd = new Random();
        int total = minRooms + rnd.nextInt(maxRooms - minRooms + 1);

        // grid: coordinate key -> Room
        Map<String, Room> grid = new HashMap<>();
        // roomPos: Room -> its coordinate
        Map<Room, int[]> roomPos = new HashMap<>();
        int counter = 0;

        // create start room at (0,0)
        Room start = new Room("房间" + (counter++), "起点房间");
        grid.put("0,0", start);
        roomPos.put(start, new int[]{0, 0});

        // ensure start has neighbors in all four directions
        for (Map.Entry<String, int[]> e : DIR_VECS.entrySet()) {
            String dir = e.getKey();
            int[] vec = e.getValue();
            int nx = vec[0];
            int ny = vec[1];
            String key = nx + "," + ny;
            Room neighbor = new Room("房间" + (counter++), "起始邻居(" + dir + ")");
            grid.put(key, neighbor);
            roomPos.put(neighbor, new int[]{nx, ny});
            start.setExit(dir, neighbor);
            neighbor.setExit(opposite(dir), start);
        }

        // grow the map until we have enough rooms
        while (grid.size() < total) {
            // collect all placed rooms that have at least one free adjacent cell
            List<Room> candidates = new ArrayList<>();
            for (Map.Entry<Room, int[]> entry : roomPos.entrySet()) {
                Room room = entry.getKey();
                int x = entry.getValue()[0];
                int y = entry.getValue()[1];
                for (int[] v : DIR_VECS.values()) {
                    String nk = (x + v[0]) + "," + (y + v[1]);
                    if (!grid.containsKey(nk)) {
                        candidates.add(room);
                        break;
                    }
                }
            }
            if (candidates.isEmpty()) break; // no room to expand (should not happen)

            Room base = candidates.get(rnd.nextInt(candidates.size()));
            int[] baseCoord = roomPos.get(base);
            // find free directions from base
            List<String> freeDirs = new ArrayList<>();
            for (Map.Entry<String, int[]> e : DIR_VECS.entrySet()) {
                int nx = baseCoord[0] + e.getValue()[0];
                int ny = baseCoord[1] + e.getValue()[1];
                if (!grid.containsKey(nx + "," + ny)) {
                    freeDirs.add(e.getKey());
                }
            }
            if (freeDirs.isEmpty()) continue; // should not happen

            String chosenDir = freeDirs.get(rnd.nextInt(freeDirs.size()));
            int[] vec = DIR_VECS.get(chosenDir);
            int newX = baseCoord[0] + vec[0];
            int newY = baseCoord[1] + vec[1];
            String newKey = newX + "," + newY;

            Room newRoom = new Room("房间" + (counter++), "随机生成的房间");
            grid.put(newKey, newRoom);
            roomPos.put(newRoom, new int[]{newX, newY});

            base.setExit(chosenDir, newRoom);
            newRoom.setExit(opposite(chosenDir), base);
        }

        // ensure all exits are bidirectional (safety)
        for (Room room : grid.values()) {
            Map<String, Room> exits = room.getExits();
            if (exits == null) continue;
            for (Map.Entry<String, Room> exitEntry : new ArrayList<>(exits.entrySet())) {
                String dir = exitEntry.getKey();
                Room neighbor = exitEntry.getValue();
                if (neighbor == null) continue;
                String opp = opposite(dir);
                Map<String, Room> nExits = neighbor.getExits();
                if (nExits == null || nExits.get(opp) != room) {
                    neighbor.setExit(opp, room);
                }
            }
        }

        return new GeneratedMap(new ArrayList<>(grid.values()), start);
    }

    private static String opposite(String dir) {
        switch (dir) {
            case "north": return "south";
            case "south": return "north";
            case "east": return "west";
            case "west": return "east";
            default: return "";
        }
    }
}