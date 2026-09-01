import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';
import checker from 'vite-plugin-checker';
import monacoEditorPlugin from 'vite-plugin-monaco-editor';

export default defineConfig(() => {
    return {
        base: '/staff',
        plugins: [
            react(),
            svgr(),
            checker({
                typescript: true,
            }),
            monacoEditorPlugin({}),
        ],
        server: {
            port: 3001,
            host: '0.0.0.0',
        },
        build: {
            // Publicly communicated compatibility covers the latest three versions of
            // Chrome, Edge, Firefox, and Safari. Builds use Vite's Baseline target,
            // which may change with Vite upgrades.
            target: 'baseline-widely-available',
            outDir: './build/staff',
        },
    };
});
