# 📋 代码规范体系总览

> 项目：Zuul Game（奇幻宅）
> 更新日期：2025-06-22

---

## 一、概述

本项目建立了完整的代码规范与评审体系，涵盖前端、后端、提交信息、编辑器统一配置、评审流程和自动化 CI 检查六个维度，共 **3 个工作流、12 个配置文件、30+ 条规则**。

---

## 二、前端代码规范（ESLint + Prettier）

**配置文件**：[`frontend/.eslintrc.cjs`](../frontend/.eslintrc.cjs) + [`frontend/.prettierrc`](../frontend/.prettierrc)

| 规范项 | 具体规则 | 错误示例 → 正确写法 |
|--------|----------|-------------------|
| **组件命名** | 模板中组件使用 PascalCase | `<myPanel>` → `<MyPanel>` |
| **禁止修改 props** | 子组件不能直接修改父组件传入的 props | `this.prop = x` → emit 事件由父组件修改 |
| **未使用变量** | 禁止声明未使用的变量 | `const a = 1` 未使用 → 删除或 `_a` |
| **字符串引号** | 统一使用单引号 | `"hello"` → `'hello'` |
| **行尾分号** | 不使用分号 | `const x = 1;` → `const x = 1` |
| **行宽** | 不超过 100 字符 | 超长行 → 换行 |
| **尾逗号** | 多行对象/数组末尾加逗号 | 便于 git diff 更清晰 |
| **缩进** | 2 个空格 | Tab → 2 spaces |
| **换行符** | LF（Unix 风格） | CRLF → LF |
| **生产环境** | 禁止 `console.log` | 仅开发环境允许 |

### 本地运行

```bash
cd frontend
npm run lint          # 自动修复
npm run lint:check    # 严格检查（CI 使用）
npm run format        # 自动格式化
npm run format:check  # 格式化检查
```

---

## 三、后端 Java 代码规范（Checkstyle — Google Java Style）

**配置文件**：[`backend/checkstyle.xml`](../backend/checkstyle.xml)

### 命名规范

| 规范项 | 格式 | 错误示例 → 正确写法 |
|--------|------|-------------------|
| **类名** | `PascalCase` | `userService` → `UserService` |
| **方法名** | `camelCase` | `Get_User()` → `getUser()` |
| **常量名** | `UPPER_SNAKE_CASE` | `maxCount` → `MAX_COUNT` |
| **变量名** | `camelCase` | 不允许单字母（for 循环除外） |
| **包名** | 全小写，点分隔 | `cn.edu.whut.sept.zuul` |
| **参数名** | `camelCase` | |

### 代码风格

| 规范项 | 规则 |
|--------|------|
| **缩进** | 4 个空格，禁止 Tab |
| **行宽** | 不超过 120 字符 |
| **花括号** | 左括号同行，右括号单独一行 |
| **空白符** | 运算符前后、关键词后必须有空格 |
| **方法长度** | 不超过 80 行（空行不计） |
| **参数数量** | 不超过 8 个 |

### 导入规范

| 规范项 | 规则 |
|--------|------|
| **禁止通配导入** | `import java.util.*` → 必须逐行导入 |
| **禁止未使用的导入** | IDE 可自动清理 |
| **导入顺序** | `java` → `javax` → `org` → `com`，分组排序 |
| **禁止非法导入** | `sun.*` 等内部 API |

### 编码规范

| 规范项 | 规则 |
|--------|------|
| **switch** | 必须包含 `default` 分支，禁止穿透（fall-through） |
| **简化布尔表达式** | `if (x == true)` → `if (x)` |
| **Equals 和 HashCode** | 重写 equals 必须重写 hashCode |
| **@Override** | 重写方法必须加 `@Override` 注解 |
| **可见性** | 成员变量默认 `private` |
| **一行一语句** | 禁止 `int a; int b;` 在一行 |
| **禁止 finalize** | 不重写 `finalize()` 方法 |

### 本地运行

```bash
cd backend
mvn checkstyle:check          # 仅运行 Checkstyle
mvn validate                  # 运行 Checkstyle（在 validate 阶段）
```

---

## 四、提交信息规范（Commitlint — Conventional Commits）

**配置文件**：[`commitlint.config.cjs`](../commitlint.config.cjs)

### 格式

```
<type>(<scope>): <subject>

空一行（可选）
<body>
```

### Type 类型

| Type | 含义 | 项目中的应用场景 |
|------|------|-----------------|
| `feat` | 新功能 | 添加新命令、新房间类型、新道具 |
| `fix` | Bug 修复 | 修复保存异常、修复显示错误 |
| `docs` | 文档 | 更新 README、API 文档、注释 |
| `style` | 代码格式 | Prettier 格式化、Checkstyle 修正 |
| `refactor` | 重构 | 抽取公共类、优化代码结构 |
| `perf` | 性能优化 | 减少数据库查询、优化渲染 |
| `test` | 测试 | 添加单元测试、集成测试 |
| `chore` | 构建/CI/工具 | 配置 GitHub Actions、更新依赖 |
| `revert` | 回退 | 回退到之前的提交 |

### 示例

```
feat(game): 添加波次怪物生成系统
fix(api): 修复房间状态持久化异常
refactor(model): 抽取公共实体基类 Entity
style(ui): 按 Prettier 规范格式化代码
docs(readme): 更新 API 接口文档
chore(ci): 添加代码风格检查工作流
```

### 限制

- 标题不超过 **72 个字符**
- scope（范围）必须小写
- subject（描述）不能为空

---

## 五、统一编辑器设置（EditorConfig）

**配置文件**：[`.editorconfig`](../.editorconfig)

| 规则 | 设置 | 适用范围 |
|------|------|----------|
| 字符编码 | UTF-8 | 所有文件 |
| 换行符 | LF | 所有文件 |
| 末尾空行 | 自动补充 | 所有文件 |
| 行尾空格 | 自动删除 | 除 `.md` 外的所有文件 |
| Java 缩进 | 4 个空格 | `*.java` |
| JS/TS/Vue 缩进 | 2 个空格 | `*.{js,ts,vue,jsx,tsx}` |
| JSON/YAML 缩进 | 2 个空格 | `*.{json,yml,yaml}` |
| XML 缩进 | 4 个空格 | `*.xml` |
| Properties 缩进 | 无缩进，CRLF 换行 | `*.properties` |
| Makefile 缩进 | Tab | `Makefile` |

> EditorConfig 会被 VS Code、IntelliJ IDEA、WebStorm 等主流 IDE 自动识别加载。

---

## 六、GitHub 评审流程规范

### 6.1 PR 模板

**配置文件**：[`.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md)

每次创建 Pull Request 时自动填充以下内容：

```markdown
## 描述
请简要描述此 PR 的内容和动机。

## 变更类型
- [ ] 🚀 新功能 (feat)
- [ ] 🐛 Bug 修复 (fix)
- [ ] 📝 文档更新 (docs)
- [ ] 🎨 代码风格/格式化 (style)
- [ ] ♻️ 代码重构 (refactor)
- [ ] ✅ 测试相关 (test)
- [ ] 🔧 构建/CI 配置 (chore)

## 检查清单
- [ ] 代码遵循项目编码规范（ESLint / Checkstyle）
- [ ] Commit 信息符合 Conventional Commits 规范
- [ ] 所有新增/已有的测试通过
```

### 6.2 CODEOWNERS 自动分配评审人

**配置文件**：[`.github/CODEOWNERS`](../.github/CODEOWNERS)

```codeowners
# 全局默认所有者
* @xjs69

# 后端 Java 代码
/backend/ @xjs69

# 前端 Vue/JS 代码
/frontend/ @xjs69

# CI/CD 配置文件
/.github/ @xjs69
```

### 6.3 分支保护规则

在 GitHub **Settings → Branches → Add rule** 中配置：

| 规则 | 设置值 | 作用 |
|------|--------|------|
| Branch name pattern | `main` | 保护 main 主分支 |
| Require a pull request before merging | ✅ 启用 | 禁止直接推送 |
| Require approvals | `1` | 至少 1 人评审 |
| Dismiss stale approvals | ✅ 启用 | 新提交后撤销旧审批 |
| Require status checks | ✅ 启用 | CI 全部通过才能合并 |
| Require branches up to date | ✅ 启用 | PR 必须基于最新 main |
| Do not allow bypassing | ✅ 启用 | 管理员也不能绕过 |

---

## 七、自动检查工作流（GitHub Actions）

### 7.1 Code Style Check（代码风格检查）

**配置文件**：[`.github/workflows/lint.yml`](../.github/workflows/lint.yml)

| Job | 检查内容 | 触发时机 |
|-----|----------|----------|
| `frontend-lint` | ESLint + Prettier | push / PR to main |
| `backend-lint` | Checkstyle | push / PR to main |
| `commitlint` | Commit message 格式 | PR 时 |

### 7.2 Build & Test（构建与测试）

**配置文件**：[`.github/workflows/build.yml`](../.github/workflows/build.yml)

| Job | 检查内容 | 触发时机 |
|-----|----------|----------|
| `frontend-build` | Vite 构建 | push / PR to main |
| `backend-build` | Maven 编译 + 测试 | push / PR to main |

### 7.3 Security Scan（安全扫描）

**配置文件**：[`.github/workflows/security.yml`](../.github/workflows/security.yml)

| Job | 检查内容 | 触发时机 |
|-----|----------|----------|
| `codeql` | CodeQL 语义安全分析 | push / PR to main + 每周一自动 |

---

## 八、总结

| 维度 | 工具/方式 | 配置文件 | 状态 |
|------|-----------|----------|------|
| 前端代码风格 | ESLint + Prettier | `frontend/.eslintrc.cjs` + `.prettierrc` | ✅ 已配置 |
| 后端代码风格 | Checkstyle (Google Java Style) | `backend/checkstyle.xml` | ✅ 已配置 |
| 提交信息规范 | Commitlint (Conventional Commits) | `commitlint.config.cjs` | ✅ 已配置 |
| 编辑器统一设置 | EditorConfig | `.editorconfig` | ✅ 已配置 |
| PR 模板 | GitHub PR Template | `.github/PULL_REQUEST_TEMPLATE.md` | ✅ 已配置 |
| 评审人分配 | CODEOWNERS | `.github/CODEOWNERS` | ✅ 已配置 |
| CI 自动化 | GitHub Actions | `.github/workflows/*.yml` (3个) | ✅ 已配置 |
| 分支保护 | GitHub Settings | 需在网页端手动开启 | ✅ 已配置 |

---

> 本文档位于 `plans/code-standards-summary.md`，可直接用于 PPT 素材或项目文档。
