package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 状态效果系统：管理玩家的减益(Debuff)状态。
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
 *   受到2点真实伤害（无视防御和魔抗，直接扣除血量，可致死），然后将流血层数减1。
 * - 层数降至0时流血状态自动移除。
 *
 * 使用方式：Player 持有一个 StatusManager 实例，
 * 通过 statusManager.applyBurn(n) / applyPoison(n) / applyBleed(n) 施加状态，
 * 通过 statusManager.tickBurn() / tickPoison() 驱动计时结算，
 * 通过 statusManager.tickBleedOnAttack() 在玩家攻击时触发流血。
 */
public class Status {

    /**
     * 状态类型枚举：定义所有可用的减益类型
     */
    public enum StatusType {
        /** 烧伤：可叠加数层，每3秒受到等同于层数的法术伤害，然后层数减半 */
        BURN("烧伤"),
        /** 中毒：可叠加数层，每1秒受到等同于层数的真实伤害，然后层数减1 */
        POISON("中毒"),
        /** 流血：可叠加数层，每次玩家攻击时受到2点真实伤害（可致死），然后层数减1 */
        BLEED("流血"),
        /** 迟缓：持续10秒不可叠加，移速降低50%，无法疾跑 */
        SLOW("迟缓"),
        /** 束缚：持续3秒不可叠加，无法移动和突刺 */
        BIND("束缚");

        private final String displayName;

        StatusType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 状态效果实例：表示一个已施加在玩家身上的具体状态。
     */
    @Getter
    public static class StatusEffect {
        /** 状态类型 */
        private final StatusType type;
        /** 当前层数（可叠加） */
        @Setter
        private int layers;
        /** 上次结算的时间戳（毫秒） */
        @Setter
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
    }

    // ======================== StatusManager ========================

    /**
     * 状态管理器：组合进 Player，负责状态效果的增删、计时结算。
     */
    @Getter
    @Setter
    public static class StatusManager {
        /** 所属玩家引用，用于直接操作玩家生命值 */
        private final Player player;
        /** 烧伤状态实例（null 表示无烧伤） */
        private StatusEffect burnEffect;
        /** 中毒状态实例（null 表示无中毒） */
        private StatusEffect poisonEffect;
        /** 流血状态实例（null 表示无流血） */
        private StatusEffect bleedEffect;
        /** 迟缓状态实例（null 表示无迟缓） */
        private StatusEffect slowEffect;
        /** 束缚状态实例（null 表示无束缚） */
        private StatusEffect bindEffect;

        /** 烧伤触发间隔（毫秒）：3秒 */
        private static final long BURN_TICK_INTERVAL = 3000L;
        /** 中毒触发间隔（毫秒）：1秒 */
        private static final long POISON_TICK_INTERVAL = 1000L;
        /** 流血每次触发伤害 */
        private static final int BLEED_DAMAGE_PER_TICK = 2;
        /** 迟缓持续时间（毫秒）：10秒 */
        private static final long SLOW_DURATION = 10000L;
        /** 束缚持续时间（毫秒）：3秒 */
        private static final long BIND_DURATION = 3000L;
        /** 迟缓移速倍率：50% */
        private static final double SLOW_SPEED_RATIO = 0.5;
        /** 属性上限值（魔抗、闪避等百分比属性） */
        private static final int MAX_PERCENT = 100;

        public StatusManager(Player player) {
            this.player = player;
            this.burnEffect = null;
            this.poisonEffect = null;
            this.bleedEffect = null;
            this.slowEffect = null;
            this.bindEffect = null;
        }

        // ========== 通用辅助方法（消除重复代码） ==========

        /**
         * 检查状态效果是否活跃（非空且未过期），若过期则自动置空。
         */
        private boolean isActive(StatusEffect effect) {
            if (effect == null) return false;
            if (effect.isExpired()) return false;
            return true;
        }

        /**
         * 获取状态层数，若状态无效则返回 0。
         */
        private int getLayers(StatusEffect effect) {
            return isActive(effect) ? effect.getLayers() : 0;
        }

        /**
         * 通用减益施加逻辑：若已有则累加层数并可选重置计时，否则新建。
         *
         * @param current    当前持有的效果引用（可能为 null）
         * @param type       状态类型
         * @param layers     要施加的层数
         * @param resetTimer 是否重置结算计时器
         * @return 新的效果实例
         */
        private StatusEffect applyDebuff(StatusEffect current, StatusType type, int layers, boolean resetTimer) {
            if (layers <= 0) return current;
            StatusEffect effect;
            if (current == null || current.isExpired()) {
                effect = new StatusEffect(type, layers);
            } else {
                current.setLayers(current.getLayers() + layers);
                effect = current;
            }
            if (resetTimer) {
                effect.setLastTickTime(System.currentTimeMillis());
            }
            return effect;
        }

        /**
         * 通用计时结算：检查是否到达触发间隔，若未到达则返回 0。
         * @return 距上次结算的毫秒数，若未到间隔返回 0
         */
        private long checkTickInterval(StatusEffect effect, long interval) {
            if (!isActive(effect)) return 0;
            long elapsed = System.currentTimeMillis() - effect.getLastTickTime();
            return elapsed >= interval ? elapsed : 0;
        }

        // ========== 烧伤管理 ==========

        public void applyBurn(int layers) {
            burnEffect = applyDebuff(burnEffect, StatusType.BURN, layers, true);
        }

        public void removeBurn() {
            burnEffect = null;
        }

        public boolean hasBurn() {
            return isActive(burnEffect);
        }

        public int getBurnLayers() {
            return getLayers(burnEffect);
        }

        // ========== 中毒管理 ==========

        public void applyPoison(int layers) {
            poisonEffect = applyDebuff(poisonEffect, StatusType.POISON, layers, true);
        }

        public void removePoison() {
            poisonEffect = null;
        }

        public boolean hasPoison() {
            return isActive(poisonEffect);
        }

        public int getPoisonLayers() {
            return getLayers(poisonEffect);
        }

        // ========== 流血管理 ==========

        public void applyBleed(int layers) {
            bleedEffect = applyDebuff(bleedEffect, StatusType.BLEED, layers, false);
        }

        public void removeBleed() {
            bleedEffect = null;
        }

        public boolean hasBleed() {
            return isActive(bleedEffect);
        }

        public int getBleedLayers() {
            return getLayers(bleedEffect);
        }

        // ========== 迟缓管理 ==========

        /**
         * 施加迟缓：不可叠加，持续10秒，每次施加刷新计时器。
         */
        public void applySlow(int layers) {
            // 迟缓不可叠加，若已存在则只刷新计时器
            if (slowEffect != null && !slowEffect.isExpired()) {
                slowEffect.setLastTickTime(System.currentTimeMillis());
                return;
            }
            slowEffect = new StatusEffect(StatusType.SLOW, 1);
        }

        public void removeSlow() {
            slowEffect = null;
        }

        public boolean hasSlow() {
            return isActive(slowEffect);
        }

        // ========== 束缚管理 ==========

        /**
         * 施加束缚：不可叠加，持续3秒，每次施加刷新计时器。
         */
        public void applyBind(int layers) {
            // 束缚不可叠加，若已存在则只刷新计时器
            if (bindEffect != null && !bindEffect.isExpired()) {
                bindEffect.setLastTickTime(System.currentTimeMillis());
                return;
            }
            bindEffect = new StatusEffect(StatusType.BIND, 1);
        }

        public void removeBind() {
            bindEffect = null;
        }

        public boolean hasBind() {
            return isActive(bindEffect);
        }

        // ========== 全局操作 ==========

        /**
         * 清除所有状态（烧伤、中毒、流血、迟缓、束缚）。
         */
        public void clear() {
            burnEffect = null;
            poisonEffect = null;
            bleedEffect = null;
            slowEffect = null;
            bindEffect = null;
        }

        // ========== 属性透传方法（供 Player 调用，当前无乘法修正） ==========

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
            return Math.min(MAX_PERCENT, player.getMagicResist());
        }

        /** 获取修正后的速度（迟缓状态下减半） */
        public int getModifiedSpeed() {
            int baseSpeed = player.getSpeed();
            if (hasSlow()) {
                return (int) Math.max(1, Math.round(baseSpeed * SLOW_SPEED_RATIO));
            }
            return baseSpeed;
        }

        /** 获取修正后的闪避率（0-100） */
        public int getModifiedDodge() {
            return Math.min(MAX_PERCENT, player.getDodge());
        }

        // ========== 流血攻击触发 ==========

        /**
         * 在玩家进行普通攻击（横扫/蓄力横扫/突刺）时触发流血效果。
         * 应由 GameService.performAttack() 和 AttackCommand 在攻击流程中调用。
         *
         * 每次触发时：
         * 1. 若玩家身上有流血状态，受到 2 点真实伤害（无视防御和魔抗，直接扣除血量，可致死）
         * 2. 流血层数减 1
         * 3. 若层数降至 0，移除流血状态
         *
         * @return 本次结算造成的实际伤害（2），无流血状态时返回 0
         */
        public int tickBleedOnAttack() {
            if (!isActive(bleedEffect)) {
                bleedEffect = null;
                return 0;
            }

            int layers = bleedEffect.getLayers();
            if (layers <= 0) {
                bleedEffect = null;
                return 0;
            }

            // 真实伤害：直接扣除固定伤害，无视防御和魔抗，可致死
            int actualDamage = BLEED_DAMAGE_PER_TICK;
            int currentHp = player.getHp();

            player.setHp(Math.max(0, currentHp - actualDamage));
            player.recordDamage(actualDamage);

            // 层数减 1
            int newLayers = layers - 1;
            bleedEffect.setLayers(newLayers);

            // 层数归零则移除
            if (newLayers <= 0) {
                bleedEffect = null;
            }

            return actualDamage;
        }

        // ========== 迟缓计时结算 ==========

        /**
         * 驱动迟缓的过期检查逻辑。
         * 迟缓不可叠加，持续 SLOW_DURATION（10秒）。
         * 超过持续时间后自动移除。
         *
         * @return true 如果迟缓刚刚过期被移除
         */
        public boolean tickSlow() {
            if (!isActive(slowEffect)) {
                slowEffect = null;
                return false;
            }
            long elapsed = System.currentTimeMillis() - slowEffect.getLastTickTime();
            if (elapsed >= SLOW_DURATION) {
                slowEffect = null;
                return true;
            }
            return false;
        }

        // ========== 束缚计时结算 ==========

        /**
         * 驱动束缚的过期检查逻辑。
         * 束缚不可叠加，持续 BIND_DURATION（3秒）。
         * 超过持续时间后自动移除。
         *
         * @return true 如果束缚刚刚过期被移除
         */
        public boolean tickBind() {
            if (!isActive(bindEffect)) {
                bindEffect = null;
                return false;
            }
            long elapsed = System.currentTimeMillis() - bindEffect.getLastTickTime();
            if (elapsed >= BIND_DURATION) {
                bindEffect = null;
                return true;
            }
            return false;
        }

        // ========== 烧伤计时结算 ==========

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
            if (checkTickInterval(burnEffect, BURN_TICK_INTERVAL) == 0) {
                if (!isActive(burnEffect)) burnEffect = null;
                return 0;
            }

            int layers = burnEffect.getLayers();
            if (layers <= 0) {
                burnEffect = null;
                return 0;
            }

            // 法术伤害 = 层数，可被魔抗减免，向下取整
            int resist = getModifiedMagicResist();
            int actualDamage = (int) Math.floor(layers * (1.0 - resist / 100.0));

            // 扣除生命值
            player.setHp(Math.max(0, player.getHp() - actualDamage));
            player.recordDamage(actualDamage);

            // 层数减半（向下取整）
            int newLayers = layers / 2;
            burnEffect.setLayers(newLayers);
            burnEffect.setLastTickTime(System.currentTimeMillis());

            // 层数归零则移除
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
            if (checkTickInterval(poisonEffect, POISON_TICK_INTERVAL) == 0) {
                if (!isActive(poisonEffect)) poisonEffect = null;
                return 0;
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
            poisonEffect.setLastTickTime(System.currentTimeMillis());

            // 层数归零则移除
            if (newLayers <= 0) {
                poisonEffect = null;
            }

            return actualDamage;
        }

        // ========== 前端信息输出 ==========

        /**
         * 构建单个减益状态的信息 Map。
         */
        private Map<String, Object> buildDebuffInfo(StatusEffect effect, long tickInterval) {
            Map<String, Object> map = new HashMap<>(6);
            map.put("type", effect.getType().name());
            map.put("name", effect.getType().getDisplayName());
            map.put("isDebuff", true);
            map.put("layers", effect.getLayers());
            long elapsed = System.currentTimeMillis() - effect.getLastTickTime();
            map.put("nextTickIn", Math.max(0, tickInterval - elapsed));
            return map;
        }

        /**
         * 获取所有活跃状态的信息列表（供前端展示）。
         */
        public List<Map<String, Object>> getActiveEffectsInfo() {
            List<Map<String, Object>> info = new ArrayList<>(5);

            if (isActive(burnEffect)) {
                info.add(buildDebuffInfo(burnEffect, BURN_TICK_INTERVAL));
            }
            if (isActive(poisonEffect)) {
                info.add(buildDebuffInfo(poisonEffect, POISON_TICK_INTERVAL));
            }
            if (isActive(bleedEffect)) {
                Map<String, Object> map = new HashMap<>(6);
                map.put("type", StatusType.BLEED.name());
                map.put("name", StatusType.BLEED.getDisplayName());
                map.put("isDebuff", true);
                map.put("layers", bleedEffect.getLayers());
                // 流血不受计时驱动，nextTickIn 设为 -1 表示"攻击时触发"
                map.put("nextTickIn", -1L);
                info.add(map);
            }
            if (isActive(slowEffect)) {
                Map<String, Object> map = new HashMap<>(6);
                map.put("type", StatusType.SLOW.name());
                map.put("name", StatusType.SLOW.getDisplayName());
                map.put("isDebuff", true);
                map.put("layers", 1);
                long elapsed = System.currentTimeMillis() - slowEffect.getLastTickTime();
                map.put("nextTickIn", Math.max(0, SLOW_DURATION - elapsed));
                info.add(map);
            }
            if (isActive(bindEffect)) {
                Map<String, Object> map = new HashMap<>(6);
                map.put("type", StatusType.BIND.name());
                map.put("name", StatusType.BIND.getDisplayName());
                map.put("isDebuff", true);
                map.put("layers", 1);
                long elapsed = System.currentTimeMillis() - bindEffect.getLastTickTime();
                map.put("nextTickIn", Math.max(0, BIND_DURATION - elapsed));
                info.add(map);
            }
            return info;
        }
    }
}
