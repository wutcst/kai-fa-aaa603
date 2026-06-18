package cn.edu.whut.sept.zuul.model;

import lombok.Data;
import java.util.List;

/**
 * 攻击请求 DTO：前端将攻击类型、玩家位置/朝向、当前房间怪物位置
 * 发给后端，由后端统一做空间命中判定并造成伤害。
 */
@Data
public class AttackRequest {
    /** 攻击类型："sweep"（扇形扫击）或 "pierce"（直线突刺） */
    private String attackType;

    /** 玩家当前 X 坐标（前端画布坐标） */
    private double playerX;

    /** 玩家当前 Y 坐标（前端画布坐标） */
    private double playerY;

    /** 玩家面朝角度（弧度，与 Phaser facingAngle 一致） */
    private double facingAngle;

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
