package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Room;
import cn.edu.whut.sept.zuul.model.RoomType;
import cn.edu.whut.sept.zuul.model.Monster;
import java.util.Random;

/**
 * 移动命令：go + 方向
 */
public class GoCommand implements Command {
    private Game game;
    private String direction;

    public GoCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        if (direction == null) {
            return GameResponse.error("要去哪儿？请指定方向（east, south, west, north）");
        }

        Room currentRoom = game.getCurrentRoom();
        Room nextRoom = currentRoom.getExits().get(direction);
        if (nextRoom == null) {
            return GameResponse.error("该方向没有门：" + direction + "！");
        }

        // 触发传输房间逻辑
        game.triggerTeleport(nextRoom);
        game.setCurrentRoom(nextRoom);

        // 如果房间当前没有怪物，且房间为怪物房，则生成怪物
        try {
            if (nextRoom.getMonsters() == null || nextRoom.getMonsters().isEmpty()) {
                RoomType roomType = nextRoom.getRoomType();
                if (roomType != null) {
                    switch (roomType) {
                        case BOSS:
                            spawnBossMonsters(nextRoom);
                            break;
                        case ELITE_MONSTER:
                            spawnEliteMonsters(nextRoom);
                            break;
                        case NORMAL_MONSTER:
                            spawnNormalMonsters(nextRoom);
                            break;
                        default:
                            // 非怪物房间（开始大厅、商店、奇遇、篝火等）不生成怪物
                            break;
                    }
                }
            }
        } catch (Exception e) {
            // 不要因为刷怪导致移动失败，记录或忽略异常
        }

        return GameResponse.success("你移动到了 " + nextRoom.getName(), nextRoom.getFullInfo());
    }

    @Override
    public void setParams(String... params) {
        if (params.length > 0) {
            this.direction = params[0];
        }
    }

    /**
     * Boss房间怪物生成：单个强力Boss怪物，属性远高于普通怪物
     */
    private void spawnBossMonsters(Room room) {
        Random rnd = new Random();
        String[] bossNames = new String[]{"巨龙", "恶魔领主", "巫妖王", "巨型石魔像", "暗影之王"};
        String base = bossNames[rnd.nextInt(bossNames.length)];
        String mname = base + "#" + (rnd.nextInt(9000) + 1000);
        String desc = "恐怖的" + base + "——最终挑战";
        int hp = 100 + rnd.nextInt(51);   // 100-150
        int attack = 15 + rnd.nextInt(11); // 15-25
        Monster m = new Monster(mname, desc, hp, attack, Monster.TYPE_BOSS);
        room.addMonster(m);
    }

    /**
     * 精英怪物房间生成：1-2只精英怪物，属性高于普通怪物
     */
    private void spawnEliteMonsters(Room room) {
        Random rnd = new Random();
        String[] eliteNames = new String[]{"暗影骑士", "地狱犬", "石像鬼", "暗黑法师", "巨型蜘蛛"};
        int count = 1 + rnd.nextInt(2); // 1-2 个怪物
        for (int i = 0; i < count; i++) {
            String base = eliteNames[rnd.nextInt(eliteNames.length)];
            String mname = base + "#" + (rnd.nextInt(9000) + 1000);
            String desc = "强大的" + base;
            int hp = 35 + rnd.nextInt(26);    // 35-60
            int attack = 8 + rnd.nextInt(8);   // 8-15
            Monster m = new Monster(mname, desc, hp, attack, Monster.TYPE_ELITE);
            room.addMonster(m);
        }
    }

    /**
     * 普通怪物房间生成：1-2只普通怪物，属性较低
     */
    private void spawnNormalMonsters(Room room) {
        Random rnd = new Random();
        String[] monsterNames = new String[]{"哥布林", "史莱姆", "骷髅", "狼人", "食人魔"};
        int count = 1 + rnd.nextInt(2); // 1-2 个怪物
        for (int i = 0; i < count; i++) {
            String base = monsterNames[rnd.nextInt(monsterNames.length)];
            String mname = base + "#" + (rnd.nextInt(9000) + 1000);
            String desc = "一个可怕的" + base;
            int hp = 8 + rnd.nextInt(23);   // 8-30
            int attack = 1 + rnd.nextInt(8); // 1-8
            Monster m = new Monster(mname, desc, hp, attack, Monster.TYPE_NORMAL);
            room.addMonster(m);
        }
    }
}