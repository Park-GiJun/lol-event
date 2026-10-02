import js from '@eslint/js';
import ts from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import prettier from 'eslint-config-prettier';
import globals from 'globals';
import { defineConfig, globalIgnores } from 'eslint/config';

export default defineConfig(
	globalIgnores(['dist', 'coverage']),
	js.configs.recommended,
	ts.configs.recommended,
	prettier,
	{
		files: ['**/*.{ts,tsx}'],
		languageOptions: {
			globals: { ...globals.browser, ...globals.node }
		},
		plugins: {
			'react-hooks': reactHooks,
			'react-refresh': reactRefresh
		},
		rules: {
			// typescript-eslint 는 TS 프로젝트에서 no-undef 를 끄도록 권고한다.
			'no-undef': 'off',
			// 시그니처를 맞추려고 두는 자리채움 인자는 _ 로 시작시켜 예외로 둔다.
			'@typescript-eslint/no-unused-vars': [
				'error',
				{ argsIgnorePattern: '^_', varsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' }
			],
			// `any` 는 일부 지점에서 의도적 탈출구 — CI 를 막지 않고 경고만.
			'@typescript-eslint/no-explicit-any': 'warn',

			'react-hooks/rules-of-hooks': 'error',
			// 의존성 누락은 "값이 바뀌었는데 화면이 안 바뀐다" 로 조용히 나타난다. warn 으로 두면
			// 아무도 안 본다 — 규칙이 시끄러우면 끄지 말고 effect 자체를 없앤다(파생값 · 핸들러 · Query).
			'react-hooks/exhaustive-deps': 'error',

			'react-refresh/only-export-components': ['warn', { allowConstantExport: true }]
		}
	},
	{
		files: ['**/*.{test,spec}.{ts,tsx}', 'src/test/**'],
		rules: { '@typescript-eslint/no-explicit-any': 'off' }
	}
);
