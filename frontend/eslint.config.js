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

const READABLE_ATTRIBUTE = '/^(title|label|alt|placeholder|aria-label|aria-description|aria-valuetext)$/';

const LITERAL_COPY = [
  { selector: 'JSXText[value=/[A-Za-z]/]', message: 'Copy goes through t(…), from src/i18n/locales/.' },
  { selector: ':matches(JSXElement, JSXFragment) > JSXExpressionContainer > Literal[value=/[A-Za-z]/]', message: 'Copy goes through t(…), from src/i18n/locales/.' },
  { selector: ':matches(JSXElement, JSXFragment) > JSXExpressionContainer > TemplateLiteral', message: 'Copy goes through t(…) with its values interpolated.' },
  { selector: ':matches(JSXElement, JSXFragment) > JSXExpressionContainer > ConditionalExpression > Literal[value=/[A-Za-z]/]', message: 'Copy goes through t(…), from src/i18n/locales/.' },
  { selector: `JSXAttribute[name.name=${READABLE_ATTRIBUTE}] > Literal`, message: 'A label a person reads goes through t(…).' },
  { selector: `JSXAttribute[name.name=${READABLE_ATTRIBUTE}] > JSXExpressionContainer > TemplateLiteral`, message: 'A label a person reads goes through t(…).' },
];

const ARROWS_ONLY = [
  { selector: ':not(MethodDefinition, Property) > FunctionExpression', message: 'Write an arrow function.' },
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
      'no-restricted-syntax': ['error', ...HTML_ELEMENT_STYLING, ...LITERAL_COPY, ...ARROWS_ONLY],
      'func-style': ['error', 'expression'],
      'react/jsx-no-comment-textnodes': 'off',
    },
  },
  {
    files: ['src/design-system/**'],
    rules: { 'no-restricted-syntax': ['error', ...LITERAL_COPY, ...ARROWS_ONLY] },
  },
  {
    files: ['**/*.test.ts', '**/*.test.tsx', 'src/test/**'],
    rules: {
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/no-unsafe-type-assertion': 'off',
      'no-restricted-syntax': ['error', ...HTML_ELEMENT_STYLING, ...ARROWS_ONLY],
    },
  },
  {
    files: ['**/*.js', 'scripts/**'],
    ...tseslint.configs.disableTypeChecked,
  },
);
