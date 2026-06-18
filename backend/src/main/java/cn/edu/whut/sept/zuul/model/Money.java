package cn.edu.whut.sept.zuul.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Money类：负责项目中购物系统的货币相关操作
 * 管理玩家拥有的货币数量，提供增减货币的方法
 */
@Getter
@Setter
public class Money {
    /** 当前货币数量 */
    private int amount;

    /** 默认初始货币数量 */
    public static final int INITIAL_AMOUNT = 50;

    /**
     * 使用默认初始金额构造
     */
    public Money() {
        this.amount = INITIAL_AMOUNT;
    }

    /**
     * 使用指定金额构造
     * @param amount 初始货币数量
     */
    public Money(int amount) {
        this.amount = Math.max(0, amount);
    }

    /**
     * 增加货币
     * @param amount 增加的数量
     * @return 增加后的货币总数
     */
    public int add(int amount) {
        if (amount > 0) {
            this.amount += amount;
        }
        return this.amount;
    }

    /**
     * 减少货币（消费）
     * @param amount 减少的数量
     * @return 是否消费成功（余额不足时返回false）
     */
    public boolean spend(int amount) {
        if (amount <= 0) {
            return false;
        }
        if (this.amount >= amount) {
            this.amount -= amount;
            return true;
        }
        return false;
    }

    /**
     * 检查是否有足够的货币
     * @param amount 需要检查的数量
     * @return 是否足够
     */
    public boolean hasEnough(int amount) {
        return this.amount >= amount;
    }

    /**
     * 重置为初始金额
     */
    public void reset() {
        this.amount = INITIAL_AMOUNT;
    }
}
