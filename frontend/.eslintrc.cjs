module.exports = {
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
    // === 代码风格 ===
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',

    // === Vue 3 规则 ===
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',
    'vue/no-v-html': 'warn',
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    'vue/no-mutating-props': 'error',

    // === 缩进与格式 ===
    'indent': 'off', // 交给 prettier 处理
    'prettier/prettier': [
      'error',
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
            singleline: 3,
            multiline: 1,
          },
        ],
      },
    },
  ],
}
