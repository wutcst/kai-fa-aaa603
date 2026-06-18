package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.Game;
import cn.edu.whut.sept.zuul.model.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 交互命令：interact <altarType>
 * 用于在篝火房间中与祭坛交互
 * 参数: heal / train / wisdom
 *
 * 在奇遇房间中与随机事件交互时：interact event（无参数或用event关键词）
 * 或：interact blacksmith <itemName>（铁匠选择目标物品）
 */
public class InteractCommand implements Command {
    private Game game;
    private String altarTypeStr;

    public InteractCommand(Game game) {
        this.game = game;
    }

    @Override
    public GameResponse execute() {
        Player player = game.getPlayer();
        if (player == null) return GameResponse.error("玩家不存在");

        Room current = game.getCurrentRoom();

        // ======== 奇遇房间随机事件交互 ========
        if (current.getRoomType() == RoomType.ENCOUNTER) {
            return handleRandomEvent(player, current);
        }

        // ======== 篝火房间祭坛交互（原有逻辑） ========
        if (current.getRoomType() != RoomType.CAMPFIRE) {
            return GameResponse.error("当前房间没有可交互的对象。");
        }

        if (altarTypeStr == null || altarTypeStr.isBlank()) {
            return GameResponse.error("请指定要交互的祭坛类型：heal（回复）、train（锻炼）、wisdom（博学）");
        }

        AltarType altarType;
        try {
            altarType = AltarType.valueOf(altarTypeStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return GameResponse.error("未知的祭坛类型：" + altarTypeStr + "。可选：heal、train、wisdom");
        }

        if (current.isAltarUsed()) {
            return GameResponse.error("这个房间的祭坛已经被使用过了，无法再次交互。");
        }

        // 初始化祭坛（如果尚未初始化）
        if (current.getAltars() == null || current.getAltars().isEmpty()) {
            current.initAltars();
        }

        Altar altar = current.activateAltar(altarType);
        if (altar == null) {
            return GameResponse.error("该祭坛无法被激活（可能已被使用）。");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你激活了").append(altar.getType().getDisplayName()).append("！\n");

        switch (altarType) {
            case HEAL:
                int healHp = (int) Math.ceil(player.getMaxHp() * 0.5);
                int healMp = (int) Math.ceil(player.getMaxMp() * 0.5);
                player.restoreHp(healHp);
                player.restoreMp(healMp);
                sb.append("祭坛散发出淡绿色的光芒，你感到体力恢复！\n");
                sb.append("恢复了 ").append(healHp).append(" 点生命值和 ").append(healMp).append(" 点魔力值。");
                break;
            case TRAIN:
                int atkBonus = (int) Math.ceil(player.getAttack() * 0.25);
                int matkBonus = (int) Math.ceil(player.getMagicAttack() * 0.25);
                int defBonus = (int) Math.ceil(player.getDefense() * 0.25);
                player.setAttack(player.getAttack() + atkBonus);
                player.setMagicAttack(player.getMagicAttack() + matkBonus);
                player.setDefense(player.getDefense() + defBonus);
                player.setMagicResist(player.getMagicResist() + 10);
                sb.append("祭坛散发出橙金色的光芒，你的身体变得更加强韧！\n");
                sb.append("攻击 +").append(atkBonus).append("，魔攻 +").append(matkBonus)
                        .append("，防御 +").append(defBonus).append("，魔抗 +10。");
                break;
            case WISDOM:
                sb.append("祭坛散发出蓝色的光芒，请从以下增益中选择一项...");
                break;
        }

        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    /**
     * 处理奇遇房间的随机事件交互
     */
    private GameResponse handleRandomEvent(Player player, Room current) {
        // 确保随机事件已初始化
        current.initRandomEvent();
        RandomEvent event = current.getRandomEvent();

        if (event == null) {
            return GameResponse.error("这个房间没有奇遇事件。");
        }

        if (event.isUsed()) {
            return GameResponse.error("这个房间的奇遇已经被触发过了。");
        }

        // 判断是否为铁匠的"选择物品"操作
        if (event.getType() == RandomEventType.BLACKSMITH && altarTypeStr != null
                && !altarTypeStr.equalsIgnoreCase("event")) {
            // 尝试从altarTypeStr解析目标物品名称（格式：blacksmith <itemName>）
            return handleBlacksmithChoose(player, current, event);
        }

        // 通用交互：根据事件类型处理
        StringBuilder sb = new StringBuilder();

        switch (event.getType()) {
            case CHEST:
                handleChest(player, sb);
                break;
            case MAIDEN:
                handleMaiden(player, sb);
                break;
            case ANGEL:
                handleAngel(player, sb);
                break;
            case BLACKSMITH:
                // 铁匠需要先展示可选物品列表
                return showBlacksmithOptions(player, current, event);
        }

        current.useRandomEvent();
        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    /**
     * 宝箱事件：增加200枚钱币
     */
    private void handleChest(Player player, StringBuilder sb) {
        int coins = 200;
        // 使用宝箱专用消息
        String[] chestDialog = {
            "你发现了一个华丽的宝箱！",
            "打开宝箱的瞬间，金色的光芒闪耀而出！",
            "「幸运的冒险者，这是属于你的财富。」——宝箱上刻着这样的文字。",
            "你获得了 " + coins + " 枚钱币！"
        };
        for (String line : chestDialog) {
            sb.append(line).append("\n");
        }
        player.getMoney().add(coins);
    }

    /**
     * 圣女事件：血回满、蓝回满
     */
    private void handleMaiden(Player player, StringBuilder sb) {
        String[] maidenDialog = {
            "一位圣洁的少女出现在你面前，她周身散发着柔和的白色光芒。",
            "圣女微笑着伸出双手，温暖的光芒笼罩了你的全身。",
            "「愿光明与你同在，勇敢的冒险者。」",
            "你的生命值和魔力值已完全恢复！"
        };
        for (String line : maidenDialog) {
            sb.append(line).append("\n");
        }
        player.setHp(player.getMaxHp());
        player.setMp(player.getMaxMp());
    }

    /**
     * 天使事件：整体属性上浮至150%，持续2分钟
     */
    private void handleAngel(Player player, StringBuilder sb) {
        String[] angelDialog = {
            "一道耀眼的光芒从天而降，一位天使降临在你面前！",
            "天使展开金色的羽翼，圣洁的光辉洒落在你身上。",
            "「勇敢的战士，接受我的祝福吧。愿神圣之力护佑你前行。」",
            "你身上浮现出淡淡的金光！在接下来的30秒内，所有属性提升至150%！"
        };
        for (String line : angelDialog) {
            sb.append(line).append("\n");
        }
        player.getStatusManager().applyAngelBuff();
    }

    /**
     * 铁匠事件：展示可选物品列表供玩家选择
     */
    private GameResponse showBlacksmithOptions(Player player, Room current, RandomEvent event) {
        StringBuilder sb = new StringBuilder();

        // 收集玩家可用的装备/饰品（不包含兜底物品）
        java.util.List<String> availableItems = getAvailableEquippableItems(player);

        // 如果玩家没有任何可锻造的物品，老铁匠一撇嘴——你太穷了
        if (availableItems.isEmpty()) {
            String[] noItemDialog = {
                "一位胡须花白的老铁匠正坐在熔炉旁，叮叮当当地敲打着什么。",
                "他看到你走来，放下手中的铁锤，上下打量了你一番。",
                "「唔…小兄弟，你身上连件像样的家伙什儿都没有，让我咋帮你强化？」",
                "「去外面搞几件装备再回来找我吧，哈哈哈！」",
                "老铁匠拍了拍你的肩膀，又回去继续敲打了。"
            };
            for (String line : noItemDialog) {
                sb.append(line).append("\n");
            }
            // 不标记为used，也不返回blacksmithItems，玩家可以之后再回来
            return GameResponse.success(sb.toString(), current.getFullInfo());
        }

        String[] blacksmithDialog = {
            "一位胡须花白的老铁匠正坐在熔炉旁，叮叮当当地敲打着什么。",
            "他看到你走来，放下手中的铁锤，露出热情的笑容。",
            "「嘿，旅行者！我看你身上有些不错的家伙什儿，需要我帮你强化一下吗？」",
            "「选一件装备或饰品，我保证让它变得更强！」"
        };
        for (String line : blacksmithDialog) {
            sb.append(line).append("\n");
        }

        sb.append("\n你可以选择以下物品进行强化：\n");
        for (int i = 0; i < availableItems.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(availableItems.get(i)).append("\n");
        }

        // 将可用物品列表存入事件的blacksmithTargetItem字段（临时存储，用逗号分隔）
        event.setBlacksmithTargetItem(String.join(",", availableItems));

        // 这里不标记为used，等待玩家选择具体物品后再使用
        // 但我们需要返回数据，所以创建一个包含事件信息和可选列表的响应
        // 注意：不调用 current.useRandomEvent()，等玩家选完再标记
        // 在返回数据中加入 blacksmithItems 列表，供前端渲染可点击按钮
        Map<String, Object> extendedData = new HashMap<>(current.getFullInfo());
        extendedData.put("blacksmithItems", availableItems);
        return GameResponse.success(sb.toString(), extendedData);
    }

    /**
     * 铁匠选择目标物品后执行强化
     */
    private GameResponse handleBlacksmithChoose(Player player, Room current, RandomEvent event) {
        String targetItemName = altarTypeStr; // 此时altarTypeStr存储的是物品名称

        // 校验物品是否可用
        java.util.List<String> availableItems = getAvailableEquippableItems(player);
        boolean valid = false;
        for (String item : availableItems) {
            if (item.equalsIgnoreCase(targetItemName)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            return GameResponse.error("你没有这件装备或饰品，请选择可用的物品：" + String.join(", ", availableItems));
        }

        // 执行强化：功能上浮150%
        StringBuilder sb = new StringBuilder();
        sb.append("铁匠接过你的「").append(targetItemName).append("」，开始仔细打磨……\n");
        sb.append("熔炉中的火焰升腾而起，铁匠挥汗如雨，敲打声铿锵有力！\n");

        // 强化的实际效果：对武器的攻击力进行增强
        String lower = targetItemName.toLowerCase();
        if (lower.contains("暗影披风")) {
            // 暗影披风已有 dodge+15, speed+20，再额外提升50%
            player.setDodge(player.getDodge() + (int) Math.round(15 * 0.5));
            player.setSpeed(player.getSpeed() + (int) Math.round(20 * 0.5));
            sb.append("暗影披风的闪避与速度属性得到强化！闪避+" ).append((int) Math.round(15 * 0.5))
              .append("，速度+").append((int) Math.round(20 * 0.5));
        } else if (lower.contains("生命戒指")) {
            // 生命戒指已有 maxHp+50，再额外提升50%
            int bonus = (int) Math.round(50 * 0.5);
            player.setMaxHp(player.getMaxHp() + bonus);
            player.setHp(Math.min(player.getHp() + bonus, player.getMaxHp()));
            sb.append("生命戒指的生命值加成得到强化！最大生命+").append(bonus);
        } else if (lower.contains("元素项链")) {
            // 元素项链已有 magicAttack+15, magicResist+20，再额外提升50%
            int atkBonus = (int) Math.round(15 * 0.5);
            int resBonus = (int) Math.round(20 * 0.5);
            player.setMagicAttack(player.getMagicAttack() + atkBonus);
            player.setMagicResist(Math.min(100, player.getMagicResist() + resBonus));
            sb.append("元素项链的魔力属性得到强化！魔攻+").append(atkBonus).append("，魔抗+").append(resBonus);
        } else {
            // 通用武器/装备强化：攻击+15%
            int atkBonus = (int) Math.ceil(player.getAttack() * 0.15);
            player.setAttack(player.getAttack() + atkBonus);
            sb.append("「").append(targetItemName).append("」被铁匠精心强化，攻击力+").append(atkBonus).append("！");
        }

        sb.append("\n铁匠满意地点点头：「好了，现在它比原来强了一半还多！」");

        // 标记事件已使用
        current.useRandomEvent();
        return GameResponse.success(sb.toString(), current.getFullInfo());
    }

    /**
     * 获取玩家背包中可被铁匠强化的装备/饰品列表。
     * 仅返回玩家实际拥有的装备/饰品（已装备或背包中的），
     * 不再提供任何兜底物品。
     */
    private java.util.List<String> getAvailableEquippableItems(Player player) {
        java.util.List<String> items = new java.util.ArrayList<>();
        // 检查已装备的饰品
        if (player.getEquippedCloak() != null) items.add(player.getEquippedCloak());
        if (player.getEquippedRing() != null) items.add(player.getEquippedRing());
        if (player.getEquippedAmulet() != null) items.add(player.getEquippedAmulet());
        // 检查背包中的装备/饰品
        for (Item bagItem : player.getBag().getInventory()) {
            String slot = Player.getItemSlot(bagItem.getName());
            if (slot != null && !items.contains(bagItem.getName())) {
                items.add(bagItem.getName());
            }
        }
        // 不再添加兜底物品——玩家没有任何装备时返回空列表
        return items;
    }

    @Override
    public void setParams(String... params) {
        if (params != null && params.length > 0) {
            this.altarTypeStr = params[0];
            // 如果参数不止一个，说明是铁匠选择物品（interact event <itemName>）
            // 此时将后续参数拼接作为物品名
            if (params.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < params.length; i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(params[i]);
                }
                this.altarTypeStr = sb.toString();
            }
        }
    }
}
