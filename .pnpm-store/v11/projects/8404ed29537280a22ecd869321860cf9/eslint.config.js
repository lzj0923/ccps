import vueI18n from '@intlify/eslint-plugin-vue-i18n';
import vue from 'eslint-plugin-vue';

export default [
  {
    files: ['src/**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module'
      }
    }
  },
  ...vue.configs['flat/base'],
  ...vueI18n.configs['flat/base'],
  {
    files: ['src/**/*.vue'],
    rules: {
      // Fixed interface text must use a translation key. Dynamic business data is unaffected.
      '@intlify/vue-i18n/no-raw-text': ['error', {
        attributes: {
          '/.+/': ['title', 'aria-label', 'aria-placeholder', 'aria-roledescription', 'aria-valuetext'],
          input: ['placeholder'],
          img: ['alt']
        },
        ignorePattern: '^[\\s\\d.,·…×＋<>—#（）/／～←›‹✓✎▤▦%+\\-*⌕⌖♙!◎●→↗↓↑↻⌂：。「」－()•••]+$',
        ignoreText: ['', 'RM', '~', 'XLSX', 'PDF']
      }],
      // The current catalog is JavaScript. Key completeness is checked by the runtime catalog test while migration is in progress.
      '@intlify/vue-i18n/no-missing-keys': 'off'
    }
  }
];
