package cn.edu.whut.sept.zuul.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏接口统一响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {
    // 响应状态：success/error
    private String status;
    // 响应消息（提示/错误信息）
    private String message;
    // 响应数据（房间信息、物品列表等）
    private Object data;

    // 快速构建成功响应
    public static GameResponse success(String message, Object data) {
        return new GameResponse("success", message, data);
    }

    // 快速构建失败响应
    public static GameResponse error(String message) {
        return new GameResponse("error", message, null);
    }
}