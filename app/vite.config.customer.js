import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';
import checker from 'vite-plugin-checker';

export default defineConfig(() => {
    return {
        plugins: [
            react({
                jsxImportSource: '@emotion/react',
                babel: {
                    plugins: ['@emotion/babel-plugin'],
                },
            }),
            svgr(),
            checker({
                typescript: true,
            }),
        ],
        server: {
            port: 3000,
            host: '0.0.0.0',
        },
        build: {
            outDir: './build/customer',
        },
    };
});