# 🚀 CI/CD 流水线演示

此文件用于触发 GitHub Actions 运行，生成 status checks 记录，以便后续配置分支保护规则。

## CI 检查清单

推送代码后，GitHub Actions 将自动运行以下 3 个工作流：

### 1. Code Style Check（代码风格检查）
- ✅ 前端：ESLint + Prettier（JS/Vue 代码规范）
- ✅ 后端：Checkstyle（Java 代码规范，基于 Google Java Style）
- ✅ Commitlint（提交信息规范检查）

### 2. Build & Test（构建与测试）
- ✅ 前端：Vite 构建
- ✅ 后端：Maven 编译 + 测试

### 3. Security Scan（安全扫描）
- ✅ CodeQL 静态安全分析

## 配置分支保护

CI 全部通过后，在仓库 Settings → Branches → Add rule 中：

```
Branch name pattern: main

☑ Require a pull request before merging
  ☑ Require approvals (1)
  ☑ Dismiss stale approvals
☑ Require status checks to pass before merging
  ☑ Require branches up to date
☑ Do not allow bypassing
```

然后在 "Status checks that are required" 中勾选以下 5 项：

1. `Code Style Check / frontend-lint`
2. `Code Style Check / backend-lint`
3. `Code Style Check / commitlint`
4. `Build & Test / frontend-build`
5. `Build & Test / backend-build`

---

> ⏱️ 预计首次 CI 运行时间：3-5 分钟
> 状态可在仓库的 **Actions** 标签页查看
