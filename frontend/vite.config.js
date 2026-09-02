import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = env.VITE_API_BASE_URL || process.env.VITE_API_BASE_URL || (mode === 'production' ? 'https://alumniconnect-bwoi.onrender.com' : '')

  return {
    plugins: [react(), tailwindcss()],
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(apiBase),
    },
  }
})
