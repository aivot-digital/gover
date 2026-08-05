import {defineConfig} from 'vitest/config';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';

export default defineConfig({
    plugins: [
        react({
            jsxImportSource: '@emotion/react',
            babel: {
                plugins: ['@emotion/babel-plugin'],
            },
        }),
        svgr(),
    ],
    test: {
        environment: 'jsdom',
        environmentOptions: {
            jsdom: {
                url: 'http://localhost/',
            },
        },
        setupFiles: './src/setupTests.ts',
        include: ['src/**/*.{spec,test}.{ts,tsx}'],
        clearMocks: true,
        restoreMocks: true,
    },
});
