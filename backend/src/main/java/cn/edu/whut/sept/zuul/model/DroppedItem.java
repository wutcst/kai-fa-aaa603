package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 掉落物：怪物死亡后掉落在地图上的物品。
 * 包含物品名称和掉落位置坐标（前端渲染用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DroppedItem {
    private String itemName;
    private int x;
    private int y;
}
