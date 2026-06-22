# Changelog

## [1.0.0] - 2025-06-22

### 新增
- 后端 Spring Boot 游戏服务，支持 RESTful API
- 前端 Vue 3 + Phaser 游戏界面
- 数据库持久化：背包、房间、怪物、商店状态
- 完整游戏循环：移动、战斗、拾取、交互、商店

### 技术栈
- 后端：Java 17 + Spring Boot 3.2 + H2 Database
- 前端：Vue 3 + Phaser 3 + Vite
- CI/CD：GitHub Actions 自动化构建与发布

### 代码规范
- 前端：ESLint + Prettier
- 后端：Checkstyle（Google Java Style）
- 提交规范：Conventional Commits
- 安全扫描：CodeQL

---

> 发布格式说明：
> - `zuul-game-v1.0.0.zip`：完整发布包
> - 解压后运行 `java -jar backend/*.jar` 启动服务
> - 浏览器访问 `http://localhost:8080/frontend/static/index.html`
