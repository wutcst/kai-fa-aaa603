package cn.edu.whut.sept.zuul.model;

import java.util.*;

/**
 * 博学祭坛增益枚举：定义8种增益类型及其效果。
 * 当玩家与博学祭坛交互时，从全部8种增益中随机选出3种供玩家选择。
 */
public enum WisdomBoon {
    /** 提高30%血量上限 */
    ENDURANCE("坚忍", "提高30%血量上限") {
        @Override
        public void apply(Player player) {
            int bonus = (int) Math.ceil(player.getMaxHp() * 0.30);
            player.setMaxHp(player.getMaxHp() + bonus);
            player.setHp(Math.min(player.getHp() + bonus, player.getMaxHp()));
        }
    },
    /** 提高50%魔力上限 */
    VASTNESS("浩瀚", "提高50%魔力上限") {
        @Override
        public void apply(Player player) {
            int bonus = (int) Math.ceil(player.getMaxMp() * 0.50);
            player.setMaxMp(player.getMaxMp() + bonus);
            player.setMp(Math.min(player.getMp() + bonus, player.getMaxMp()));
        }
    },
    /** 提高15点物防 */
    PREPARATION("整备", "提高15点物防") {
        @Override
        public void apply(Player player) {
            player.setDefense(player.getDefense() + 15);
        }
    },
    /** 提高25点魔抗 */
    GUARDIAN("守护", "提高25点魔抗") {
        @Override
        public void apply(Player player) {
            player.setMagicResist(Math.min(100, player.getMagicResist() + 25));
        }
    },
    /** 提高15点物攻 */
    VIGOR("强健", "提高15点物攻") {
        @Override
        public void apply(Player player) {
            player.setAttack(player.getAttack() + 15);
        }
    },
    /** 提高25点魔攻 */
    ERUDITION("博学", "提高25点魔攻") {
        @Override
        public void apply(Player player) {
            player.setMagicAttack(player.getMagicAttack() + 25);
        }
    },
    /** 提高50点移速 */
    AGILITY("敏捷", "提高50点移速") {
        @Override
        public void apply(Player player) {
            player.setSpeed(player.getSpeed() + 50);
        }
    },
    /** 提高25点闪避率 */
    GRACE("灵动", "提高25点闪避率") {
        @Override
        public void apply(Player player) {
            player.setDodge(Math.min(100, player.getDodge() + 25));
        }
    };

    private final String displayName;
    private final String description;

    WisdomBoon(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /**
     * 将此增益效果应用到指定玩家身上。
     * @param player 目标玩家
     */
    public abstract void apply(Player player);

    /**
     * 从全部8种增益中随机选取指定数量（不重复）。
     * @param count 需要选取的数量
     * @return 随机选取的增益列表
     */
    public static List<WisdomBoon> randomPick(int count) {
        List<WisdomBoon> all = new ArrayList<>(List.of(values()));
        Collections.shuffle(all, new Random());
        return all.subList(0, Math.min(count, all.size()));
    }

    /**
     * 转为前端可用的 Map 格式。
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("name", this.name());
        map.put("displayName", this.displayName);
        map.put("description", this.description);
        return map;
    }
}
