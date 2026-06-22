smodule.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:prettier/recommended',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  plugins: ['vue'],
  rules: {
    // === 代码风格（放宽以适应现有代码） ===
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'no-empty': 'warn', // 空块只警告不报错（GameCanvas.vue 中有大量占位空块）
    'no-constant-condition': 'warn',

    // === Vue 3 规则 ===
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',
    'vue/no-v-html': 'warn',
    'vue/component-name-in-template-casing': 'off', // 不强制组件命名风格
    'vue/no-mutating-props': 'warn',

    // === 缩进与格式 ===
    'indent': 'off', // 交给 prettier 处理
    'prettier/prettier': [
      'warn',  // 从 error 降为 warn，不阻塞 CI
      {
        singleQuote: true,
        semi: false,
        printWidth: 100,
        trailingComma: 'all',
        tabWidth: 2,
        endOfLine: 'lf',
      },
    ],
  },
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        'vue/max-attributes-per-line': [
          'warn',
          {
            singleline: 5,  // 每行允许更多属性
            multiline: 1,
          },
        ],
      },
    },
  ],
}
