package cn.edu.whut.sept.zuul.controller;

import cn.edu.whut.sept.zuul.model.AttackRequest;
import cn.edu.whut.sept.zuul.model.GameResponse;
import cn.edu.whut.sept.zuul.model.GameSaveEntity;
import cn.edu.whut.sept.zuul.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /**
     * 执行玩家攻击（扇形扫击/直线突刺），由后端统一做空间命中判定。
     * POST /api/attack
     * 请求体：{"attackType":"sweep","playerX":...,"playerY":...,"facingAngle":...,"monsters":[...]}
     */
    @PostMapping("/attack")
    public GameResponse performAttack(@RequestBody AttackRequest request) {
        return gameService.performAttack(request);
    }

    // ======================== 存档接口 ========================

    /**
     * 保存当前游戏
     * POST /api/save
     * 请求体：{"saveId": 1} 或 {}（新建存档）
     */
    @PostMapping("/save")
    public ResponseEntity<GameSaveEntity> saveGame(@RequestBody Map<String, Object> request) {
        Long saveId = null;
        if (request != null && request.containsKey("saveId") && request.get("saveId") != null) {
            saveId = ((Number) request.get("saveId")).longValue();
        }
        GameSaveEntity saved = gameService.saveGame(saveId);
        return ResponseEntity.ok(saved);
    }

    /**
     * 加载存档
     * POST /api/load
     * 请求体：{"saveId": 1}
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> loadGame(@RequestBody Map<String, Object> request) {
        if (request == null || !request.containsKey("saveId") || request.get("saveId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 saveId 参数"));
        }
        Long saveId = ((Number) request.get("saveId")).longValue();
        Map<String, Object> data = gameService.loadGame(saveId);
        return ResponseEntity.ok(data);
    }

    /**
     * 获取所有存档列表
     * GET /api/saves
     */
    @GetMapping("/saves")
    public ResponseEntity<List<Map<String, Object>>> listSaves() {
        return ResponseEntity.ok(gameService.listSaves());
    }

    /**
     * 删除存档
     * POST /api/deleteSave
     * 请求体：{"saveId": 1}
     */
    @PostMapping("/deleteSave")
    public ResponseEntity<Map<String, Object>> deleteSave(@RequestBody Map<String, Object> request) {
        if (request == null || !request.containsKey("saveId") || request.get("saveId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 saveId 参数"));
        }
        Long saveId = ((Number) request.get("saveId")).longValue();
        gameService.deleteSave(saveId);
        return ResponseEntity.ok(Map.of("message", "存档已删除"));
    }

    // ======================== 风隐技能接口 ========================

    /**
     * 激活风隐形态
     * POST /api/windcloak/activate
     */
    @PostMapping("/windcloak/activate")
    public GameResponse activateWindCloak() {
        return gameService.activateWindCloak();
    }

    /**
     * 解除风隐形态
     * POST /api/windcloak/deactivate
     */
    @PostMapping("/windcloak/deactivate")
    public GameResponse deactivateWindCloak() {
        return gameService.deactivateWindCloak();
    }
}