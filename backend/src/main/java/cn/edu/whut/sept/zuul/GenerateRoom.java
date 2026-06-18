package cn.edu.whut.sept.zuul;

import cn.edu.whut.sept.zuul.model.Room;
import cn.edu.whut.sept.zuul.model.RoomType;
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

    /**
     * 使用指定种子生成地图（存档恢复用）
     */
    public static GeneratedMap generate(int minRooms, int maxRooms, long seed) {
        if (minRooms < 1) minRooms = 1;
        if (maxRooms < minRooms) maxRooms = minRooms;
        Random rnd = new Random(seed);
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

        List<Room> allRooms = new ArrayList<>(grid.values());

        // ========== 房间分类 ==========
        classifyRooms(allRooms, start, rnd);

        return new GeneratedMap(allRooms, start);
    }

    /**
     * 对生成的所有房间进行分类标注。
     *
     * 分类规则：
     * - 开始大厅（1个）：startRoom，玩家初始生成房间
     * - 商店房间（1个）
     * - 奇遇房间（2个，若总房间数 > 12 则为 3 个）
     * - 篝火（休息）房间（2个）
     * - Boss（领袖）房间（1个）：从仅有一个出口的尽头房间中选取
     * - 精英怪物房间（2个）
     * - 其余均为普通怪物房间
     */
    private static void classifyRooms(List<Room> allRooms, Room startRoom, Random rnd) {
        int totalRooms = allRooms.size();

        // 1. 开始大厅
        startRoom.setRoomType(RoomType.START_HALL);

        // 构建候选池（排除开始大厅）
        List<Room> candidatePool = new ArrayList<>();
        for (Room r : allRooms) {
            if (r != startRoom) {
                candidatePool.add(r);
            }
        }

        // 2. Boss房间：从仅有一个出口（尽头房间）的候选房间中选取
        List<Room> deadEnds = new ArrayList<>();
        for (Room r : candidatePool) {
            int exitCount = r.getExitCount();
            if (exitCount == 1) {
                deadEnds.add(r);
            }
        }

        Room bossRoom = null;
        if (!deadEnds.isEmpty()) {
            bossRoom = deadEnds.get(rnd.nextInt(deadEnds.size()));
            bossRoom.setRoomType(RoomType.BOSS);
            candidatePool.remove(bossRoom);
        }

        // 如果没有任何尽头房间（极端情况），则选一个出口最少的房间
        if (bossRoom == null && !candidatePool.isEmpty()) {
            candidatePool.sort(Comparator.comparingInt(Room::getExitCount));
            bossRoom = candidatePool.get(0);
            bossRoom.setRoomType(RoomType.BOSS);
            candidatePool.remove(bossRoom);
        }

        // 3. 精英怪物房间（2个）
        int eliteCount = 2;
        for (int i = 0; i < eliteCount && !candidatePool.isEmpty(); i++) {
            int idx = rnd.nextInt(candidatePool.size());
            Room eliteRoom = candidatePool.remove(idx);
            eliteRoom.setRoomType(RoomType.ELITE_MONSTER);
        }

        // 4. 商店房间（1个）
        if (!candidatePool.isEmpty()) {
            int idx = rnd.nextInt(candidatePool.size());
            Room shop = candidatePool.remove(idx);
            shop.setRoomType(RoomType.SHOP);
        }

        // 5. 奇遇房间（2个，若房间数 > 12 则为 3 个）
        int encounterCount = (totalRooms > 12) ? 3 : 2;
        for (int i = 0; i < encounterCount && !candidatePool.isEmpty(); i++) {
            int idx = rnd.nextInt(candidatePool.size());
            Room enc = candidatePool.remove(idx);
            enc.setRoomType(RoomType.ENCOUNTER);
        }

        // 6. 篝火休息房间（2个）
        int campfireCount = 2;
        for (int i = 0; i < campfireCount && !candidatePool.isEmpty(); i++) {
            int idx = rnd.nextInt(candidatePool.size());
            Room camp = candidatePool.remove(idx);
            camp.setRoomType(RoomType.CAMPFIRE);
        }

        // 7. 剩余候选房间均为普通怪物房间（默认已为 NORMAL_MONSTER，无需额外操作）
        // candidatePool 中剩余的房间保持默认的 RoomType.NORMAL_MONSTER
        
        // 打印分类结果（调试用）
        System.out.println("[GenerateRoom] Room classification (" + totalRooms + " rooms total):");
        for (Room r : allRooms) {
            System.out.println("  " + r.getName() + " -> " + r.getRoomType().getDisplayName()
                    + " (exits: " + r.getExitCount() + ")");
        }
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
