import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

const HTML_ELEMENT_STYLING = [
  {
    selector: "JSXOpeningElement[name.name=/^[a-z]/] > JSXAttribute[name.name=/^(className|style)$/]",
    message: 'Styling belongs to src/design-system/: compose a primitive, or extend one.',
  },
];

export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'src-tauri', 'src/protocol/generated'] },
  ...tseslint.configs.strictTypeChecked,
  jsxA11y.flatConfigs.recommended,
  reactHooks.configs.flat.recommended,
  {
    languageOptions: {
      globals: globals.browser,
      parserOptions: { projectService: { allowDefaultProject: ['eslint.config.js'] }, tsconfigRootDir: import.meta.dirname },
    },
    rules: {
      '@typescript-eslint/restrict-template-expressions': ['error', { allowNumber: true }],
      '@typescript-eslint/no-confusing-void-expression': 'off',
      'no-restricted-syntax': ['error', ...HTML_ELEMENT_STYLING],
      'react/jsx-no-comment-textnodes': 'off',
    },
  },
  {
    files: ['src/design-system/**'],
    rules: { 'no-restricted-syntax': 'off' },
  },
  {
    files: ['**/*.test.ts', '**/*.test.tsx', 'src/test/**'],
    rules: {
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/no-unsafe-type-assertion': 'off',
    },
  },
  {
    files: ['**/*.js', 'scripts/**'],
    ...tseslint.configs.disableTypeChecked,
  },
);
