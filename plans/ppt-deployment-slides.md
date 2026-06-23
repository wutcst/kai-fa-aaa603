# 📊 PPT 素材：系统集成与发布部署

> 项目：Zuul Game（奇幻宅）
> 用途：PPT 直接复制使用
> 共 6 页

---

## 第 1 页：标题页

| 项目 | 内容 |
|------|------|
| **标题** | 系统集成与持续交付 |
| **副标题** | 基于 GitHub Actions 的 CI/CD 流水线 |
| **项目名称** | Zuul Game（奇幻宅） |
| **小字** | 指导教师：XXX \| 日期：202X年X月 |

---

## 第 2 页：整体架构

### 架构图描述

```
[开发者推送代码] ──→ [GitHub Actions]
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
   [Code Style]   [Build & Test]   [Security]
   (Lint 检查)    (编译+测试)      (CodeQL)
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                  ┌──────────┐
                  │ Release  │ ← git tag v*
                  └────┬─────┘
                       ▼
              ┌─────────────────┐
              │ zuul-game-v1.0.0│
              │ .zip (可下载)   │
              └─────────────────┘
```

### PPT 文字素材

> 系统集成分三个层次：
>
> - **持续集成（CI）**：每次 push/PR 自动运行代码规范检查、编译测试、安全扫描
> - **持续交付（CD）**：推送版本标签（`git tag v*`）自动构建产物并发布到 GitHub Release
> - **发布物**：ZIP 包包含后端 JAR + 前端静态文件，下载即可运行

### 📷 IDEA 演示操作

> 打开项目 `.github/workflows/` 目录，展示 4 个 yml 文件

---

## 第 3 页：CI 工作流详解

### 工作流表格

| 工作流 | 配置文件 | 触发条件 | 检查内容 |
|--------|----------|----------|----------|
| **Code Style Check** | [`lint.yml`](.github/workflows/lint.yml) | push / PR to main | 前端 ESLint、后端 Checkstyle、Commitlint |
| **Build & Test** | [`build.yml`](.github/workflows/build.yml) | push / PR to main | Vite 构建、Maven 编译+测试、artifacts 上传 |
| **Security Scan** | [`security.yml`](.github/workflows/security.yml) | push / PR / 每周定时 | CodeQL 语义安全分析 |
| **Release** | [`release.yml`](.github/workflows/release.yml) | 推送 `v*` 标签 | 构建前后端、打包 ZIP、发布到 Releases |

### 关键配置示例

```yaml
# .github/workflows/build.yml
name: Build & Test

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  frontend-build:
    steps:
      - run: npm ci && npm run build
      - uses: actions/upload-artifact@v4  # 上传构建产物

  backend-build:
    steps:
      - run: mvn clean verify
      - run: mvn package -DskipTests
      - uses: actions/upload-artifact@v4  # 上传 JAR 包
```

### 📷 IDEA 演示操作

> 逐一打开 `.github/workflows/` 下的 4 个 yml 文件

---

## 第 4 页：持续交付 — GitHub Release

### 发布流程

| 步骤 | 命令 / 操作 | 说明 |
|------|-------------|------|
| ① 创建标签 | `git tag -a v1.0.0 -m "Release v1.0.0"` | 标记版本号 |
| ② 推送标签 | `git push origin v1.0.0` | 触发 Release 工作流 |
| ③ 自动构建 | GitHub Actions 自动运行 | 编译后端 JAR + Vite 构建前端 |
| ④ 打包发布 | 自动生成 ZIP 上传到 Releases | 包含完整运行环境 |
| ⑤ 下载运行 | 用户从 Releases 页面下载 | `java -jar` 即可启动 |

### 发布包结构

```
zuul-game-v1.0.0.zip
├── README.md                  # 项目说明
├── API.md                     # API 文档
├── CHANGELOG.md               # 版本日志
├── plans/                     # 设计文档
├── backend/
│   └── zuul-springboot-1.0.0.jar    ← Spring Boot 可执行 JAR
└── frontend/
    └── static/                       ← 前端静态文件
        ├── index.html
        ├── assets/
        └── ...
```

### 配置文件

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*'  # 推送 v1.0.0、v2.0.0 等标签时触发

jobs:
  release:
    steps:
      - uses: actions/checkout@v4
      - run: mvn package -DskipTests        # 构建后端
      - run: npm ci && npm run build        # 构建前端
      - run: zip -r zuul-game-v1.0.0.zip .  # 打包发布包
      - uses: softprops/action-gh-release@v2  # 创建 Release
```

### 📷 GitHub 演示操作

> 进入仓库 → Releases → 展示 v1.0.0 发布页面
> 点击下载 zuul-game-v1.0.0.zip

---

## 第 5 页：分支保护与代码评审

### 保护规则

在 GitHub **Settings → Branches → Add rule** 中配置：

| 规则 | 设置 | 作用 |
|------|------|------|
| 🔒 Require pull request | ✅ 启用 | 禁止直接推送 main 分支 |
| 👥 Require approvals | 1 人 | 至少 1 人评审通过 |
| 📋 Require status checks | ✅ 启用 | CI 全部通过才能合并 |
| 🔄 Require up-to-date | ✅ 启用 | PR 必须基于最新 main |
| 🛑 Do not allow bypassing | ✅ 启用 | 管理员也不能绕过 |

### 配套文件

| 文件 | 作用 |
|------|------|
| [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) | 创建 PR 时自动填充检查清单 |
| [`.github/CODEOWNERS`](.github/CODEOWNERS) | 按目录自动分配评审人 |
| [`commitlint.config.cjs`](commitlint.config.cjs) | 提交信息格式规范 |

### 📷 GitHub 演示操作

> 展示一个真实的 PR 页面：
> - CI 检查绿色通过 ✅
> - Reviewer 的评论
> - Merge 按钮可用

---

## 第 6 页：部署与运行

### 方式一：开发环境运行（IDEA 演示）

```bash
# 终端 1：启动后端
cd backend
mvn spring-boot:run

# 终端 2：启动前端（支持热更新）
cd frontend
npm run dev

# 浏览器访问 http://localhost:5173
```

### 方式二：生产环境运行（Release 包）

```bash
# 1. 从 GitHub Releases 下载 zuul-game-v1.0.0.zip
# 2. 解压
unzip zuul-game-v1.0.0.zip -d zuul-game

# 3. 启动后端（需要 Java 17+）
java -jar zuul-game/backend/zuul-springboot-1.0.0.jar

# 4. 浏览器访问 http://localhost:8080/frontend/static/index.html
```

### 所需环境

| 软件 | 版本要求 | 验证命令 |
|------|----------|----------|
| Java | 17+ | `java -version` |
| Node.js | 18+ | `node -v` |
| Maven | 3.8+ | `mvn -v` |

### 📷 IDEA 演示操作

> 在终端执行 `java -jar` 启动游戏，浏览器展示运行效果

---

## 附录：推荐演示流程

| 顺序 | 演示内容 | 操作位置 | 预计时间 |
|------|----------|----------|----------|
| 1 | 展示 `.github/workflows/` 4 个 yml | IDEA | 1 分钟 |
| 2 | 展示 GitHub Actions 运行记录 | GitHub Actions 标签页 | 1 分钟 |
| 3 | 展示 Release 下载页面 | GitHub Releases | 1 分钟 |
| 4 | 下载 ZIP 并运行 | 终端 + 浏览器 | 2 分钟 |
| 5 | 展示分支保护配置 | GitHub Settings | 1 分钟 |

---

> 本文档位于 `plans/ppt-deployment-slides.md`，可直接复制到 PPT 中使用。
