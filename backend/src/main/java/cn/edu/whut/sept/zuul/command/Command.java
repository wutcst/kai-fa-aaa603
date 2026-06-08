package cn.edu.whut.sept.zuul.command;

import cn.edu.whut.sept.zuul.model.GameResponse;

/**
 * 命令接口：所有命令实现此接口
 */
public interface Command {
    /**
     * 执行命令
     * @return 游戏响应（替代原控制台输出）
     */
    GameResponse execute();

    /**
     * 设置命令参数（如go east中的east）
     */
    void setParams(String... params);
}