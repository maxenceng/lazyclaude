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

// ADR 0085: state with behaviour is a class, a helper sits at module level; an inline callback stays an arrow.
const NO_NESTED_FUNCTIONS = [
  {
    selector: ':function VariableDeclarator > :matches(ArrowFunctionExpression, FunctionExpression)',
    message: 'Declare the helper at module level, or make this a class with a private method.',
  },
  {
    selector: ':matches(:function ReturnStatement, ArrowFunctionExpression) > ObjectExpression > Property > :function',
    message: 'An object of closures is a class: give it private state and methods.',
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
      'no-restricted-syntax': ['error', ...HTML_ELEMENT_STYLING, ...LITERAL_COPY, ...ARROWS_ONLY, ...NO_NESTED_FUNCTIONS],
      'func-style': ['error', 'expression'],
      'react/jsx-no-comment-textnodes': 'off',
    },
  },
  {
    files: ['src/design-system/**'],
    rules: { 'no-restricted-syntax': ['error', ...LITERAL_COPY, ...ARROWS_ONLY, ...NO_NESTED_FUNCTIONS] },
  },
  {
    files: ['**/*.test.ts', '**/*.test.tsx', 'src/test/**'],
    rules: {
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/no-unsafe-type-assertion': 'off',
      'no-restricted-syntax': ['error', ...HTML_ELEMENT_STYLING, ...ARROWS_ONLY, ...NO_NESTED_FUNCTIONS],
    },
  },
  {
    files: ['**/*.js', 'scripts/**'],
    ...tseslint.configs.disableTypeChecked,
  },
);
