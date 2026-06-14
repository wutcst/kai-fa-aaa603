package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.Room;
import java.util.*;

/**
 * 生成一个随机连通地图（网格），由若干房间组成，连接仅限于 north/south/east/west。
 * 要求：起始房间（坐标0,0）必须在四个方向上都有房间连接；总房间数在 min..max 之间
 */
public class GenerateRoom {

    public static class GeneratedMap {
        public final List<Room> rooms;
        public final Room startRoom;

        public GeneratedMap(List<Room> rooms, Room startRoom) {
            this.rooms = rooms;
            this.startRoom = startRoom;
        }
    }

    private static final Map<String, int[]> DIR_VECS = new LinkedHashMap<>();
    static {
        DIR_VECS.put("north", new int[]{0, -1});
        DIR_VECS.put("south", new int[]{0, 1});
        DIR_VECS.put("east", new int[]{1, 0});
        DIR_VECS.put("west", new int[]{-1, 0});
    }

    // generate between minRooms and maxRooms rooms (inclusive)
    public static GeneratedMap generate(int minRooms, int maxRooms) {
        if (minRooms < 1) minRooms = 1;
        if (maxRooms < minRooms) maxRooms = minRooms;
        Random rnd = new Random();
        int total = minRooms + rnd.nextInt(maxRooms - minRooms + 1);

        // grid keyed by "x,y"
        Map<String, Room> grid = new HashMap<>();
        Map<String, int[]> coordMap = new HashMap<>();

        // helper
        final java.util.concurrent.atomic.AtomicInteger idx = new java.util.concurrent.atomic.AtomicInteger(0);

        // create start room at (0,0)
        Room start = new Room("房间" + idx.getAndIncrement(), "一个随机生成的房间") ;
        grid.put("0,0", start);
        coordMap.put("0,0", new int[]{0,0});

        // ensure start has 4 neighbors: create rooms at NESW
        for (Map.Entry<String,int[]> e : DIR_VECS.entrySet()) {
            String dir = e.getKey();
            int[] vec = e.getValue();
            int nx = 0 + vec[0];
            int ny = 0 + vec[1];
            String k = nx + "," + ny;
            Room r = new Room("房间" + idx.getAndIncrement(), "连接到起始房间的房间（" + dir + "）");
            grid.put(k, r);
            coordMap.put(k, new int[]{nx, ny});
            start.setExit(dir, r);
            // opposite link
            String opp = opposite(dir);
            r.setExit(opp, start);
        }

        // now we have 5 rooms
        // dynamically add rooms by attaching to random existing rooms that have free adjacent cells
        while (grid.size() < total) {
            // build list of candidate cells that have at least one free neighbor
            List<int[]> candidates = new ArrayList<>();
            for (int[] c : coordMap.values()) {
                int x = c[0], y = c[1];
                boolean hasFree = false;
                for (int[] v : DIR_VECS.values()) {
                    String kk = (x + v[0]) + "," + (y + v[1]);
                    if (!grid.containsKey(kk)) { hasFree = true; break; }
                }
                if (hasFree) candidates.add(new int[]{x,y});
            }
            if (candidates.isEmpty()) break; // no space left

            int[] base = candidates.get(rnd.nextInt(candidates.size()));
            // compute free directions from base
            List<String> freeDirs = new ArrayList<>();
            for (Map.Entry<String,int[]> e : DIR_VECS.entrySet()) {
                int nx = base[0] + e.getValue()[0];
                int ny = base[1] + e.getValue()[1];
                String kk = nx + "," + ny;
                if (!grid.containsKey(kk)) freeDirs.add(e.getKey());
            }
            if (freeDirs.isEmpty()) continue;
            String chosenDir = freeDirs.get(rnd.nextInt(freeDirs.size()));
            int[] vec = DIR_VECS.get(chosenDir);
            int newx = base[0] + vec[0];
            int newy = base[1] + vec[1];
            String newKey = newx + "," + newy;
            Room newRoom = new Room("房间" + idx.getAndIncrement(), "随机生成的房间") ;
            grid.put(newKey, newRoom);
            coordMap.put(newKey, new int[]{newx,newy});
            // link both ways
            Room baseRoom = grid.get(base[0] + "," + base[1]);
            baseRoom.setExit(chosenDir, newRoom);
            newRoom.setExit(opposite(chosenDir), baseRoom);
        }

        List<Room> result = new ArrayList<>(grid.values());
        // Safety pass: ensure all exits are bidirectional (if A->B exists, ensure B->A exists)
        for (Map.Entry<String, Room> entry : new ArrayList<>(grid.entrySet())) {
            Room r = entry.getValue();
            if (r.getExits() == null) continue;
            for (String dir : new ArrayList<>(r.getExits().keySet())) {
                Room nb = r.getExits().get(dir);
                if (nb == null) continue;
                String opp = opposite(dir);
                // if neighbor doesn't point back, fix it
                if (nb.getExits() == null || !nb.getExits().containsKey(opp) || nb.getExits().get(opp) != r) {
                    nb.setExit(opp, r);
                }
                // if neighbor is not present in grid (shouldn't happen) add it
                boolean found = false;
                for (Map.Entry<String, Room> e2 : grid.entrySet()) {
                    if (e2.getValue() == nb) { found = true; break; }
                }
                if (!found) {
                    // place neighbor at expected coordinate if base coord known
                    // try to determine coordinate of r
                    int[] baseCoord = coordMap.getOrDefault(getRoomKey(grid, r), new int[]{0,0});
                    int[] vec = DIR_VECS.get(dir);
                    int nx = baseCoord[0] + vec[0];
                    int ny = baseCoord[1] + vec[1];
                    String nk = nx + "," + ny;
                    grid.put(nk, nb);
                    coordMap.put(nk, new int[]{nx, ny});
                }
            }
        }

        // Ensure start has explicit neighbors in all four directions (defensive)
        for (Map.Entry<String,int[]> e : DIR_VECS.entrySet()) {
            String dir = e.getKey();
            if (start.getExits() == null || !start.getExits().containsKey(dir) || start.getExits().get(dir) == null) {
                int nx = e.getValue()[0];
                int ny = e.getValue()[1];
                String k = nx + "," + ny;
                if (!grid.containsKey(k)) {
                    Room r = new Room("房间" + idx.getAndIncrement(), "修复生成的邻居（" + dir + "）");
                    grid.put(k, r);
                    coordMap.put(k, new int[]{nx, ny});
                    start.setExit(dir, r);
                    r.setExit(opposite(dir), start);
                } else {
                    Room r = grid.get(k);
                    start.setExit(dir, r);
                    r.setExit(opposite(dir), start);
                }
            }
        }

        // final result list
        result = new ArrayList<>(grid.values());
        return new GeneratedMap(result, start);
    }

    // find coordinate key for a room instance in the grid map
    private static String getRoomKey(Map<String, Room> grid, Room room) {
        for (Map.Entry<String, Room> e : grid.entrySet()) {
            if (e.getValue() == room) return e.getKey();
        }
        return null;
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

