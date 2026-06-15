package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.Room;
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
        // 记录房间历史（供back命令使用）
        game.addRoomHistory(currentRoom);
        game.setCurrentRoom(nextRoom);

        // 如果房间当前没有怪物，则按概率生成怪物（进入时刷怪）
        try {
            if (nextRoom.getMonsters() == null || nextRoom.getMonsters().isEmpty()) {
                Random rnd = new Random();
                // 40% 概率在进入时生成怪物
                if (rnd.nextDouble() < 0.40) {
                    String[] monsterNames = new String[]{"哥布林", "史莱姆", "骷髅", "狼人", "食人魔"};
                    int count = 1 + rnd.nextInt(2); // 1-2 个怪物
                    for (int i = 0; i < count; i++) {
                        String base = monsterNames[rnd.nextInt(monsterNames.length)];
                        String mname = base + "#" + (rnd.nextInt(9000) + 1000);
                        String desc = "一个可怕的" + base;
                        int hp = 8 + rnd.nextInt(23); // 8..30
                        int attack = 1 + rnd.nextInt(8); // 1..8
                        Monster m = new Monster(mname, desc, hp, attack);
                        nextRoom.addMonster(m);
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
}