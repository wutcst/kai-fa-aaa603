package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 状态效果系统：管理玩家的增益(Buff)和减益(Debuff)状态。
 * 当前仅包含烧伤(BURN)状态。
 *
 * 烧伤机制：
 * - 可叠加数层，每次施加烧伤会在现有层数上累加。
 * - 每3秒触发一次：受到等同于当前烧伤层数的法术伤害（可被魔抗减免，结果向下取整），
 *   然后将烧伤层数减半（向下取整）。
 * - 层数降至0时烧伤状态自动移除。
 *
 * 使用方式：Player 持有一个 StatusManager 实例，
 * 通过 statusManager.applyBurn(n) 施加烧伤，
 * 通过 statusManager.tickBurn() 驱动每3秒的烧伤结算。
 */
public class Status {

    /**
     * 状态类型枚举：定义所有可用的增益/减益类型
     */
    public enum StatusType {
        /** 烧伤：可叠加数层，每3秒受到等同于层数的法术伤害，然后层数减半 */
        BURN("烧伤", true);

        private final String displayName;
        private final boolean debuff;

        StatusType(String displayName, boolean debuff) {
            this.displayName = displayName;
            this.debuff = debuff;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isDebuff() {
            return debuff;
        }

        public boolean isBuff() {
            return !debuff;
        }
    }

    /**
     * 状态效果实例：表示一个已施加在玩家身上的具体状态。
     * 烧伤状态使用 layers（层数）和 lastTickTime（上次结算时间戳）管理。
     */
    @Getter
    @Setter
    public static class StatusEffect {
        /** 状态类型 */
        private StatusType type;
        /** 当前层数（烧伤可叠加） */
        private int layers;
        /** 上次烧伤结算的时间戳（毫秒） */
        private long lastTickTime;

        /**
         * @param type   状态类型
         * @param layers 初始层数
         */
        public StatusEffect(StatusType type, int layers) {
            this.type = type;
            this.layers = layers;
            this.lastTickTime = System.currentTimeMillis();
        }

        /** 该状态是否已过期（层数 <= 0 视为过期） */
        public boolean isExpired() {
            return layers <= 0;
        }

        public boolean isDebuff() {
            return type.isDebuff();
        }

        public boolean isBuff() {
            return type.isBuff();
        }
    }

    // ======================== StatusManager ========================

    /**
     * 状态管理器：组合进 Player，负责烧伤状态的增删、计时结算和属性修正。
     * 由于当前仅有烧伤一种状态，使用单一 StatusEffect 实例管理，
     * 保留 StatusEffect 抽象以便后续扩展更多状态类型。
     */
    @Getter
    public static class StatusManager {
        /** 所属玩家引用，用于直接操作玩家生命值 */
        private final Player player;
        /** 烧伤状态实例（null 表示无烧伤） */
        private StatusEffect burnEffect;

        /** 烧伤触发间隔（毫秒）：3秒 */
        public static final long BURN_TICK_INTERVAL = 3000L;

        public StatusManager(Player player) {
            this.player = player;
            this.burnEffect = null;
        }

        // ---- 状态管理 ----

        /**
         * 施加烧伤层数。若已有烧伤状态则在现有层数上累加，否则新建。
         *
         * @param layers 要增加的烧伤层数（必须 > 0）
         */
        public void applyBurn(int layers) {
            if (layers <= 0) return;
            if (burnEffect == null || burnEffect.isExpired()) {
                burnEffect = new StatusEffect(StatusType.BURN, layers);
            } else {
                burnEffect.setLayers(burnEffect.getLayers() + layers);
                // 重置结算计时器：新层数叠加后从此刻重新开始3秒倒计时
                burnEffect.setLastTickTime(System.currentTimeMillis());
            }
        }

        /**
         * 移除烧伤状态。
         */
        public void removeBurn() {
            burnEffect = null;
        }

        /**
         * 清除所有状态（当前即清除烧伤）。
         */
        public void clear() {
            burnEffect = null;
        }

        /**
         * 是否拥有烧伤状态。
         */
        public boolean hasBurn() {
            return burnEffect != null && !burnEffect.isExpired();
        }

        /**
         * 获取当前烧伤层数。
         */
        public int getBurnLayers() {
            return (burnEffect != null && !burnEffect.isExpired()) ? burnEffect.getLayers() : 0;
        }

        // ---- 属性修正方法 ----
        // 当前无属性修正类状态，所有方法直接返回基础值，保留接口以兼容 Player 调用。

        /** 获取修正后的物理攻击力 */
        public int getModifiedAttack() {
            return player.getAttack();
        }

        /** 获取修正后的物理防御力 */
        public int getModifiedDefense() {
            return player.getDefense();
        }

        /** 获取修正后的魔法攻击力 */
        public int getModifiedMagicAttack() {
            return player.getMagicAttack();
        }

        /** 获取修正后的魔法抗性（0-100） */
        public int getModifiedMagicResist() {
            return player.getMagicResist();
        }

        /** 获取修正后的速度 */
        public int getModifiedSpeed() {
            return player.getSpeed();
        }

        /** 获取修正后的闪避率（0-100） */
        public int getModifiedDodge() {
            return player.getDodge();
        }

        // ---- 伤害钩子 ----

        /**
         * 受伤前的钩子：可用于护盾、伤害减免等效果（预留扩展）。
         * @return 修正后的伤害值
         */
        public int onBeforeTakeDamage(int incomingDamage) {
            return incomingDamage;
        }

        /**
         * 造成伤害前的钩子：可用于伤害加深等效果（预留扩展）。
         * @return 修正后的伤害值
         */
        public int onBeforeDealDamage(int damage) {
            return damage;
        }

        // ---- 核心：烧伤计时结算 ----

        /**
         * 驱动烧伤的每3秒结算逻辑。
         * 应由 GameService 在每次命令执行和状态查询时调用，确保烧伤持续生效。
         *
         * 每次触发时：
         * 1. 计算法术伤害 = 当前层数 × (1 - 魔抗/100)，结果向下取整
         * 2. 从玩家生命值中扣除该伤害
         * 3. 烧伤层数减半（向下取整）
         * 4. 若层数降至 0，移除烧伤状态
         *
         * @return 本次结算造成的实际伤害（未触发时返回 0）
         */
        public int tickBurn() {
            if (burnEffect == null || burnEffect.isExpired()) {
                burnEffect = null;
                return 0;
            }

            long now = System.currentTimeMillis();
            if (now - burnEffect.getLastTickTime() < BURN_TICK_INTERVAL) {
                return 0; // 还未到3秒
            }

            int layers = burnEffect.getLayers();
            if (layers <= 0) {
                burnEffect = null;
                return 0;
            }

            // 1. 法术伤害 = 层数，可被魔抗减免，向下取整
            int resist = Math.min(100, getModifiedMagicResist());
            int actualDamage = (int) Math.floor(layers * (1.0 - resist / 100.0));

            // 2. 扣除生命值
            player.setHp(Math.max(0, player.getHp() - actualDamage));
            player.recordDamage(actualDamage);

            // 3. 层数减半（向下取整）
            int newLayers = layers / 2;
            burnEffect.setLayers(newLayers);
            burnEffect.setLastTickTime(now);

            // 4. 层数归零则移除
            if (newLayers <= 0) {
                burnEffect = null;
            }

            return actualDamage;
        }

        // ---- 辅助方法 ----

        /**
         * 获取所有活跃状态的信息列表（供前端展示）。
         */
        public List<Map<String, Object>> getActiveEffectsInfo() {
            List<Map<String, Object>> info = new ArrayList<>();
            if (burnEffect != null && !burnEffect.isExpired()) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", StatusType.BURN.name());
                map.put("name", StatusType.BURN.getDisplayName());
                map.put("isDebuff", true);
                map.put("layers", burnEffect.getLayers());
                long elapsed = System.currentTimeMillis() - burnEffect.getLastTickTime();
                map.put("nextTickIn", Math.max(0, BURN_TICK_INTERVAL - elapsed));
                info.add(map);
            }
            return info;
        }
    }
}
