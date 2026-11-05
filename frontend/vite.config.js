import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    server: {
        // 本机 5173 已被其他项目占用，本项目固定使用 5174
        port: 5174,
        // 同时监听 IPv4/IPv6，兼容内置浏览器与局域网访问
        host: '0.0.0.0',
        proxy: {
            '/api': {
                target: 'http://localhost:8081',
                changeOrigin: true
            }
        }
    }
});
