
<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen" alt="Spring Boot 3.2.0">
  <img src="https://img.shields.io/badge/Vue-3.3.4-4FC08D" alt="Vue 3">
  <img src="https://img.shields.io/badge/Phaser-3.60.0-9B59B6" alt="Phaser 3">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT License">
</p>

<h1 align="center">⚔ ZUUL — 失落的古迹 ⚔</h1>

<p align="center">
  <em>一款基于 Web 的 Roguelike 地牢探险游戏</em><br>
  <strong>深入迷宫，探寻被遗忘的秘密</strong>
</p>

<p align="center">
  <a href="#-项目简介">项目简介</a> •
  <a href="#-核心功能">核心功能</a> •
  <a href="#-技术栈">技术栈</a> •
  <a href="#-系统架构">系统架构</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-项目结构">项目结构</a> •
  <a href="#-开发团队">开发团队</a>
</p>

---

## 📖 项目简介

**ZUUL — 失落的古迹** 是一个基于经典文本冒险游戏 *World of Zuul* 扩展开发的图形化 Roguelike 地牢探险游戏。玩家将扮演一位勇敢的探险者，穿越程序随机生成的迷宫，与神秘的怪物战斗，收集强大的魔法物品，并在篝火处恢复体力、提升能力，最终揭示隐藏在废墟中的真相。

本项目为 **武汉理工大学软件工程实训** 小组协同开发任务，旨在通过实践巩固软件工程规范、提高面向对象建模与抽象能力、培养小组协同开发能力。

> **游戏 motto**: *"勇气是人类最伟大的赞歌。"*

---

## ✨ 核心功能

### 🎮 游戏玩法
| 功能 | 说明 |
|------|------|
| **随机地图生成** | 每次游戏生成独特的 10×15 房间网格，包含多种房间类型 |
| **实时战斗系统** | WASD 移动、J 攻击/长按蓄力、Shift+方向+J 突刺、H 月光波 |
| **多种怪物类型** | 普通怪物、精英怪物（带特殊减益效果）、Boss 怪物 |
| **状态效果系统** | 烧伤（持续火焰伤害）、中毒（持续毒伤害）、流血（攻击触发伤害） |
| **背包系统** | 15格背包，支持物品使用、丢弃、装备/卸下 |
| **装备系统** | 铁剑（+攻击）、铁盾（+防御）、暗影披风（+闪避）、生命戒指（+HP）、元素项链（+MP） |
| **商店系统** | 购买/出售物品，动态商品列表 |
| **篝火祭坛** | 治愈祭坛（回复 HP/MP）、训练祭坛（提升属性）、博学祭坛（选择增益） |
| **奇遇事件** | 宝箱（随机奖励）、喷泉（随机效果）、精英敌人（击败得稀有掉落） |
| **怪物掉落** | 货币奖励、药水、装备、饰品、浆果 |
| **存档系统** | 3个存档槽位，支持保存/读取/删除 |

### 🏠 房间类型
| 类型 | 颜色 | 说明 |
|------|------|------|
| 🟡 **START_HALL** | 金色 | 起始大厅 |
| 🔵 **SHOP** | 蓝色 | 商人房间，可购买/出售物品 |
| 🟣 **ENCOUNTER** | 紫色 | 奇遇事件房间 |
| 🟠 **CAMPFIRE** | 橙色 | 篝火休息点，包含三个祭坛 |
| 🔴 **BOSS** | 红色 | Boss 战房间 |
| 🟠 **ELITE_MONSTER** | 番茄红 | 精英怪物房间 |
| 🔵 **NORMAL_MONSTER** | 蓝色 | 普通怪物房间 |

---

## 🛠 技术栈

### 后端 (Backend)
| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 17 | 编程语言 |
| **Spring Boot** | 3.2.0 | Web 框架 |
| **Spring Data JPA** | — | 持久化层 |
| **H2 Database** | — | 嵌入式数据库 |
| **Maven** | — | 项目构建与管理 |
| **Lombok** | — | 代码简化 |

### 前端 (Frontend)
| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue 3** | 3.3.4 | 前端框架 |
| **Phaser 3** | 3.60.0 | 游戏引擎 |
| **Vite** | 5.0.0 | 构建工具 |
| **Pinia** | 2.0.33 | 状态管理 |

### 开发工具与流程
| 工具 | 用途 |
|------|------|
| **IntelliJ IDEA** | 后端开发 IDE |
| **VS Code** | 前端开发 IDE |
| **GitHub** | 代码仓库与协同开发 |
| **GitHub Issues** | 任务管理与分配 |
| **GitHub Actions** | CI/CD 自动化流水线 |
| **Postman** | API 测试 |

---

## 🏗 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3 + Phaser 3)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ MainMenu │  │GameCanvas│  │BackpackUI│  │ Minimap  │   │
│  │   (Vue)  │  │ (Phaser) │  │   (Vue)  │  │ (Canvas) │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       └──────────────┼──────────────┼──────────────┘        │
│                      │   HTTP REST API (JSON)               │
└──────────────────────┼──────────────────────────────────────┘
                       │
┌──────────────────────┼──────────────────────────────────────┐
│                      ▼                                      │
│  Backend (Spring Boot 3.2.0)                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              GameController (REST API)               │   │
│  │  GET /api/game  POST /api/command  POST /api/attack  │   │
│  │  POST /api/reset  GET /api/map  GET /api/backpack    │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                          │                                    │
│  ┌──────────────────────┴───────────────────────────────┐   │
│  │                  GameService                         │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                          │                                    │
│  ┌──────────────────────┴───────────────────────────────┐   │
│  │              Game (核心游戏逻辑)                       │   │
│  │  Room / Player / Monster / Item / Bag / Combat       │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                          │                                    │
│  ┌──────────────────────┴───────────────────────────────┐   │
│  │            Repository Layer (Spring Data JPA)         │   │
│  │  GameSaveRepo / RoomStateRepo / MonsterStateRepo     │   │
│  │  BagItemRepo / ShopStateRepo                         │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                          │                                    │
│  ┌──────────────────────┴───────────────────────────────┐   │
│  │               H2 Database (嵌入式)                    │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### API 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/game` | 获取当前游戏状态 |
| `POST` | `/api/command` | 执行游戏命令 |
| `POST` | `/api/attack` | 执行玩家攻击 |
| `POST` | `/api/reset` | 重置游戏 |
| `POST` | `/api/save/{slot}` | 保存游戏到指定槽位 |
| `POST` | `/api/load/{slot}` | 从指定槽位加载存档 |
| `GET` | `/api/map` | 获取全地图数据 |
| `GET` | `/api/backpack` | 获取背包物品列表 |

> 完整的 API 文档请参见 [`API.md`](API.md)。

---

## 🚀 快速开始

### 环境要求

- **JDK 17+**
- **Node.js 18+**
- **Maven 3.8+**

### 1. 启动后端

```bash
# 进入后端目录
cd backend

# 使用 Maven 编译并启动
mvn spring-boot:run
```

后端服务默认启动在 `http://localhost:8080`。

### 2. 启动前端

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端开发服务器默认启动在 `http://localhost:5173`，并自动代理 `/api` 请求到后端。

### 3. 开始游戏

在浏览器中打开 `http://localhost:5173`，点击 **"开始游戏"** 即可进入冒险！

---

## 📁 项目结构

```
kai-fa-aaa603/
├── README.md                 # 项目介绍文档（本文件）
├── API.md                    # API 接口文档
├── backend/                  # 后端项目 (Spring Boot + Maven)
│   ├── pom.xml               # Maven 依赖配置
│   ├── src/main/java/.../zuul/
│   │   ├── Game.java         # 游戏核心逻辑
│   │   ├── GenerateRoom.java # 随机地图生成器
│   │   ├── ZuulApplication.java # Spring Boot 启动类
│   │   ├── command/          # 命令处理类
│   │   │   ├── GoCommand.java
│   │   │   ├── AttackCommand.java
│   │   │   ├── PickupCommand.java
│   │   │   ├── BagCommand.java
│   │   │   ├── ShopCommand.java
│   │   │   ├── InteractCommand.java
│   │   │   └── ...
│   │   ├── controller/       # REST 控制器
│   │   │   └── GameController.java
│   │   ├── model/            # 数据模型
│   │   │   ├── Room.java
│   │   │   ├── Player.java
│   │   │   ├── Monster.java
│   │   │   ├── Bag.java
│   │   │   ├── Item.java
│   │   │   └── ...
│   │   ├── repository/       # 数据持久层
│   │   └── service/          # 业务服务层
│   └── src/main/resources/
│       └── application.properties
├── frontend/                 # 前端项目 (Vue 3 + Vite)
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── App.vue           # 主应用组件
│       ├── main.js           # 入口文件
│       ├── api/gameApi.js    # 后端 API 封装
│       ├── components/
│       │   ├── MainMenu.vue  # 主菜单界面
│       │   └── GameCanvas.vue # 游戏主画布 (Phaser 3)
│       ├── composables/
│       │   └── useSlotDialog.js  # 存档弹窗逻辑
│       └── entity/
│           └── EntityDrawer.js   # 游戏实体绘制器
├── plans/                    # 开发计划文档
│   ├── database-full-implementation.md
│   ├── frontend-refactor-v2.md
│   ├── shop-item-system.md
│   ├── equipment-system.md
│   ├── encounter-event-system.md
│   └── ...
└── REPORT.docx               # 小组实训报告
```

---

## 👥 开发团队

| 角色 | 姓名 | GitHub | 主要职责 |
|------|------|--------|----------|
| 👑 **组长 / 核心后端** | 李冬晨 | [@Sader-Lee](https://github.com/Sader-Lee) | 项目架构设计、玩家系统、take/drop/items命令、魔法饼干、前后端分离、怪物系统（生成/攻击）、商店系统（购买/出售）、背包系统、房间战斗封锁、数据库连接、攻击特效 |
| 🎨 **前端 / 战斗系统** | 蒋志成 | [@BytesJiang](https://github.com/BytesJiang) | 前端框架搭建、移动控制、物品拾取/传送、小地图、UI/UX优化、攻击特效（火焰粒子+屏幕振动）、蓄力攻击/击退、状态效果系统（烧伤/中毒/流血）、随机地图生成、房间颜色分类、篝火祭坛、奇遇房间、月光波技能、Bug修复 |
| 🛠 **特殊系统 / 实体** | 熊俊森 | [@XJS-123](https://github.com/XJS-123) | 初始页面搭建、饰品系统完整实现、奇遇事件系统、人物模型绘制、Buff系统（天使buff与中毒buff冲突处理）、铁匠系统（保底机制）、游戏实体构造（掉落物等）、地图背景添加 |

> 本项目为 **武汉理工大学 2026 软件工程实训** 小组作业，遵循 MIT 开源协议。

---

## 📜 许可证

本项目基于 MIT 许可证开源，详见项目根目录的 `LICENSE` 文件。

---

<p align="center">
  <sub>Built with ❤️ by WHUT 软件工程实训小组</sub><br>
  <sub>© 2026 武汉理工大学 计算机科学与技术学院</sub>
</p>
