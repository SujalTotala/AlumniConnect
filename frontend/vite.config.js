import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')

  if (command === 'build' && mode === 'production' && !env.VITE_API_BASE_URL) {
    throw new Error('VITE_API_BASE_URL must be set for production builds.')
  }

  return {
    plugins: [react(), tailwindcss()],
  }
})
