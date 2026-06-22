// commitlint 配置文件
// 遵循 Conventional Commits 规范
// 规范: https://www.conventionalcommits.org/zh-hans/v1.0.0/

module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // type 枚举
    'type-enum': [
      2,
      'always',
      [
        'feat',     // 新功能
        'fix',      // Bug 修复
        'docs',     // 文档更新
        'style',    // 代码格式（不影响功能）
        'refactor', // 重构（既不是新增功能，也不是修复 Bug）
        'perf',     // 性能优化
        'test',     // 增加测试
        'chore',    // 构建过程或辅助工具的变动
        'revert',   // 回退
      ],
    ],
    // scope 小写
    'scope-case': [2, 'always', 'lower-case'],
    // subject 不能为空
    'subject-empty': [2, 'never'],
    // subject 不以句号结尾
    'subject-full-stop': [0, 'never'],
    // subject 小写开头
    'subject-case': [0, 'always', 'lower-case'],
    // 最大长度
    'header-max-length': [2, 'always', 72],
  },
  // 可选：支持自定义解析器处理 issue 引用
  parserOpts: {
    headerPattern: /^(\w*)(?:\(([\w$.\-*/ ]*)\))?: (.*)$/,
    headerCorrespondence: ['type', 'scope', 'subject'],
  },
}
