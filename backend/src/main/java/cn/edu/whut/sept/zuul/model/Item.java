package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 物品类：名称、描述
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private String name;       // 物品名称
    private String description;// 物品描述

    /**
     * 简化构造器（仅名称，描述自动生成）
     */
    public Item(String name) {
        this.name = name;
        this.description = "普通的" + name;
    }
}