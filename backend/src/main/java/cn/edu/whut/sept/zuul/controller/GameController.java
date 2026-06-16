package cn.edu.whut.sept.zuul.controller;

import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.http.ResponseEntity;

/**
 * 游戏REST API控制器
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class GameController {
    private final GameService gameService;

    /**
     * 执行游戏命令
     * POST /api/command
     * 请求体：{"command": "go east"}
     */
    @PostMapping("/command")
    public GameResponse executeCommand(@RequestBody Map<String, String> request) {
        String command = request.get("command");
        if (command == null || command.trim().isEmpty()) {
            return GameResponse.error("Command cannot be empty!");
        }
        return gameService.executeCommand(command);
    }

    /**
     * 获取当前游戏状态
     * GET /api/game
     */
    @GetMapping("/game")
    public GameResponse getGameStatus() {
        return gameService.getGameStatus();
    }

    /**
     * 重置游戏
     * POST /api/reset
     */
    @PostMapping("/reset")
    public GameResponse resetGame() {
        return gameService.resetGame();
    }

    /**
     * 获取全地图概览数据
     * GET /api/map
     */
    @GetMapping("/map")
    public ResponseEntity<Map<String, Object>> getMap() {
        return ResponseEntity.ok(gameService.getFullMap());
    }

    /**
     * 获取背包数据
     * GET /api/backpack
     */
    @GetMapping("/backpack")
    public GameResponse getBackpack() {
        return gameService.getGameStatus();
    }
}