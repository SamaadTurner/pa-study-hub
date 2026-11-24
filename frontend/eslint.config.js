// ESLint settings for the frontend. Run with: npm run lint
// The same rules as the old .eslintrc.cjs: ESLint's and typescript-eslint's recommended sets and
// the two React hooks rules (rules of hooks, missing useEffect dependencies). The newer
// react-hooks preset adds React Compiler rules; those are left out on purpose, since this file
// only moves the old settings to the new format.
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import reactHooks from 'eslint-plugin-react-hooks'
import globals from 'globals'

export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'playwright-report', 'test-results', 'coverage'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: { ...globals.browser },
    },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
    },
  },
)
