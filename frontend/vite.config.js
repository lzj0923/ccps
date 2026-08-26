import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');

  return {
    plugins: [vue()],
    server: env.VITE_DEV_PROXY_TARGET
      ? {
          proxy: {
            '/api': {
              target: env.VITE_DEV_PROXY_TARGET,
              changeOrigin: true,
              secure: true,
            },
          },
        }
      : undefined,
  };
});
