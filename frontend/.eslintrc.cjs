// ESLint settings for the frontend. Run with: npm run lint
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  parser: '@typescript-eslint/parser',
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    // Checks the rules of hooks and missing useEffect dependencies
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', 'node_modules', 'playwright-report', 'test-results'],
}
