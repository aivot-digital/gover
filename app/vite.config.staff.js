import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';

export default defineConfig(() => {
    return {
        base: '/staff',
        plugins: [
            react({
                jsxImportSource: '@emotion/react',
                babel: {
                    plugins: ['@emotion/babel-plugin'],
                },
            }),
            svgr(),
        ],
        server: {
            port: 3001,
            host: '0.0.0.0',
        },
        preview: {
            port: 3001,
            host: '0.0.0.0',
        },
        build: {
            outDir: './build/staff',
        },
    };
});