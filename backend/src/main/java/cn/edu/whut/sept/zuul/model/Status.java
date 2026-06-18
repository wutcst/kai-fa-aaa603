package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 状态效果系统：管理玩家的增益(Buff)和减益(Debuff)状态。
 * 当前包含烧伤(BURN)、中毒(POISON)和流血(BLEED)三种状态。
 *
 * 烧伤机制：
 * - 可叠加数层，每次施加烧伤会在现有层数上累加。
 * - 每3秒触发一次：受到等同于当前烧伤层数的法术伤害（可被魔抗减免，结果向下取整），
 *   然后将烧伤层数减半（向下取整）。
 * - 层数降至0时烧伤状态自动移除。
 *
 * 中毒机制：
 * - 可叠加数层，每次施加中毒会在现有层数上累加。
 * - 每1秒触发一次：受到等同于 min(当前层数, 玩家血量-1) 的真实伤害
 *   （无视防御和魔抗，直接扣除血量），然后将中毒层数减1。
 * - 玩家不会因中毒状态导致血量降低至1以下，
 *   即当玩家血量不高于中毒层数时，最多扣至1血并触发伤害计数器。
 *   例如：玩家血量15，中毒20层 → 伤害=14，HP降至1，层数降至19。
 * - 层数降至0时中毒状态自动移除。
 *
 * 流血机制：
 * - 可叠加数层，每次施加流血会在现有层数上累加。
 * - 每次玩家进行普通攻击（横扫、蓄力横扫、突刺）时，若身上有流血状态，
 *   受到2点真实伤害（无视防御和魔抗，直接扣除血量），然后将流血层数减1。
 * - 层数降至0时流血状态自动移除。
 *
 * 使用方式：Player 持有一个 StatusManager 实例，
 * 通过 statusManager.applyBurn(n) / applyPoison(n) / applyBleed(n) 施加状态，
 * 通过 statusManager.tickBurn() / tickPoison() 驱动计时结算，
 * 通过 statusManager.tickBleedOnAttack() 在玩家攻击时触发流血。
 */
public class Status {

    /**
     * 状态类型枚举：定义所有可用的增益/减益类型
     */
    public enum StatusType {
        /** 烧伤：可叠加数层，每3秒受到等同于层数的法术伤害，然后层数减半 */
        BURN("烧伤", true),
        /** 天使祝福：整体属性上浮至150%，持续30秒 */
        ANGEL_BUFF("天使祝福", false),
        /** 中毒：可叠加数层，每1秒受到等同于层数的真实伤害，然后层数减1 */
        POISON("中毒", true),
        /** 流血：可叠加数层，每次玩家攻击时受到1点真实伤害，然后层数减1 */
        BLEED("流血", true);

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
        /** 当前层数（可叠加） */
        private int layers;
        /** 上次结算的时间戳（毫秒） */
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
     * 状态管理器：组合进 Player，负责状态效果的增删、计时结算和属性修正。
     * 保留 StatusEffect 抽象以便后续扩展更多状态类型。
     */
    @Getter
    public static class StatusManager {
        /** 所属玩家引用，用于直接操作玩家生命值 */
        private final Player player;
        /** 烧伤状态实例（null 表示无烧伤） */
        private StatusEffect burnEffect;
        /** 天使祝福状态实例（null 表示无天使祝福） */
        private StatusEffect angelBuffEffect;
        /** 中毒状态实例（null 表示无中毒） */
        private StatusEffect poisonEffect;
        /** 流血状态实例（null 表示无流血） */
        private StatusEffect bleedEffect;

        /** 烧伤触发间隔（毫秒）：3秒 */
        public static final long BURN_TICK_INTERVAL = 3000L;
        /** 中毒触发间隔（毫秒）：1秒 */
        public static final long POISON_TICK_INTERVAL = 1000L;
        /** 天使祝福持续时间（毫秒）：30秒 */
        public static final long ANGEL_BUFF_DURATION = 30000L;

        public StatusManager(Player player) {
            this.player = player;
            this.burnEffect = null;
            this.angelBuffEffect = null;
            this.poisonEffect = null;
            this.bleedEffect = null;
        }

        // ========== 烧伤管理 ==========

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

        // ========== 天使祝福管理 ==========

        /**
         * 施加天使祝福效果：全属性上浮至150%，持续30秒。
         * 如果已有天使祝福，重置持续时间。
         */
        public void applyAngelBuff() {
            if (angelBuffEffect == null || angelBuffEffect.isExpired()) {
                angelBuffEffect = new StatusEffect(StatusType.ANGEL_BUFF, 1);
            } else {
                // 重置计时
                angelBuffEffect.setLastTickTime(System.currentTimeMillis());
            }
        }

        /**
         * 移除天使祝福效果。
         */
        public void removeAngelBuff() {
            angelBuffEffect = null;
        }

        /**
         * 是否拥有活跃的天使祝福。
         */
        public boolean hasAngelBuff() {
            return angelBuffEffect != null && !angelBuffEffect.isExpired() && !isAngelBuffExpired();
        }

        /**
         * 检查天使祝福是否过期
         */
        private boolean isAngelBuffExpired() {
            if (angelBuffEffect == null) return true;
            long now = System.currentTimeMillis();
            return (now - angelBuffEffect.getLastTickTime()) >= ANGEL_BUFF_DURATION;
        }

        /**
         * 天使祝福的属性乘数（1.5倍）
         */
        public double getAngelBuffMultiplier() {
            return hasAngelBuff() ? 1.5 : 1.0;
        }

        // ========== 中毒管理 ==========

        /**
         * 施加中毒层数。若已有中毒状态则在现有层数上累加，否则新建。
         *
         * @param layers 要增加的中毒层数（必须 > 0）
         */
        public void applyPoison(int layers) {
            if (layers <= 0) return;
            if (poisonEffect == null || poisonEffect.isExpired()) {
                poisonEffect = new StatusEffect(StatusType.POISON, layers);
            } else {
                poisonEffect.setLayers(poisonEffect.getLayers() + layers);
                // 重置结算计时器：新层数叠加后从此刻重新开始1秒倒计时
                poisonEffect.setLastTickTime(System.currentTimeMillis());
            }
        }

        /**
         * 移除中毒状态。
         */
        public void removePoison() {
            poisonEffect = null;
        }

        /**
         * 施加流血层数。若已有流血状态则在现有层数上累加，否则新建。
         *
         * @param layers 要增加的流血层数（必须 > 0）
         */
        public void applyBleed(int layers) {
            if (layers <= 0) return;
            if (bleedEffect == null || bleedEffect.isExpired()) {
                bleedEffect = new StatusEffect(StatusType.BLEED, layers);
            } else {
                bleedEffect.setLayers(bleedEffect.getLayers() + layers);
            }
        }

        /**
         * 移除流血状态。
         */
        public void removeBleed() {
            bleedEffect = null;
        }

        /**
         * 清除所有状态（烧伤、中毒和流血）。
         */
        public void clear() {
            burnEffect = null;
            poisonEffect = null;
            bleedEffect = null;
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
        /**
         * 是否拥有中毒状态。
         */
        public boolean hasPoison() {
            return poisonEffect != null && !poisonEffect.isExpired();
        }

        /**
         * 获取当前中毒层数。
         */
        public int getPoisonLayers() {
            return (poisonEffect != null && !poisonEffect.isExpired()) ? poisonEffect.getLayers() : 0;
        }


        /**
         * 是否拥有流血状态。
         */
        public boolean hasBleed() {
            return bleedEffect != null && !bleedEffect.isExpired();
        }

        /**
         * 获取当前流血层数。
         */
        public int getBleedLayers() {
            return (bleedEffect != null && !bleedEffect.isExpired()) ? bleedEffect.getLayers() : 0;
        }

        // ---- 属性修正方法 ----
        // 当前无属性修正类状态，所有方法直接返回基础值，保留接口以兼容 Player 调用。

        /** 获取修正后的物理攻击力 */
        public int getModifiedAttack() {
            double mult = getAngelBuffMultiplier();
            return (int) Math.round(player.getAttack() * mult);
        }

        /** 获取修正后的物理防御力 */
        public int getModifiedDefense() {
            double mult = getAngelBuffMultiplier();
            return (int) Math.round(player.getDefense() * mult);
        }

        /** 获取修正后的魔法攻击力 */
        public int getModifiedMagicAttack() {
            double mult = getAngelBuffMultiplier();
            return (int) Math.round(player.getMagicAttack() * mult);
        }

        /** 获取修正后的魔法抗性（0-100） */
        public int getModifiedMagicResist() {
            double mult = getAngelBuffMultiplier();
            return Math.min(100, (int) Math.round(player.getMagicResist() * mult));
        }

        /** 获取修正后的速度 */
        public int getModifiedSpeed() {
            double mult = getAngelBuffMultiplier();
            return (int) Math.round(player.getSpeed() * mult);
        }

        /** 获取修正后的闪避率（0-100） */
        public int getModifiedDodge() {
            double mult = getAngelBuffMultiplier();
            return Math.min(100, (int) Math.round(player.getDodge() * mult));
        }

        // ========== 伤害钩子 ==========

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

        // ---- 核心：流血攻击触发 ----

        /**
         * 在玩家进行普通攻击（横扫/蓄力横扫/突刺）时触发流血效果。
         * 应由 GameService.performAttack() 和 AttackCommand 在攻击流程中调用。
         *
         * 每次触发时：
         * 1. 若玩家身上有流血状态，受到 2 点真实伤害（无视防御和魔抗，直接扣除血量）
         * 2. 流血层数减 1
         * 3. 若层数降至 0，移除流血状态
         *
         * @return 本次结算造成的实际伤害（2），无流血状态或血量已为 1 时返回 0
         */
        public int tickBleedOnAttack() {
            if (bleedEffect == null || bleedEffect.isExpired()) {
                bleedEffect = null;
                return 0;
            }
    
            int layers = bleedEffect.getLayers();
            if (layers <= 0) {
                bleedEffect = null;
                return 0;
            }
    
            // 真实伤害：直接扣除 2 点血量，无视防御和魔抗
            int actualDamage = 2;
            int currentHp = player.getHp();

            // 防御：HP 最低保持 1（流血不会杀死玩家）
            if (currentHp > 1) {
                player.setHp(currentHp - actualDamage);
                player.recordDamage(actualDamage);
            } else {
                // 血量已为 1，不造成伤害（但仍扣除层数）
                actualDamage = 0;
            }

            // 层数减 1
            int newLayers = layers - 1;
            bleedEffect.setLayers(newLayers);

            // 层数归零则移除
            if (newLayers <= 0) {
                bleedEffect = null;
            }

            return actualDamage;
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

        // ========== 中毒计时结算 ==========

        /**
         * 驱动中毒的每1秒结算逻辑。
         * 应由 GameService 在每次命令执行和状态查询时调用，确保中毒持续生效。
         *
         * 每次触发时：
         * 1. 真实伤害 = min(当前层数, 玩家血量 - 1)，即中毒无法将血量降至1以下
         * 2. 扣除血量并记录伤害（无视防御和魔抗）
         * 3. 中毒层数减1
         * 4. 若层数降至 0，移除中毒状态
         *
         * 例如：玩家血量 15，拥有 20 层中毒 → 伤害 = 14，HP 降至 1，层数降至 19
         *
         * @return 本次结算造成的实际伤害（未触发或无伤害时返回 0）
         */
        public int tickPoison() {
            if (poisonEffect == null || poisonEffect.isExpired()) {
                poisonEffect = null;
                return 0;
            }

            long now = System.currentTimeMillis();
            if (now - poisonEffect.getLastTickTime() < POISON_TICK_INTERVAL) {
                return 0; // 还未到1秒
            }

            int layers = poisonEffect.getLayers();
            if (layers <= 0) {
                poisonEffect = null;
                return 0;
            }

            int currentHp = player.getHp();

            // HP 最低保持 1，伤害 = min(层数, 当前血量 - 1)
            int actualDamage = 0;
            if (currentHp > 1) {
                actualDamage = Math.min(layers, currentHp - 1);
                player.setHp(currentHp - actualDamage);
                player.recordDamage(actualDamage);
            }
            // 若 currentHp <= 1，已经是最低血量，不造成伤害也不记录

            // 层数减1
            int newLayers = layers - 1;
            poisonEffect.setLayers(newLayers);
            poisonEffect.setLastTickTime(now);

            // 层数归零则移除
            if (newLayers <= 0) {
                poisonEffect = null;
            }

            return actualDamage;
        }

        // ========== 天使祝福计时 ==========

        /**
         * tick天使祝福：检查是否过期（每帧/每次轮询调用）
         * @return true 如果祝福仍在生效，false 如果已过期
         */
        public boolean tickAngelBuff() {
            if (angelBuffEffect == null || angelBuffEffect.isExpired()) return false;
            if (isAngelBuffExpired()) {
                angelBuffEffect = null;
                return false;
            }
            return true;
        }

        /**
         * 获取天使祝福剩余毫秒数
         */
        public long getAngelBuffRemainingMs() {
            if (angelBuffEffect == null || angelBuffEffect.isExpired()) return 0;
            long elapsed = System.currentTimeMillis() - angelBuffEffect.getLastTickTime();
            return Math.max(0, ANGEL_BUFF_DURATION - elapsed);
        }

        // ========== 辅助方法 ==========

        /**
         * 获取所有活跃状态的信息列表（供前端展示）。
         * 三种状态各自独立检查，互不嵌套。
         */
        public List<Map<String, Object>> getActiveEffectsInfo() {
            List<Map<String, Object>> info = new ArrayList<>();

            // 烧伤
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

            // 天使祝福（独立检查，不与任何其他状态嵌套）
            if (angelBuffEffect != null && !angelBuffEffect.isExpired() && !isAngelBuffExpired()) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", StatusType.ANGEL_BUFF.name());
                map.put("name", StatusType.ANGEL_BUFF.getDisplayName());
                map.put("isDebuff", false);
                long elapsed = System.currentTimeMillis() - angelBuffEffect.getLastTickTime();
                map.put("remainingMs", Math.max(0, ANGEL_BUFF_DURATION - elapsed));
                info.add(map);
            }

            // 中毒（独立检查，不与任何其他状态嵌套）
            if (poisonEffect != null && !poisonEffect.isExpired()) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", StatusType.POISON.name());
                map.put("name", StatusType.POISON.getDisplayName());
                map.put("isDebuff", true);
                map.put("layers", poisonEffect.getLayers());
                long elapsed = System.currentTimeMillis() - poisonEffect.getLastTickTime();
                map.put("nextTickIn", Math.max(0, POISON_TICK_INTERVAL - elapsed));
                info.add(map);
            }
            if (bleedEffect != null && !bleedEffect.isExpired()) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", StatusType.BLEED.name());
                map.put("name", StatusType.BLEED.getDisplayName());
                map.put("isDebuff", true);
                map.put("layers", bleedEffect.getLayers());
                // 流血不受计时驱动，nextTickIn 设为 -1 表示"攻击时触发"
                map.put("nextTickIn", -1L);
                info.add(map);
            }
            return info;
        }
    }
}
