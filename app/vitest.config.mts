import {defineConfig} from 'vitest/config';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';

export default defineConfig({
    plugins: [
        react(),
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
        // Layout regressions in shared accessibility styles need the real CSS, not Vitest's empty CSS stub.
        css: {include: [/\/src\/index\.scss(?:\?|$)/]},
        include: ['src/**/*.{spec,test}.{ts,tsx}'],
        clearMocks: true,
        restoreMocks: true,
    },
});
