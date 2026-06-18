package cn.edu.whut.sept.zuul.model;

import lombok.Data;
import java.util.List;

/**
 * 攻击请求 DTO：前端将攻击类型、玩家位置/朝向、当前房间怪物位置
 * 发给后端，由后端统一做空间命中判定并造成伤害。
 *
 * <p>对于 "pierce"（突刺）攻击，前端应额外提供 {@link #startX}/{@link #startY}
 * 和 {@link #endX}/{@link #endY}，表示突刺移动的起点和终点。
 * 后端将基于起点→终点线段做命中判定，覆盖整个冲刺路径。</p>
 */
@Data
public class AttackRequest {
    /** 攻击类型："sweep"（扇形扫击）、"pierce"（直线突刺）或 "charged"（蓄力360°范围攻击） */
    private String attackType;

    /** 玩家当前 X 坐标（前端画布坐标）。
     *  对于 pierce 攻击，此字段为突刺起点；也可用 {@link #startX} 显式指定。 */
    private double playerX;

    /** 玩家当前 Y 坐标（前端画布坐标）。
     *  对于 pierce 攻击，此字段为突刺起点；也可用 {@link #startY} 显式指定。 */
    private double playerY;

    /** 玩家面朝角度（弧度，与 Phaser facingAngle 一致） */
    private double facingAngle;

    /** 突刺攻击的起点 X 坐标（仅 pierce 类型使用，与 playerX 等同时可省略） */
    private Double startX;

    /** 突刺攻击的起点 Y 坐标（仅 pierce 类型使用，与 playerY 等同时可省略） */
    private Double startY;

    /** 突刺攻击的终点 X 坐标（仅 pierce 类型使用，必须提供） */
    private Double endX;

    /** 突刺攻击的终点 Y 坐标（仅 pierce 类型使用，必须提供） */
    private Double endY;

    /** 当前房间内所有存活怪物的名称与坐标 */
    private List<MonsterPosition> monsters;

    /**
     * 怪物坐标快照
     */
    @Data
    public static class MonsterPosition {
        /** 怪物名称（与后端 Room.monsters 中一致） */
        private String name;
        /** 怪物 X 坐标 */
        private double x;
        /** 怪物 Y 坐标 */
        private double y;
    }
}
